package dev.shadowsoffire.apothic_attributes.mob_effect;

import net.minecraft.ChatFormatting;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.neoforged.neoforge.common.NeoForgeMod;

public class FlyingEffect extends MobEffect {

    public FlyingEffect() {
        super(MobEffectCategory.BENEFICIAL, ChatFormatting.RED.getColor());
        this.addAttributeModifier(NeoForgeMod.CREATIVE_FLIGHT.value(), "ea575584-4ff4-4c96-a1a3-f2024d9fd898", 1, Operation.ADDITION);
    }

    @Override
    public void addAttributeModifiers(AttributeMap pAttributeMap, int pAmplifier) {
        super.addAttributeModifiers(pAttributeMap, 0);
    }

}
