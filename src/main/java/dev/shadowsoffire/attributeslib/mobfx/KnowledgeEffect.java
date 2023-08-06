package dev.shadowsoffire.attributeslib.mobfx;

import dev.shadowsoffire.attributeslib.AttributesLib;
import dev.shadowsoffire.attributeslib.api.ALObjects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class KnowledgeEffect extends MobEffect {

    public KnowledgeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF4EE42);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void xp(LivingExperienceDropEvent e) {
        if (e.getAttackingPlayer() != null && e.getAttackingPlayer().hasEffect(ALObjects.MobEffects.KNOWLEDGE.get())) {
            int level = e.getAttackingPlayer().getEffect(ALObjects.MobEffects.KNOWLEDGE.get()).getAmplifier() + 1;
            int curXp = e.getDroppedExperience();
            int newXp = curXp + e.getOriginalExperience() * level * AttributesLib.knowledgeMult;
            e.setDroppedExperience(newXp);
        }
    }

}
