package dev.shadowsoffire.apothic_attributes.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.shadowsoffire.apothic_attributes.api.ALCombatRules;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.api.AttributeChangedValueEvent;
import dev.shadowsoffire.apothic_attributes.util.IAttributeManager;
import dev.shadowsoffire.apothic_attributes.util.IEntityOwned;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.level.Level;

@Mixin(value = LivingEntity.class, remap = false)
public abstract class LivingEntityMixin extends Entity {

    @Shadow
    private AttributeMap attributes;

    public LivingEntityMixin(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    /**
     * Constructor mixin to call {@link IEntityOwned#setOwner(LivingEntity)} on {@link #attributes}.<br>
     * Supports {@link AttributeChangedValueEvent}.
     */
    @Inject(at = @At(value = "TAIL"), method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", require = 1, remap = false)
    public void apoth_ownedAttrMap(EntityType<?> type, Level level, CallbackInfo ci) {
        ((IEntityOwned) attributes).setOwner((LivingEntity) (Object) this);
    }

    /**
     * @author Shadows
     * @reason Injection of the Sundering potion effect, which is applied during resistance calculations.
     * @param value  Damage modifier percentage after resistance has been applied [1.0, -inf]
     * @param max    Zero
     * @param source The damage source
     * @param damage The initial damage amount
     */
    @Redirect(at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"), method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F")
    public float apoth_sunderingApplyEffect(float value, float max, DamageSource source, float damage) {
        if (this.hasEffect(ALObjects.MobEffects.SUNDERING.get()) && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            int level = this.getEffect(ALObjects.MobEffects.SUNDERING.get()).getAmplifier() + 1;
            value += damage * level * 0.2F;
        }
        return Math.max(value, max);
    }

    /**
     * @author Shadows
     * @reason Used to enter an if-condition so the above mixin always triggers.
     */
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z"), method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F")
    public boolean apoth_sunderingHasEffect(LivingEntity ths, MobEffect effect) {
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
    public abstract boolean hasEffect(MobEffect ef);

    @Shadow
    public abstract MobEffectInstance getEffect(MobEffect ef);

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(FFF)F"), method = "getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F", require = 1)
    public float apoth_applyArmorPen(float amount, float armor, float toughness, DamageSource src, float amt2) {
        return ALCombatRules.getDamageAfterArmor((LivingEntity) (Object) this, src, amount, armor, toughness);
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterMagicAbsorb(FF)F"), method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F", require = 1)
    public float apoth_applyProtPen(float amount, float protPoints, DamageSource src, float amt2) {
        return ALCombatRules.getDamageAfterProtection((LivingEntity) (Object) this, src, amount, protPoints);
    }

    /**
     * @author ChampionAsh5357
     * @reason Lock attribute updates for event until after new modifiers are added
     */
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffect;removeAttributeModifiers(Lnet/minecraft/world/entity/ai/attributes/AttributeMap;)V"), method = "onEffectUpdated", require = 1)
    public void apoth_onEffectUpdateRemoveAttribute(MobEffectInstance pEffectInstance, boolean pForced, Entity pEntity, CallbackInfo ci) {
        ((IAttributeManager) attributes).setAttributesUpdating(true);
    }

    /**
     * @author ChampionAsh5357
     * @reason Unlock attribute updates for event until after new modifiers are added
     */
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffect;addAttributeModifiers(Lnet/minecraft/world/entity/ai/attributes/AttributeMap;I)V", shift = At.Shift.AFTER), method = "onEffectUpdated", require = 1)
    public void apoth_onEffectUpdateAddAttribute(MobEffectInstance pEffectInstance, boolean pForced, Entity pEntity, CallbackInfo ci) {
        ((IAttributeManager) attributes).setAttributesUpdating(false);
    }
}
