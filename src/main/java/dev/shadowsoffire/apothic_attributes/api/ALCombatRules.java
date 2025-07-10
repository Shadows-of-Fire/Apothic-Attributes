package dev.shadowsoffire.apothic_attributes.api;

import java.math.BigDecimal;

import dev.shadowsoffire.apothic_attributes.ALConfig;
import dev.shadowsoffire.apothic_attributes.api.ALObjects.Attributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Contains AL-specific combat calculations for armor and protection values.
 */
public class ALCombatRules {

    /**
     * Gets the amount of damage the user would take after applying protection points and protection bypass.<br>
     * Protection bypass is based on {@linkplain Attributes#PROT_PIERCE Protection Pierce} and {@linkplain Attributes#PROT_SHRED Protection Shred}.
     * <p>
     * This is not invoked if the user does not have any protection points, and excess protection bypass has no effect.
     *
     * @param target     The attack's target.
     * @param src        The DamageSource of the attack.
     * @param amount     The amount of damage the attack is currently dealing, after armor has been applied.
     * @param protPoints The amount of protection points the target has against the incoming damage source.
     * @return The modified damage value, after applying protection points, accounting for the attacker's bypass.
     */
    public static float getDamageAfterProtection(LivingEntity target, DamageSource src, float amount, float protPoints) {
        if (src.getEntity() instanceof LivingEntity attacker) {
            float shred = (float) attacker.getAttributeValue(Attributes.PROT_SHRED);
            if (shred > 0.001F) {
                protPoints *= 1 - shred;
            }
            float pierce = (float) attacker.getAttributeValue(Attributes.PROT_PIERCE);
            if (pierce > 0.001F) {
                protPoints -= pierce;
            }
        }

        if (protPoints <= 0) return amount;
        return amount * getProtDamageReduction(protPoints);
    }

    /**
     * Computes the damage reduction factor for the given amount of protection points.<br>
     * Each protection point reduces damage by 2.5%, up to 85%.
     * <p>
     * In vanilla, each protection point reduces damage by 4%, up to 80%.
     * <p>
     * This expression may be configured in the apothic_attributes.cfg file.
     *
     * @see #getDamageAfterProtection(LivingEntity, DamageSource, float, float)
     */
    public static float getProtDamageReduction(float protPoints) {
        if (ALConfig.getProtExpr().isPresent()) {
            return ALConfig.getProtExpr().get().setVariable("protPoints", new BigDecimal(protPoints)).eval().floatValue();
        }
        return 1 - Math.min(0.025F * protPoints, 0.85F);
    }

    /**
     * Gets the amount of damage the user would take after applying armor, toughness, and armor bypass.<br>
     * Armor bypass is based on {@linkplain Attributes#ARMOR_PIERCE Armor Pierce} and {@linkplain Attributes#ARMOR_SHRED Armor Shred}.
     * <p>
     * Unlike protection bypass, additional armor bypass will cause unarmored targets to take additional damage.<br>
     * Not invoked if the incoming damage source {@linkplain DamageSource#isBypassArmor() bypasses armor}.
     * <p>
     * With the introduction of this attribute, armor toughness acts as a shield against armor bypass.<br>
     * Each point of armor toughness reduces the effectiveness of all armor bypass by 2%, up to 60%.<br>
     * That said, armor toughness no longer reduces damage, and only reduces armor bypass.
     *
     * @param target    The attack's target.
     * @param src       The DamageSource of the attack.
     * @param amount    The amount of damage the attack is currently dealing, after armor has been applied.
     * @param armor     The amount of armor points the target has.
     * @param toughness The amount of armor toughness points the target has.
     * @return The modified damage value, after applying armor, accounting for the attacker's bypass.
     */
    public static float getDamageAfterArmor(LivingEntity target, DamageSource src, float amount, float armor, float toughness) {
        if (src.getEntity() instanceof LivingEntity attacker) {
            float shred = (float) attacker.getAttributeValue(Attributes.ARMOR_SHRED);
            float bypassResist = getBypassResistance(amount, armor, toughness);
            if (shred > 0.001F) {
                shred *= 1 - bypassResist;
                armor *= 1 - shred;
            }
            float pierce = (float) attacker.getAttributeValue(Attributes.ARMOR_PIERCE);
            if (pierce > 0.001F) {
                pierce *= 1 - bypassResist;
                armor -= pierce;
            }
        }

        if (armor <= 0) {
            return amount + (ALConfig.negativeArmorFactor * -armor * amount);
        }

        float reduction = getArmorDamageReduction(amount, armor, toughness);
        if (src.getWeaponItem() != null && target.level() instanceof ServerLevel serverlevel) {
            // Normally we just work with the reduction, or the multiplier on the final damage value.
            // However, the vanilla ARMOR_EFFECTIVENESS enchantment effect works on the "effectiveness", which is how much damage the armor would block.
            // So we have to translate from reduction to effectiveness and back again to support the effect.
            float effectiveness = 1 - reduction;
            effectiveness = Mth.clamp(EnchantmentHelper.modifyArmorEffectiveness(serverlevel, src.getWeaponItem(), target, src, effectiveness), 0.0F, 1.0F);
            reduction = 1 - effectiveness;
        }

        return amount * reduction;
    }

