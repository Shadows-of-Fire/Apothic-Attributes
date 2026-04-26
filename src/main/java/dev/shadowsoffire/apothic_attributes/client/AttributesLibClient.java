package dev.shadowsoffire.apothic_attributes.client;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.Lists;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.datafixers.util.Pair;

import dev.shadowsoffire.apothic_attributes.ALConfig;
import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.placebo.config.Configuration;
import dev.shadowsoffire.placebo.util.Offset;
import dev.shadowsoffire.placebo.util.Offset.AnchorPoint;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CritParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffect.AttributeTemplate;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.GatherEffectScreenTooltipsEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class AttributesLibClient {

    /**
     * Holds the currently-active {@link AttributesGui} so we can route {@link ScreenEvent.MouseScrolled.Pre}
     * events to it directly — in 26.1, {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen#mouseScrolled}
     * no longer falls through to {@code ContainerEventHandler.mouseScrolled}, so listeners added via
     * {@link ScreenEvent.Init.Post#addListener} never receive scroll events through the normal dispatch path.
     */
    @Nullable
    private static AttributesGui activeAttribGui = null;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void addAttribComponent(ScreenEvent.Init.Post e) {
        if (ALConfig.enableAttributesGui && e.getScreen() instanceof InventoryScreen scn) {
            var atrComp = new AttributesGui(scn);
            e.addListener(atrComp);
            e.addListener(atrComp.toggleBtn);
            e.addListener(atrComp.hideUnchangedBtn);
            if (AttributesGui.wasOpen || AttributesGui.swappedFromCurios) atrComp.toggleVisibility();
            AttributesGui.swappedFromCurios = false;
            activeAttribGui = atrComp;
        }
        else if (!(e.getScreen() instanceof InventoryScreen)) {
            activeAttribGui = null;
        }
    }

    @SubscribeEvent
    public void forwardScroll(ScreenEvent.MouseScrolled.Pre e) {
        AttributesGui gui = activeAttribGui;
        if (gui != null && e.getScreen() instanceof InventoryScreen
            && gui.isMouseOver(e.getMouseX(), e.getMouseY())
            && gui.mouseScrolled(e.getMouseX(), e.getMouseY(), e.getScrollDeltaX(), e.getScrollDeltaY())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void effectGuiTooltips(GatherEffectScreenTooltipsEvent e) {
        if (e.getTooltip().size() == 0) return;

        List<Component> tooltips = e.getTooltip();
        MobEffectInstance effectInst = e.getEffectInstance();
        Holder<MobEffect> effect = effectInst.getEffect();

        MutableComponent name = (MutableComponent) tooltips.get(0);
        Component duration = tooltips.remove(1);
        duration = Component.translatable("(%s)", duration).withStyle(ChatFormatting.WHITE);

        name.append(" ").append(duration);

        if (ApothicAttributes.getTooltipFlag().isAdvanced()) {
            name.append(" ").append(Component.translatable("[%s]", effect.unwrapKey().get().identifier().toString()).withStyle(ChatFormatting.GRAY));
        }

        String key = effect.value().getDescriptionId() + ".desc";
        if (I18n.exists(key)) {
            tooltips.add(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY));
        }
        else if (ApothicAttributes.getTooltipFlag().isAdvanced() && effect.value().attributeModifiers.isEmpty()) {
            tooltips.add(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        List<Pair<Holder<Attribute>, AttributeModifier>> list = Lists.newArrayList();
        Map<Holder<Attribute>, AttributeTemplate> map = effect.value().attributeModifiers;
        if (!map.isEmpty()) {
            for (Map.Entry<Holder<Attribute>, AttributeTemplate> entry : map.entrySet()) {
                AttributeTemplate template = entry.getValue();
                list.add(new Pair<>(entry.getKey(), template.create(effectInst.getAmplifier())));
            }
        }

        if (!list.isEmpty()) {
            for (Pair<Holder<Attribute>, AttributeModifier> pair : list) {
                tooltips.add(pair.getFirst().value().toComponent(pair.getSecond(), ApothicAttributes.getTooltipFlag()));
            }
        }
    }

    @SubscribeEvent
    public void potionTooltips(ItemTooltipEvent e) {
        if (!ALConfig.enablePotionTooltips) return;

        ItemStack stack = e.getItemStack();
        List<Component> tooltips = e.getToolTip();

        if (stack.getItem() instanceof PotionItem) {
            List<MobEffectInstance> effects = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).customEffects();
            if (effects.size() == 1 && tooltips.size() >= 2) {
                MobEffect effect = effects.get(0).getEffect().value();
                String key = effect.getDescriptionId() + ".desc";
                if (I18n.exists(key)) {
                    tooltips.add(2, Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY));
                }
                else if (e.getFlags().isAdvanced() && effect.attributeModifiers.isEmpty()) {
                    tooltips.add(2, Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                }
            }
        }
    }

    @SubscribeEvent
    public void commands(RegisterClientCommandsEvent e) {
        e.getDispatcher().register(
            LiteralArgumentBuilder.<CommandSourceStack>literal("apothic_attributes_client")
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("set_btn_pos")
                    .then(Commands.argument("anchor", StringArgumentType.string()).suggests(AnchorPoint.SUGGEST_ANCHOR_POINT)
                        .executes(c -> {
                            updateHudPos(AnchorPoint.parse(c.getArgument("anchor", String.class)), 0, 0);
                            return 0;
                        })
                        .then(Commands.argument("x", IntegerArgumentType.integer(-1000, 1000))
                            .then(Commands.argument("y", IntegerArgumentType.integer(-1000, 1000))
                                .executes(c -> {
                                    updateHudPos(AnchorPoint.parse(c.getArgument("anchor", String.class)), c.getArgument("x", Integer.class), c.getArgument("y", Integer.class));
                                    return 0;
                                }))))));
    }

    private static void updateHudPos(AnchorPoint anchor, int x, int y) {
        Configuration cfg = ALConfig.load();
        ALConfig.attributesGuiButtonOffset = new Offset(anchor, x, y);
        Offset.save("GUI Button Offset", "client", ALConfig.attributesGuiButtonOffset, cfg);
    }

    public static void apothCrit(int entityId) {
        Entity entity = Minecraft.getInstance().level.getEntity(entityId);
        if (entity != null) {
            Minecraft.getInstance().particleEngine.createTrackingEmitter(entity, ALObjects.Particles.APOTH_CRIT.get());
        }
    }

    public static class ModBusSub {
        @SubscribeEvent
        public static void clientReload(AddClientReloadListenersEvent e) {
            e.addListener(ApothicAttributes.loc("al_config"), ALConfig.makeReloader());
        }

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent e) {
            if (ModList.get().isLoaded("curios")) {
                // NeoForge.EVENT_BUS.register(new CuriosClientCompat());
            }
        }

        @SubscribeEvent
        public static void particleFactories(RegisterParticleProvidersEvent e) {
            e.registerSpriteSet(ALObjects.Particles.APOTH_CRIT.get(), ApothCritProvider::new);
        }
    }

    public static class ApothCritParticle extends CritParticle {

        public ApothCritParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, TextureAtlasSprite sprite) {
            super(level, x, y, z, xa, ya, za, sprite);
            this.bCol = 1F;
            this.rCol = 0.3F;
            this.gCol = 0.8F;
        }

    }

    public static class ApothCritProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public ApothCritProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xa, double ya, double za, RandomSource random) {
            return new ApothCritParticle(level, x, y, z, xa, ya, za, this.sprite.get(random));
        }
    }

}
