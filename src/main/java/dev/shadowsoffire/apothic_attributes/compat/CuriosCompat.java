package dev.shadowsoffire.apothic_attributes.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Multimap;

import dev.shadowsoffire.apothic_attributes.api.ALObjects.BuiltInRegs;
import dev.shadowsoffire.apothic_attributes.client.ModifierSource;
import dev.shadowsoffire.apothic_attributes.client.ModifierSource.ItemModifierSource;
import dev.shadowsoffire.apothic_attributes.client.ModifierSourceType;
import dev.shadowsoffire.apothic_attributes.modifiers.EntityEquipmentSlot;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiers;
import dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiersEvent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public class CuriosCompat {

    private static final Map<String, Holder<EntityEquipmentSlot>> CURIO_TYPE_TO_EQUIPMENT_SLOT = new HashMap<>();

    static {
        if (!ModList.get().isLoaded("curios")) {
            throw new UnsupportedOperationException("This optional compat class requires Curios to be loaded.");
        }
    }

    public static void init() {
        ModifierSourceType.register(new ModifierSourceType<>(){

            @Override
            public void extract(LivingEntity entity, BiConsumer<AttributeModifier, ModifierSource<?>> map) {
                CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
                    Map<String, ICurioStacksHandler> curios = handler.getCurios();
                    for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
                        ICurioStacksHandler stacksHandler = entry.getValue();
                        String identifier = entry.getKey();
                        IDynamicStackHandler stackHandler = stacksHandler.getStacks();

                        for (int i = 0; i < stacksHandler.getSlots(); i++) {
                            SlotContext slotContext = new SlotContext(identifier, entity, i, false, true);
                            ItemStack stack = stackHandler.getStackInSlot(i);
                            if (!stack.isEmpty()) {
                                ResourceLocation id = CuriosApi.getSlotId(slotContext);
                                Multimap<Holder<Attribute>, AttributeModifier> modifiers = CuriosApi.getAttributeModifiers(slotContext, id, stack);
                                ModifierSource<?> src = new ItemModifierSource(stack);
                                modifiers.values().forEach(m -> map.accept(m, src));
                            }
                        }
                    }
                });
            }

            @Override
            public int getPriority() {
                return 20;
            }
        });

        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, CuriosCompat::stackAttrModifierCompat);
    }

    @Nullable
    public static synchronized Holder<EntityEquipmentSlot> getSlotForCurio(String curioType) {
        return CURIO_TYPE_TO_EQUIPMENT_SLOT.computeIfAbsent(curioType, key -> {
            for (EntityEquipmentSlot slot : BuiltInRegs.ENTITY_EQUIPMENT_SLOT) {
                if (slot instanceof CurioEquipmentSlot curioSlot && curioSlot.curioType().equals(key)) {
                    return BuiltInRegs.ENTITY_EQUIPMENT_SLOT.wrapAsHolder(slot);
                }
            }
            return null;
        });
    }

    /**
     * This method attempts to bridge Curios with {@link StackAttributeModifiers}.
     * <p>
     * For this to work, a mod wanting compat with a specific curio slot needs to register
     * both a {@link CurioEquipmentSlot} and a {@link EntitySlotGroup} with the same key.
     * <p>
     * This method will then try to map a curio slot type to a {@link EntityEquipmentSlot} by iterating the registry for a matching {@link CurioEquipmentSlot}.
     * If one is found, it will fire the {@link StackAttributeModifiersEvent}, with the following contracts:
     * <ol>
     * <li>If a matching {@link EntitySlotGroup} is found, the base curio modifiers will be visible in the event</li>
     * <li>Modifiers for any {@link EntitySlotGroup} that contain the "selected" {@link CurioEquipmentSlot} will be reflected in the curio</li>
     * </ol>
     * Unfortunately this falls apart if multiple mods try to create equipment slots and groups for the same curio slot. Not sure what to do about that just yet.
     */
    public static void stackAttrModifierCompat(CurioAttributeModifierEvent e) {
        Holder<EntityEquipmentSlot> curioSlot = getSlotForCurio(e.getSlotContext().identifier());
        if (curioSlot != null) {
            var builder = StackAttributeModifiers.builder();
            // Try to find a group with the same name, if it exists.
            // The mod which created the equipment slot should have also created the group object.
            EntitySlotGroup group = BuiltInRegs.ENTITY_SLOT_GROUP.get(curioSlot.getKey().location());
            if (group != null) {
                e.getModifiers().forEach((attr, modif) -> {
                    builder.add(attr, modif, group);
                });
            }

            var event = new StackAttributeModifiersEvent(e.getItemStack(), builder.build());
            NeoForge.EVENT_BUS.post(event);

            if (event.hasChanges()) {
                // Only wipe the original modifiers if we were able to populate them to the stack event.
                if (group != null) {
                    e.clearModifiers();
                }
                StackAttributeModifiers newModifs = event.build();

                for (StackAttributeModifiers.Entry entry : newModifs.modifiers()) {
                    if (entry.slots().test(curioSlot)) {
                        e.addModifier(entry.attribute(), entry.modifier());
                    }
                }
            }
        }
    }

}
