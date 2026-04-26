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
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.CurioAttributeModifiers;
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
     * <p>
     * Curios 15.x reshaped {@link CurioAttributeModifierEvent} from per-slot to per-item: each
     * {@link CurioAttributeModifiers.Entry default modifier} carries its own
     * {@link top.theillusivec4.curios.api.common.slot.SlotTypePredicate slot-type predicate}, which lists the
     * curio slot ids it applies to. We walk those entries, group them by the {@link EntitySlotGroup} that maps
     * to each candidate slot, fire {@link StackAttributeModifiersEvent} with the grouped result, and (if any
     * mod modified the modifiers) replace the original entries with slot-id-scoped versions of the new ones.
     */
    public static void stackAttrModifierCompat(CurioAttributeModifierEvent e) {
        List<CurioAttributeModifiers.Entry> modifiers = e.getImmutableModifiers();
        if (modifiers.isEmpty()) return;

        // Build a curio-slot-id → EntitySlotGroup map for every slot id covered by the defaults' predicates,
        // and remember which entries we successfully mapped to a group. Entries whose predicates don't resolve
        // to any known group are left alone for Curios to handle as-is.
        Map<String, EntitySlotGroup> groupBySlotId = new LinkedHashMap<>();
        Set<CurioAttributeModifiers.Entry> handled = new HashSet<>();

        for (CurioAttributeModifiers.Entry entry : modifiers) {
            for (String slotId : entry.slotType().id()) {
                EntitySlotGroup group = groupBySlotId.computeIfAbsent(slotId, sid -> {
                    Holder<EntityEquipmentSlot> slotHolder = getSlotForCurio(sid);
                    if (slotHolder == null) return null;
                    return BuiltInRegs.ENTITY_SLOT_GROUP.get(slotHolder.getKey().identifier())
                        .map(Holder::value)
                        .orElse(null);
                });
                if (group != null) {
                    handled.add(entry);
                }
            }
        }

        var builder = StackAttributeModifiers.builder();
        if (!handled.isEmpty()) {
            // Stage the handled entries into a StackAttributeModifiers, scoped to the first matching group per
            // entry. Entries that match multiple groups are duplicated across them, mirroring how Curios would
            // have applied the same modifier across multiple equipped slots.
            for (CurioAttributeModifiers.Entry entry : modifiers) {
                if (!handled.contains(entry)) continue;
                for (String slotId : entry.slotType().id()) {
                    EntitySlotGroup group = groupBySlotId.get(slotId);
                    if (group != null) {
                        builder.add(entry.attributeHolder(), entry.modifier(), group);
                    }
                }
            }
        }

        var event = new StackAttributeModifiersEvent(e.getItemStack(), builder.build());
        NeoForge.EVENT_BUS.post(event);
        if (!event.hasChanges()) return;

        // The other-mod listeners modified the modifiers; remove the original Curios entries we mapped, and
        // emit the resulting StackAttributeModifiers entries back into the event scoped to whichever curio
        // slot ids correspond to the entry's resolved EntityEquipmentSlot set.
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
