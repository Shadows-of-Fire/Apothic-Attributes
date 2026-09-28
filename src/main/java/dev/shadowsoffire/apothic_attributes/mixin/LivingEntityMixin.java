package dev.shadowsoffire.apothic_attributes.mixin;

import java.util.Stack;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import dev.shadowsoffire.apothic_attributes.api.ALCombatRules;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.util.AttributesUtil;
import dev.shadowsoffire.apothic_attributes.util.LEInvoker;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

@Mixin(value = LivingEntity.class, remap = false)
public abstract class LivingEntityMixin extends Entity implements LEInvoker {

    @Shadow
    @Nullable
    protected Stack<DamageContainer> damageContainers;

    public LivingEntityMixin(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    /**
     * @author Shadows
     * @reason Injection of the Sundering potion effect, which is applied during resistance calculations.
     * @param value  Damage modifier percentage after resistance has been applied [1.0, -inf]
     * @param max    Zero
     * @param source The damage source
     * @param damage The initial damage amount
     */
    @Redirect(at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"), method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F", require = 1)
    public float apoth_sunderingApplyEffect(float value, float max, DamageSource source, float damage) {
        if (this.hasEffect(ALObjects.MobEffects.SUNDERING) && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            int level = this.getEffect(ALObjects.MobEffects.SUNDERING).getAmplifier() + 1;
            value += damage * level * 0.2F;
        }

        // The return value of getDamageAfterMagicAbsorb is ignored, so we have to manipulate the damage container directly.
        float dmg = this.damageContainers.peek().getNewDamage();
        if (value >= dmg) {
            this.damageContainers.peek().setNewDamage(value);
        }

        return Math.max(value, max);
    }

    /**
     * @author Shadows
     * @reason Used to enter an if-condition so the above mixin always triggers.
     */
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z"), method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F", require = 1)
    public boolean apoth_sunderingHasEffect(LivingEntity instance, Holder<MobEffect> effect) {
        return true;
    }

    /**
     * @author Shadows
     * @reason Used to prevent an NPE since we're faking true on hasEffect
     */
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;getAmplifier()I"), method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F")
    public int apoth_sunderingGetAmplifier(@Nullable MobEffectInstance inst) {
        return inst == null ? -1 : inst.getAmplifier();
    }

    @Shadow
    public abstract boolean hasEffect(Holder<MobEffect> ef);

    @Shadow
    public abstract MobEffectInstance getEffect(Holder<MobEffect> ef);

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterMagicAbsorb(FF)F"), method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F", require = 1)
    public float apoth_applyProtPen(float amount, float protPoints, DamageSource src, float amt2) {
        return ALCombatRules.getDamageAfterProtection((LivingEntity) (Object) this, src, amount, protPoints);
    }

    @Shadow
    protected abstract void internalSetAbsorptionAmount(float absorptionAmount);

    @Override
    public void apoth_setInternalAbsorption(float absorptionAmount) {
        this.internalSetAbsorptionAmount(absorptionAmount);
    }

    /**
     * @author Shadows
     * @reason Adds the absorption granted by {@link ALObjects.Attributes#OVERHEAL} to the effective max absorption, so that the clamps in
     *         {@link LivingEntity#setAbsorptionAmount} and {@code LivingEntity#onAttributeUpdated} only strip non-Overheal absorption.
     *         <p>
     *         On the client, we never report a max below the current absorption. The client refreshes dirty attributes on its own, which
     *         would otherwise trim the local player's (server-synced) absorption. The server is authoritative for the real value.
     */
    @ModifyReturnValue(method = "getMaxAbsorption()F", at = @At("RETURN"), require = 1)
    private float apoth_overhealMaxAbsorption(float original) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) {
            return Math.max(original, self.getAbsorptionAmount());
        }
        return original + AttributesUtil.getOverhealAbsorption(self);
    }

    /**
     * @author Shadows
     * @reason Keeps the tracked Overheal absorption from exceeding the real absorption after any clamped write (damage, effect expiry, max
     *         absorption changes). Non-Overheal absorption is consumed first; the Overheal portion only shrinks once the total falls below it.
     */
    @Inject(method = "setAbsorptionAmount(F)V", at = @At("TAIL"), require = 1)
    private void apoth_trimOverhealAbsorption(float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide && self.hasData(ALObjects.Attachments.OVERHEAL_ABSORPTION)) {
            float tracked = self.getData(ALObjects.Attachments.OVERHEAL_ABSORPTION);
            float current = self.getAbsorptionAmount();
            if (tracked > current) {
                AttributesUtil.setOverhealAbsorption(self, current);
            }
        }
    }

}
