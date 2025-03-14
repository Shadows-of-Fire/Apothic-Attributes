package dev.shadowsoffire.apothic_attributes.modifiers;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;

/**
 * Helper class for converting between {@link EquipmentSlot} / {@link EntityEquipmentSlot} and {@link EquipmentSlotGroup} / {@link EntitySlotGroup}.
 */
public class EquipmentSlotCompat {

    private static final BiMap<EquipmentSlot, Holder<EntityEquipmentSlot>> SLOT_MAP = Util.make(HashBiMap.create(7), map -> {
        map.put(EquipmentSlot.MAINHAND, ALObjects.EquipmentSlots.MAINHAND);
        map.put(EquipmentSlot.OFFHAND, ALObjects.EquipmentSlots.OFFHAND);
        map.put(EquipmentSlot.HEAD, ALObjects.EquipmentSlots.HEAD);
        map.put(EquipmentSlot.CHEST, ALObjects.EquipmentSlots.CHEST);
        map.put(EquipmentSlot.LEGS, ALObjects.EquipmentSlots.LEGS);
        map.put(EquipmentSlot.FEET, ALObjects.EquipmentSlots.FEET);
        map.put(EquipmentSlot.BODY, ALObjects.EquipmentSlots.BODY);
    });

    private static final BiMap<EquipmentSlotGroup, EntitySlotGroup> GROUP_MAP = Util.make(HashBiMap.create(10), map -> {
        map.put(EquipmentSlotGroup.ANY, ALObjects.EquipmentSlotGroups.ANY_VANILLA);
        map.put(EquipmentSlotGroup.MAINHAND, ALObjects.EquipmentSlotGroups.MAINHAND);
        map.put(EquipmentSlotGroup.OFFHAND, ALObjects.EquipmentSlotGroups.OFFHAND);
        map.put(EquipmentSlotGroup.HAND, ALObjects.EquipmentSlotGroups.HAND);
        map.put(EquipmentSlotGroup.HEAD, ALObjects.EquipmentSlotGroups.HEAD);
        map.put(EquipmentSlotGroup.CHEST, ALObjects.EquipmentSlotGroups.CHEST);
        map.put(EquipmentSlotGroup.LEGS, ALObjects.EquipmentSlotGroups.LEGS);
        map.put(EquipmentSlotGroup.FEET, ALObjects.EquipmentSlotGroups.FEET);
        map.put(EquipmentSlotGroup.ARMOR, ALObjects.EquipmentSlotGroups.ARMOR);
        map.put(EquipmentSlotGroup.BODY, ALObjects.EquipmentSlotGroups.BODY);
    });

    /**
     * Converts an apoth {@link EntityEquipmentSlot} to a vanilla {@link EquipmentSlot}.
     */
    @Nullable
    public static EquipmentSlot toVanilla(Holder<EntityEquipmentSlot> slot) {
        return SLOT_MAP.inverse().get(slot);
    }

    /**
     * Converts a vanilla {@link EquipmentSlot} to an apoth {@link EntityEquipmentSlot}.
     */
    @Nullable
    public static Holder<EntityEquipmentSlot> fromVanilla(EquipmentSlot slot) {
        return SLOT_MAP.get(slot);
    }

    /**
     * Converts an apoth {@link EntitySlotGroup} to a vanilla {@link EquipmentSlotGroup}.
     */
    @Nullable
    public static EquipmentSlotGroup toVanilla(EntitySlotGroup group) {
        return GROUP_MAP.inverse().get(group);
    }

    /**
     * Converts a vanilla {@link EquipmentSlotGroup} to an apoth {@link EntitySlotGroup}.
     */
    @Nullable
    public static EntitySlotGroup fromVanilla(EquipmentSlotGroup group) {
        return GROUP_MAP.get(group);
    }

}
