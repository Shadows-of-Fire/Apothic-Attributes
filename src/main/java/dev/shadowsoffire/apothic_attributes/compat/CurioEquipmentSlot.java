package dev.shadowsoffire.apothic_attributes.compat;

import java.util.Collections;
import java.util.Iterator;
import java.util.stream.IntStream;

import dev.shadowsoffire.apothic_attributes.modifiers.EntityEquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public record CurioEquipmentSlot(String curioType) implements EntityEquipmentSlot {

    @Override
    public Iterable<ItemStack> getStacks(LivingEntity entity) {
        ICuriosItemHandler handler = CuriosApi.getCuriosInventory(entity).orElse(null);
        if (handler != null) {
            ICurioStacksHandler stacks = handler.getCurios().get(this.curioType);
            if (stacks != null) {
                return new CurioStackIterator(stacks.getStacks());
            }
        }
        return Collections.emptyList();
    }

    private static record CurioStackIterator(IDynamicStackHandler handler) implements Iterable<ItemStack> {

        @Override
        public Iterator<ItemStack> iterator() {
            return IntStream.rangeClosed(0, handler.getSlots()).mapToObj(handler::getStackInSlot).iterator();
        }

    }

}
