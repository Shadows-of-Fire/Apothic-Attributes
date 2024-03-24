package dev.shadowsoffire.apothic_attributes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(value = ThrownTrident.class, remap = false)
public class ThrownTridentMixin extends AbstractArrow {

    protected ThrownTridentMixin(EntityType<? extends AbstractArrow> type, Level level, ItemStack stack) {
        super(type, level, stack);
    }

    @ModifyConstant(method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V", constant = @Constant(floatValue = 8.0F))
    public float apoth_getTridentDamage(float defaultDmg) {
        return (float) (this.getBaseDamage() * 4.0F);
    }

}
