package dev.shadowsoffire.attributeslib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import dev.shadowsoffire.attributeslib.AttributesLib;
import dev.shadowsoffire.attributeslib.api.ALCombatRules;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

@Mixin(CombatRules.class)
public class CombatRulesMixin {

    /**
     * @see {@link ALCombatRules#getDamageAfterProtection(net.minecraft.world.entity.LivingEntity, net.minecraft.world.damagesource.DamageSource, float, float)}
     */
    @ModifyReturnValue(method = "getDamageLeft", at = @At("RETURN"))
    public static float getDamageAfterMagicAbsorb(float original, float damage, float protPoints) {
        return damage * ALCombatRules.getProtDamageReduction(protPoints);
    }

    /**
     * @see {@link ALCombatRules#getDamageAfterArmor(LivingEntity, DamageSource, float, float, float)}
     */
    @ModifyReturnValue(method = "getInflictedDamage", at = @At("RETURN"))
    public static float getDamageAfterAbsorb(float original, float damage, float armor, float toughness) {
        AttributesLib.LOGGER.trace("Invocation of CombatRules#getDamageAfterAbsorb is bypassing armor pen.");
        return damage * ALCombatRules.getArmorDamageReduction(damage, armor);
    }
}
