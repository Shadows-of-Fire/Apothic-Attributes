package dev.shadowsoffire.apothic_attributes.mob_effect;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public class KnowledgeEffect extends MobEffect {

    public KnowledgeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF4EE42);
        this.addAttributeModifier(ALObjects.Attributes.EXPERIENCE_GAINED.get(), "55688e2f-7db8-4d0b-bc90-eff194546c04", 4, Operation.MULTIPLY_TOTAL);
    }

}
