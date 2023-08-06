package dev.shadowsoffire.attributeslib.util;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

public class AttributesUtil {

    public static boolean isPhysicalDamage(DamageSource src) {
        return !src.is(DamageTypes.MAGIC) && !src.is(DamageTypes.INDIRECT_MAGIC) && !src.is(DamageTypeTags.IS_FIRE) && !src.is(DamageTypeTags.IS_EXPLOSION);
    }

}
