package dev.shadowsoffire.apothic_attributes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

@Mixin(value = NearestAttackableTargetGoal.class, remap = false)
public abstract class NearestAttackableTargetGoalMixin extends TargetGoal {

    public NearestAttackableTargetGoalMixin(Mob pMob, boolean pMustSee) {
        super(pMob, pMustSee);
    }

    @Shadow
    TargetingConditions targetConditions;

    /**
     * Normally, the follow range is encoded into the TargetingConditions at construction time.<br>
     * This means that modifications to it (via attribute modifiers) won't actually change anything.<br>
     * This mixin makes it update before use, so the real value is used.
     * <p>
     * Technically {@link TargetGoal#canContinueToUse()} uses the real value, which should kick it back after a delay.
     */
    @Inject(method = "findTarget()V", at = @At("HEAD"))
    private void apoth_updateFollowRange(CallbackInfo ci) {
        this.targetConditions.range(this.getFollowDistance());
    }

}
