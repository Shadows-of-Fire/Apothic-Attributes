package dev.shadowsoffire.apothic_attributes.util;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.Tags;

public class AttributesUtil {

    public static boolean isPhysicalDamage(DamageSource src) {
        return src.is(Tags.DamageTypes.IS_PHYSICAL) && !src.is(ALObjects.Tags.IS_NON_PHYSICAL);
    }

    /**
     * Computes the maximum total absorption that {@link ALObjects.Attributes#OVERHEAL} will fill up to, which is 50% of the entity's max health.
     */
    public static float getOverhealCap(LivingEntity entity) {
        return entity.getMaxHealth() * 0.5F;
    }

    /**
     * Returns the portion of the entity's current absorption that was granted by {@link ALObjects.Attributes#OVERHEAL}.
     * <p>
     * The tracked value is clamped to the current absorption amount, so this never reports more absorption than the entity actually has.
     */
    public static float getOverhealAbsorption(LivingEntity entity) {
        if (!entity.hasData(ALObjects.Attachments.OVERHEAL_ABSORPTION)) return 0;
        return Math.min(entity.getData(ALObjects.Attachments.OVERHEAL_ABSORPTION), entity.getAbsorptionAmount());
    }

    /**
     * Records the portion of the entity's current absorption that was granted by {@link ALObjects.Attributes#OVERHEAL}.
     */
    public static void setOverhealAbsorption(LivingEntity entity, float amount) {
        entity.setData(ALObjects.Attachments.OVERHEAL_ABSORPTION, Math.max(0, amount));
    }

}
