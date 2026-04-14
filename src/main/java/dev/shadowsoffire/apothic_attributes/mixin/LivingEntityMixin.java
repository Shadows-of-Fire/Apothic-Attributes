package dev.shadowsoffire.apothic_attributes.mixin;

import java.util.List;
import java.util.Stack;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;

import dev.shadowsoffire.apothic_attributes.api.ALCombatRules;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.util.LEInvoker;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
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
     * Replaces the 1.21.1-era {@code IItemExtension#canElytraFly} mixin. In 26.1, gliding is gated by {@link LivingEntity#canGlide()}
     * rather than per-item extension hooks. When {@link ALObjects.Attributes#ELYTRA_FLIGHT} is positive and the vanilla preconditions
     * (not on ground, not a passenger, no Levitation) are met, force-return {@code true} so the entity can glide without needing a
     * GLIDER data component on equipment.
     */
    @Inject(method = "canGlide()Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void apoth_canGlideFromAttribute(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getAttributeValue(ALObjects.Attributes.ELYTRA_FLIGHT) > 0
            && !self.onGround()
            && !self.isPassenger()
            && !self.hasEffect(MobEffects.LEVITATION)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Makes {@link LivingEntity#canGlideUsing(ItemStack, EquipmentSlot)} also return {@code true} for any item that
     * provides a positive {@link ALObjects.Attributes#ELYTRA_FLIGHT} modifier on a slot group containing {@code slot}.
     * This lets non-GLIDER items (wings, charms, capes) enable gliding through the attribute instead of the data
     * component, and keeps {@link LivingEntity#updateFallFlying} from picking an unrelated slot for durability damage.
     */
    @Inject(method = "canGlideUsing(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private static void apoth_canGlideUsingFromAttribute(ItemStack stack, EquipmentSlot slot, CallbackInfoReturnable<Boolean> cir) {
        ItemAttributeModifiers mods = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (mods == null) return;
        for (ItemAttributeModifiers.Entry entry : mods.modifiers()) {
            if (entry.attribute().equals(ALObjects.Attributes.ELYTRA_FLIGHT)
                && entry.slot().test(slot)
                && entry.modifier().amount() > 0) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    /**
     * Prevents {@link LivingEntity#updateFallFlying} from crashing in {@code Util.getRandom} when the ELYTRA_FLIGHT
     * attribute is provided by something other than an equipment slot (e.g. a mob effect, an entity-wide attribute
     * supplier), in which case {@code slotsWithGliders} is empty but the outer guard {@link LivingEntity#canGlide}
     * has already returned {@code true} via {@link #apoth_canGlideFromAttribute}.
     * <p>
     * Injects right before the {@code Util.getRandom} call and cancels the rest of the method when the list is empty,
     * skipping both the durability damage and the outer {@code gameEvent(ELYTRA_GLIDE)}.
     */
    @Inject(method = "updateFallFlying()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getRandom(Ljava/util/List;Lnet/minecraft/util/RandomSource;)Ljava/lang/Object;"), cancellable = true, require = 1)
    private void apoth_skipEmptyGliderDamage(CallbackInfo ci, @Local List<EquipmentSlot> slotsWithGliders) {
        if (slotsWithGliders.isEmpty()) {
            ci.cancel();
            this.gameEvent(GameEvent.ELYTRA_GLIDE); // Since we cancel the remainder of the method, we need to make sure this still executes for consistency.
        }
    }

}
