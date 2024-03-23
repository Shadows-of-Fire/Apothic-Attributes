package dev.shadowsoffire.apothic_attributes.mobfx;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Applied via {@link LivingEntityMixin}
 */
public class SunderingEffect extends MobEffect {

    public SunderingEffect() {
        super(MobEffectCategory.HARMFUL, 0x989898);
    }

}
