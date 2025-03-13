package dev.shadowsoffire.apothic_attributes.client;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;

import dev.shadowsoffire.apothic_attributes.ALConfig;
import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CritParticle;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import net.neoforged.neoforge.client.event.GatherEffectScreenTooltipsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class AttributesLibClient {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void addAttribComponent(ScreenEvent.Init.Post e) {
        if (ALConfig.enableAttributesGui && e.getScreen() instanceof InventoryScreen scn) {
            var atrComp = new AttributesGui(scn);
            e.addListener(atrComp);
            e.addListener(atrComp.toggleBtn);
            e.addListener(atrComp.hideUnchangedBtn);
            if (AttributesGui.wasOpen || AttributesGui.swappedFromCurios) atrComp.toggleVisibility();
            AttributesGui.swappedFromCurios = false;
        }
    }

    @SuppressWarnings("deprecation")
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void effectGuiTooltips(GatherEffectScreenTooltipsEvent e) {
        List<Component> tooltips = e.getTooltip();
        MobEffectInstance effectInst = e.getEffectInstance();
        Holder<MobEffect> effect = effectInst.getEffect();

        MutableComponent name = (MutableComponent) tooltips.get(0);
        Component duration = tooltips.remove(1);
        duration = Component.translatable("(%s)", duration).withStyle(ChatFormatting.WHITE);

        name.append(" ").append(duration);

        if (ApothicAttributes.getTooltipFlag().isAdvanced()) {
            name.append(" ").append(Component.translatable("[%s]", effect.unwrapKey().get().location().toString()).withStyle(ChatFormatting.GRAY));
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

    public static void apothCrit(int entityId) {
        Entity entity = Minecraft.getInstance().level.getEntity(entityId);
        if (entity != null) {
            Minecraft.getInstance().particleEngine.createTrackingEmitter(entity, ALObjects.Particles.APOTH_CRIT.get());
        }
    }

    public static class ModBusSub {
        @SubscribeEvent
        public static void clientReload(RegisterClientReloadListenersEvent e) {
            e.registerReloadListener(ALConfig.makeReloader());
        }

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent e) {
            if (ModList.get().isLoaded("curios")) {
                NeoForge.EVENT_BUS.register(new CuriosClientCompat());
            }
        }

        @SubscribeEvent
        public static void particleFactories(RegisterParticleProvidersEvent e) {
            e.registerSprite(ALObjects.Particles.APOTH_CRIT.get(), ApothCritParticle::new);
        }
    }

    public static class ApothCritParticle extends CritParticle {

        public ApothCritParticle(SimpleParticleType type, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
            super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
            this.bCol = 1F;
            this.rCol = 0.3F;
            this.gCol = 0.8F;
        }

    }

}
