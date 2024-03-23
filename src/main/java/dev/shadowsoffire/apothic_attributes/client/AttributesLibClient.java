package dev.shadowsoffire.apothic_attributes.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;

import dev.shadowsoffire.apothic_attributes.ALConfig;
import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.api.AttributeHelper;
import dev.shadowsoffire.apothic_attributes.api.IFormattableAttribute;
import dev.shadowsoffire.apothic_attributes.api.client.AddAttributeTooltipsEvent;
import dev.shadowsoffire.apothic_attributes.api.client.GatherEffectScreenTooltipsEvent;
import dev.shadowsoffire.apothic_attributes.api.client.GatherSkippedAttributeTooltipsEvent;
import dev.shadowsoffire.apothic_attributes.util.IFlying;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CritParticle;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents;
import net.minecraft.world.effect.AttributeModifierTemplate;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStack.TooltipPart;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class AttributesLibClient {

    private static final UUID FAKE_MERGED_UUID = UUID.fromString("a6b0ac71-e435-416e-a991-7623eaa129a4");

    @SubscribeEvent
    public void updateClientFlyStateOnRespawn(ClientPlayerNetworkEvent.Clone e) {
        // Teleporting to another dimension constitutes a respawn - the other checks we have ensure the mayFly state returns, but not the flying state.
        // For this one we have to ensure that the state is marked to be restored before MultiPlayerGameMode#adjustPlayer is called in
        // ClientPacketListener#handleRespawn.
        if (e.getOldPlayer().getAbilities().flying) {
            ((IFlying) e.getNewPlayer()).markFlying();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void tooltips(ItemTooltipEvent e) {
        ItemStack stack = e.getItemStack();
        List<Component> list = e.getToolTip();
        int markIdx1 = -1, markIdx2 = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getContents() instanceof LiteralContents tc) {
                if ("APOTH_REMOVE_MARKER".equals(tc.text())) {
                    markIdx1 = i;
                }
                if ("APOTH_REMOVE_MARKER_2".equals(tc.text())) {
                    markIdx2 = i;
                    break;
                }
            }
        }
        if (markIdx1 == -1 || markIdx2 == -1) return;
        var it = list.listIterator(markIdx1);
        for (int i = markIdx1; i < markIdx2 + 1; i++) {
            it.next();
            it.remove();
        }
        int flags = getHideFlags(stack);
        if (shouldShowInTooltip(flags, TooltipPart.MODIFIERS)) {
            applyModifierTooltips(e.getEntity(), stack, it::add, e.getFlags());
        }
        NeoForge.EVENT_BUS.post(new AddAttributeTooltipsEvent(stack, e.getEntity(), list, it, e.getFlags()));
    }

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
        MobEffect effect = effectInst.getEffect();

        MutableComponent name = (MutableComponent) tooltips.get(0);
        Component duration = tooltips.remove(1);
        duration = Component.translatable("(%s)", duration).withStyle(ChatFormatting.WHITE);

        name.append(" ").append(duration);

        if (ApothicAttributes.getTooltipFlag().isAdvanced()) {
            name.append(" ").append(Component.translatable("[%s]", BuiltInRegistries.MOB_EFFECT.getKey(effect)).withStyle(ChatFormatting.GRAY));
        }

        String key = effect.getDescriptionId() + ".desc";
        if (I18n.exists(key)) {
            tooltips.add(Component.translatable(key).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        }
        else if (ApothicAttributes.getTooltipFlag().isAdvanced() && effect.getAttributeModifiers().isEmpty()) {
            tooltips.add(Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        List<Pair<Attribute, AttributeModifier>> list = Lists.newArrayList();
        Map<Attribute, AttributeModifierTemplate> map = effect.getAttributeModifiers();
        if (!map.isEmpty()) {
            for (Map.Entry<Attribute, AttributeModifierTemplate> entry : map.entrySet()) {
                AttributeModifierTemplate template = entry.getValue();
                list.add(new Pair<>(entry.getKey(), template.create(effectInst.getAmplifier())));
            }
        }

        if (!list.isEmpty()) {
            tooltips.add(CommonComponents.EMPTY);
            for (Pair<Attribute, AttributeModifier> pair : list) {
                tooltips.add(IFormattableAttribute.toComponent(pair.getFirst(), pair.getSecond(), ApothicAttributes.getTooltipFlag()));
            }
        }
    }

    @SubscribeEvent
    public void potionTooltips(ItemTooltipEvent e) {
        if (!ALConfig.enablePotionTooltips) return;

        ItemStack stack = e.getItemStack();
        List<Component> tooltips = e.getToolTip();

        if (stack.getItem() instanceof PotionItem) {
            List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);
            if (effects.size() == 1 && tooltips.size() >= 2) {
                MobEffect effect = effects.get(0).getEffect();
                String key = effect.getDescriptionId() + ".desc";
                if (I18n.exists(key)) {
                    tooltips.add(2, Component.translatable(key).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
                    tooltips.add(3, CommonComponents.EMPTY);
                }
                else if (e.getFlags().isAdvanced() && effect.getAttributeModifiers().isEmpty()) {
                    tooltips.add(2, Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                    tooltips.add(3, CommonComponents.EMPTY);
                }
            }
        }
    }

    public static Multimap<Attribute, AttributeModifier> getSortedModifiers(ItemStack stack, EquipmentSlot slot) {
        var unsorted = stack.getAttributeModifiers(slot);
        Multimap<Attribute, AttributeModifier> map = AttributeHelper.sortedMap();
        for (Map.Entry<Attribute, AttributeModifier> ent : unsorted.entries()) {
            if (ent.getKey() != null && ent.getValue() != null) map.put(ent.getKey(), ent.getValue());
            else ApothicAttributes.LOGGER.debug("Detected broken attribute modifier entry on item {}.  Attr={}, Modif={}", stack, ent.getKey(), ent.getValue());
        }
        return map;
    }

    public static void apothCrit(int entityId) {
        Entity entity = Minecraft.getInstance().level.getEntity(entityId);
        if (entity != null) {
            Minecraft.getInstance().particleEngine.createTrackingEmitter(entity, ALObjects.Particles.APOTH_CRIT.get());
        }
    }

    private static boolean shouldShowInTooltip(int pHideFlags, ItemStack.TooltipPart pPart) {
        return (pHideFlags & pPart.getMask()) == 0;
    }

    private static int getHideFlags(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("HideFlags", 99) ? stack.getTag().getInt("HideFlags") : stack.getItem().getDefaultTooltipHideFlags(stack);
    }

    private static void applyModifierTooltips(@Nullable Player player, ItemStack stack, Consumer<Component> tooltip, TooltipFlag flag) {
        Multimap<Attribute, AttributeModifier> mainhand = getSortedModifiers(stack, EquipmentSlot.MAINHAND);
        Multimap<Attribute, AttributeModifier> offhand = getSortedModifiers(stack, EquipmentSlot.OFFHAND);
        Multimap<Attribute, AttributeModifier> dualHand = AttributeHelper.sortedMap();
        for (Attribute atr : mainhand.keys()) {
            Collection<AttributeModifier> modifMh = mainhand.get(atr);
            Collection<AttributeModifier> modifOh = offhand.get(atr);
            modifMh.stream().filter(a1 -> modifOh.stream().anyMatch(a2 -> a1.getId().equals(a2.getId()))).forEach(modif -> dualHand.put(atr, modif));
        }

        dualHand.values().forEach(m -> {
            mainhand.values().remove(m);
            offhand.values().removeIf(m1 -> m1.getId().equals(m.getId()));
        });

        Set<UUID> skips = new HashSet<>();
        NeoForge.EVENT_BUS.post(new GatherSkippedAttributeTooltipsEvent(stack, player, skips, flag));

        applyTextFor(player, stack, tooltip, dualHand, "both_hands", skips, flag);
        applyTextFor(player, stack, tooltip, mainhand, EquipmentSlot.MAINHAND.getName(), skips, flag);
        applyTextFor(player, stack, tooltip, offhand, EquipmentSlot.OFFHAND.getName(), skips, flag);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.ordinal() < 2) continue;
            Multimap<Attribute, AttributeModifier> modifiers = getSortedModifiers(stack, slot);
            applyTextFor(player, stack, tooltip, modifiers, slot.getName(), skips, flag);
        }
    }

    private static MutableComponent padded(String padding, Component comp) {
        return Component.literal(padding).append(comp);
    }

    private static MutableComponent list() {
        return AttributeHelper.list();
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

    private static record BaseModifier(AttributeModifier base, List<AttributeModifier> children) {}

    private static void applyTextFor(@Nullable Player player, ItemStack stack, Consumer<Component> tooltip, Multimap<Attribute, AttributeModifier> modifierMap, String group, Set<UUID> skips, TooltipFlag flag) {
        if (!modifierMap.isEmpty()) {
            modifierMap.values().removeIf(m -> skips.contains(m.getId()));

            tooltip.accept(Component.empty());
            tooltip.accept(Component.translatable("item.modifiers." + group).withStyle(ChatFormatting.GRAY));

            if (modifierMap.isEmpty()) return;

            Map<Attribute, BaseModifier> baseModifs = new IdentityHashMap<>();

            modifierMap.forEach((attr, modif) -> {
                if (modif.getId().equals(((IFormattableAttribute) attr).getBaseUUID())) {
                    baseModifs.put(attr, new BaseModifier(modif, new ArrayList<>()));
                }
            });

            modifierMap.forEach((attr, modif) -> {
                BaseModifier base = baseModifs.get(attr);
                if (base != null && base.base != modif) {
                    base.children.add(modif);
                }
            });

            for (Map.Entry<Attribute, BaseModifier> entry : baseModifs.entrySet()) {
                Attribute attr = entry.getKey();
                BaseModifier baseModif = entry.getValue();
                double entityBase = player == null ? 0 : player.getAttributeBaseValue(attr);
                double base = baseModif.base.getAmount() + entityBase;
                final double rawBase = base;
                double amt = base;
                double baseBonus = ((IFormattableAttribute) attr).getBonusBaseValue(stack);
                for (AttributeModifier modif : baseModif.children) {
                    if (modif.getOperation() == Operation.ADDITION) base = amt = amt + modif.getAmount();
                    else if (modif.getOperation() == Operation.MULTIPLY_BASE) amt += modif.getAmount() * base;
                    else amt *= 1 + modif.getAmount();
                }
                amt += baseBonus;
                boolean isMerged = !baseModif.children.isEmpty() || baseBonus != 0;
                MutableComponent text = IFormattableAttribute.toBaseComponent(attr, amt, entityBase, isMerged, flag);
                tooltip.accept(padded(" ", text).withStyle(isMerged ? ChatFormatting.GOLD : ChatFormatting.DARK_GREEN));
                if (Screen.hasShiftDown() && isMerged) {
                    // Display the raw base value, and then all children modifiers.
                    text = IFormattableAttribute.toBaseComponent(attr, rawBase, entityBase, false, flag);
                    tooltip.accept(list().append(text.withStyle(ChatFormatting.DARK_GREEN)));
                    for (AttributeModifier modifier : baseModif.children) {
                        tooltip.accept(list().append(IFormattableAttribute.toComponent(attr, modifier, flag)));
                    }
                    if (baseBonus > 0) {
                        ((IFormattableAttribute) attr).addBonusTooltips(stack, tooltip, flag);
                    }
                }
            }

            for (Attribute attr : modifierMap.keySet()) {
                if (baseModifs.containsKey(attr)) continue;
                Collection<AttributeModifier> modifs = modifierMap.get(attr);
                // Initiate merged-tooltip logic if we have more than one modifier for a given attribute.
                if (modifs.size() > 1) {
                    double[] sums = new double[3];
                    boolean[] merged = new boolean[3];
                    Map<Operation, List<AttributeModifier>> shiftExpands = new HashMap<>();
                    for (AttributeModifier modifier : modifs) {
                        if (modifier.getAmount() == 0) continue;
                        if (sums[modifier.getOperation().ordinal()] != 0) merged[modifier.getOperation().ordinal()] = true;
                        sums[modifier.getOperation().ordinal()] += modifier.getAmount();
                        shiftExpands.computeIfAbsent(modifier.getOperation(), k -> new LinkedList<>()).add(modifier);
                    }
                    for (Operation op : Operation.values()) {
                        int i = op.ordinal();
                        if (sums[i] == 0) continue;
                        if (merged[i]) {
                            TextColor color = sums[i] < 0 ? TextColor.fromRgb(0xF93131) : TextColor.fromRgb(0x7A7AF9);
                            if (sums[i] < 0) sums[i] *= -1;
                            var fakeModif = new AttributeModifier(FAKE_MERGED_UUID, ApothicAttributes.MODID + ":merged", sums[i], op);
                            MutableComponent comp = IFormattableAttribute.toComponent(attr, fakeModif, flag);
                            tooltip.accept(comp.withStyle(comp.getStyle().withColor(color)));
                            if (merged[i] && Screen.hasShiftDown()) {
                                shiftExpands.get(Operation.fromValue(i)).forEach(modif -> tooltip.accept(list().append(IFormattableAttribute.toComponent(attr, modif, flag))));
                            }
                        }
                        else {
                            var fakeModif = new AttributeModifier(FAKE_MERGED_UUID, ApothicAttributes.MODID + ":merged", sums[i], op);
                            tooltip.accept(IFormattableAttribute.toComponent(attr, fakeModif, flag));
                        }
                    }
                }
                else modifs.forEach(m -> {
                    if (m.getAmount() != 0) tooltip.accept(IFormattableAttribute.toComponent(attr, m, flag));
                });
            }
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
