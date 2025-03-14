package dev.shadowsoffire.apothic_attributes.modifiers;

import com.mojang.serialization.Codec;

import dev.shadowsoffire.apothic_attributes.api.ALObjects.BuiltInRegs;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Interface which represents an equipment slot type for a living entity. This is an extensible version of {@link EquipmentSlot}.
 * 
 * @see {@link EquipmentSlotCompat} for interop code between this class and {@link EquipmentSlot}.
 */
public interface EntityEquipmentSlot {

    public static final Codec<Holder<EntityEquipmentSlot>> CODEC = BuiltInRegs.ENTITY_EQUIPMENT_SLOT.holderByNameCodec();
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<EntityEquipmentSlot>> STREAM_CODEC = ByteBufCodecs.holderRegistry(BuiltInRegs.ENTITY_EQUIPMENT_SLOT.key());

    /**
     * Returns all items that are in this slot on the target entity.
     * <p>
     * TODO: Determine if this should be a single ItemStack, and require multiple EES objects for multi-indexed or multi-item slots.
     * 
     * @param entity
     * @return
     */
    Iterable<ItemStack> getStacks(LivingEntity entity);

}
