package dev.shadowsoffire.apothic_attributes.modifiers;

import java.util.function.Predicate;

import com.mojang.serialization.Codec;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.api.ALObjects.BuiltInRegs;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;

public record EntitySlotGroup(ResourceLocation id, HolderSet<EntityEquipmentSlot> slots) implements Predicate<Holder<EntityEquipmentSlot>> {

    public static final Codec<EntitySlotGroup> CODEC = BuiltInRegs.ENTITY_SLOT_GROUP.byNameCodec();
    public static final StreamCodec<RegistryFriendlyByteBuf, EntitySlotGroup> STREAM_CODEC = ByteBufCodecs.registry(BuiltInRegs.ENTITY_SLOT_GROUP.key());

    @Override
    public boolean test(Holder<EntityEquipmentSlot> t) {
        return this.slots.contains(t);
    }

    /**
     * Converts a vanilla {@link EquipmentSlotGroup} to an apoth {@link EntitySlotGroup}.
     */
    public static EntitySlotGroup fromVanilla(EquipmentSlotGroup group) {
        return switch (group) {
            case ANY -> ALObjects.EquipmentSlotGroups.ANY_VANILLA;
            case MAINHAND -> ALObjects.EquipmentSlotGroups.MAINHAND;
            case OFFHAND -> ALObjects.EquipmentSlotGroups.OFFHAND;
            case HAND -> ALObjects.EquipmentSlotGroups.HAND;
            case FEET -> ALObjects.EquipmentSlotGroups.FEET;
            case LEGS -> ALObjects.EquipmentSlotGroups.LEGS;
            case CHEST -> ALObjects.EquipmentSlotGroups.CHEST;
            case HEAD -> ALObjects.EquipmentSlotGroups.HEAD;
            case ARMOR -> ALObjects.EquipmentSlotGroups.ARMOR;
            case BODY -> ALObjects.EquipmentSlotGroups.BODY;
        };
    }

}
