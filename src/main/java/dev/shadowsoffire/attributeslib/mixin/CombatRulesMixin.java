package dev.shadowsoffire.attributeslib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import dev.shadowsoffire.attributeslib.AttributesLib;
import dev.shadowsoffire.attributeslib.api.ALCombatRules;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

@Mixin(value = CombatRules.class, remap = false)
public class CombatRulesMixin {

    /**
     * @author Shadows
     * @reason Changing combat rules to reflect custom formulas.
     * @see {@link ALCombatRules#getDamageAfterProtection(net.minecraft.world.entity.LivingEntity, net.minecraft.world.damagesource.DamageSource, float, float)}
     */
    @Overwrite
    public static float getDamageAfterMagicAbsorb(float damage, float protPoints) {
        return damage * ALCombatRules.getProtDamageReduction(protPoints);
    }

    /**
     * @author Shadows
     * @reason Changing combat rules to reflect custom formulas.
     * @see {@link ALCombatRules#getDamageAfterArmor(LivingEntity, DamageSource, float, float, float)}
     */
    @Overwrite
    public static float getDamageAfterAbsorb(float damage, float armor, float toughness) {
        AttributesLib.LOGGER.trace("Invocation of CombatRules#getDamageAfterAbsorb is bypassing armor pen.");
        return damage * ALCombatRules.getArmorDamageReduction(damage, armor);
    }
}
