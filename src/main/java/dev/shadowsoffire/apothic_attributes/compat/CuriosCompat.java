package dev.shadowsoffire.apothic_attributes.compat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.CurioAttributeModifiers;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosSlotTypes;
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

            @SuppressWarnings("removal")
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
                                Identifier id = CuriosApi.getSlotId(slotContext);
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
     * Bridges Curios with {@link StackAttributeModifiers}.
     * <p>
     * For this to work, a mod wanting compat with a specific curio slot needs to register both a
     * {@link CurioEquipmentSlot} and a {@link EntitySlotGroup} with matching keys.
     */
    public static void stackAttrModifierCompat(CurioAttributeModifierEvent e) {
        List<CurioAttributeModifiers.Entry> modifiers = e.getImmutableModifiers();
        Map<String, EntitySlotGroup> groupBySlotId = new LinkedHashMap<>();
        Set<CurioAttributeModifiers.Entry> handled = new HashSet<>();

        // Build a mapping from curio slot types to Apothic slot groups.
        for (String curio : CuriosSlotTypes.getSlotTypes(EffectiveSide.get().isClient()).keySet()) {
            Holder<EntityEquipmentSlot> curioSlot = getSlotForCurio(curio);
            if (curioSlot != null) {
                EntitySlotGroup group = BuiltInRegs.ENTITY_SLOT_GROUP.get(curioSlot.getKey().identifier()).map(Holder::value).orElse(null);
                if (group != null) {
                    groupBySlotId.put(curio, group);
                }
            }
        }

        // Go through the existing modifiers, mark ones that match an Apoth slot group, and record them in a StackAttributeModifiers builder.
        var builder = StackAttributeModifiers.builder();
        for (CurioAttributeModifiers.Entry entry : modifiers) {
            for (String slotId : entry.slotType().id()) {
                EntitySlotGroup group = groupBySlotId.get(slotId);
                if (group != null) {
                    handled.add(entry);
                    builder.add(entry.attributeHolder(), entry.modifier(), group);
                }
            }
        }

        // Fire our event, and bail if no consumers changed anything.
        var event = new StackAttributeModifiersEvent(e.getItemStack(), builder.build());
        NeoForge.EVENT_BUS.post(event);
        if (!event.hasChanges()) return;

        // If consumers did change something, nuke everything we might handle and rebuild from our event.
        e.removeIf(handled::contains);

        StackAttributeModifiers newModifs = event.build();
        for (StackAttributeModifiers.Entry newEntry : newModifs.modifiers()) {
            for (Map.Entry<String, EntitySlotGroup> groupEntry : groupBySlotId.entrySet()) {
                Holder<EntityEquipmentSlot> slotHolder = getSlotForCurio(groupEntry.getKey());
                if (slotHolder != null && newEntry.slots().test(slotHolder)) {
                    e.addModifier(newEntry.attribute(), newEntry.modifier(), groupEntry.getKey());
                }
            }
        }
    }

}
