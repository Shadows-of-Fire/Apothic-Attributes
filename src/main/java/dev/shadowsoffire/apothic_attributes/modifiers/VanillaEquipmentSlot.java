package dev.shadowsoffire.apothic_attributes.modifiers;

import java.util.List;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public record VanillaEquipmentSlot(EquipmentSlot slot) implements EntityEquipmentSlot {

    @Override
    public Iterable<ItemStack> getStacks(LivingEntity entity) {
        return List.of(entity.getItemBySlot(this.slot));
    }

}