    /**
     * Computes the A value used in the Y = A / (A + X) formula used by {@link #getArmorDamageReduction(float, float)}.<br>
     * This value is a flat 10 for small damage values (< 20), and increases after that point.
     * <p>
     * This expression may be configured in the apothic_attributes.cfg file.
     * 
     * @param damage The amount of incoming damage.
     * @return The A value, for use in {@link #getArmorDamageReduction(float, float)}
     */
    public static float getAValue(float damage) {
        if (ALConfig.getAValueExpr().isPresent()) {
            return ALConfig.getAValueExpr().get().setVariable("damage", new BigDecimal(damage)).eval().floatValue();
        }
        return damage < 20 ? 10 : 10 + (damage - 20) / 2;
    }

    /**
     * Computes the damage reduction factor of the given armor level.<br>
     * Armor reduces a percentage of incoming damage equal to <code>A / (A + armor)</code>, where A varies based on the damage.
     * <p>
     * Armor Toughness no longer impacts this calculation.
     * <p>
     * The vanilla calculation is <code>DR = clamp(armor - damage / (2 + toughness / 4), armor / 5, 20) / 25</code>
     * <p>
     * For comparisons, see https://i.imgur.com/2OHQhgp.png
     *
     * @see #getAValue(float)
     * @see #getDamageAfterArmor(LivingEntity, DamageSource, float, float, float)
     */
    public static float getArmorDamageReduction(float damage, float armor, float toughness) {
        float a = getAValue(damage);
        if (ALConfig.getArmorExpr().isPresent()) {
            return ALConfig.getArmorExpr().get()
                .setVariable("a", new BigDecimal(a))
                .setVariable("damage", new BigDecimal(damage))
                .setVariable("armor", new BigDecimal(armor))
                .setVariable("toughness", new BigDecimal(toughness))
                .eval().floatValue();
        }
        return a / (a + armor);
    }

    /**
     * Computes the amount of armor bypass that the target's toughness value will resist.
     * <p>
     * A returned value of 1.0 will nullify enemy armor bypass.
     */
    public static float getBypassResistance(float damage, float armor, float toughness) {
        if (ALConfig.getToughnessExpr().isPresent()) {
            float bypassResist = ALConfig.getToughnessExpr().get()
                .setVariable("damage", new BigDecimal(damage))
                .setVariable("armor", new BigDecimal(armor))
                .setVariable("toughness", new BigDecimal(toughness))
                .eval().floatValue();
            return Math.clamp(bypassResist, 0, 1);
        }
        return Math.min(toughness * 0.02F, 0.6F);
    }
}
