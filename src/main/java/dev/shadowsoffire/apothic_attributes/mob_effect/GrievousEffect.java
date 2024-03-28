package dev.shadowsoffire.apothic_attributes.mob_effect;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.minecraft.ChatFormatting;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public class GrievousEffect extends MobEffect {

    public GrievousEffect() {
        super(MobEffectCategory.HARMFUL, ChatFormatting.DARK_RED.getColor());
        this.addAttributeModifier(ALObjects.Attributes.HEALING_RECEIVED.get(), "e04b0b87-5722-4841-bb87-98c6a4632c6f", -0.4, Operation.ADDITION);
    }

}
