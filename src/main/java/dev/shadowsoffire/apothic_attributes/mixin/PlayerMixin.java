package dev.shadowsoffire.apothic_attributes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@Mixin(value = Player.class, remap = false)
public class PlayerMixin {

    /**
     * Wraps {@link Entity#hurtOrSimulate(DamageSource, float)} in {@link Player#attack(Entity)} so aux damage kills still flow through follow-up logic.
     * <p>
     * This is necessary since if we kill the target in aux damage, we have to cancel the {@link LivingIncomingDamageEvent} to prevent
     * {@link LivingEntity#die(DamageSource)} from being called twice. Under normal conditions, canceling the event will cause {@code hurtOrSimulate}
     * to return false, which would prevent follow-up effects (like sweep attacks).
     */
    @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), method = "attack(Lnet/minecraft/world/entity/Entity;)V", require = 1)
    private static boolean apoth_handleKilledByAuxDmg(Entity target, DamageSource src, float dmg, Operation<Boolean> wrapped) {
        boolean res = wrapped.call(target, src, dmg);
        return res || target.getPersistentData().getBooleanOr("apoth.killed_by_aux_dmg", false);
    }

    /**
     * Raises a flag on the target entity while vanilla's sweep-attack block is calling {@link LivingEntity#hurtServer(ServerLevel, DamageSource, float)}
     * so we can modify aux damage based on the value of {@link Attributes#SWEEPING_DAMAGE_RATIO}.
     */
    @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"), method = "doSweepAttack(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;FLnet/minecraft/world/phys/AABB;)V", require = 1)
    private boolean apoth_markSweepAttacks(LivingEntity target, ServerLevel level, DamageSource src, float dmg, Operation<Boolean> wrapped) {
        target.getPersistentData().putBoolean("apoth.hit_by_sweep_attack", true);
        boolean res = wrapped.call(target, level, src, dmg);
        target.getPersistentData().remove("apoth.hit_by_sweep_attack");
        return res;
    }

    /**
     * "Fixes" MC-268917 by clamping the fall distance while sneaking to the base step height value of 0.6F instead of using the total.
     * <p>
     * This ensures that, while sneaking, the player may never move down further than 0.6 blocks, instead of being able to fall down an amount equal to their step
     * height.
     * 
     * @apiNote "Fixes" is in quotes because Mojang has closed the bug as "Working as Intended", despite years of player feedback indicating this is a poor choice.
     */
    @WrapOperation(method = "maybeBackOffFromEdge", at = @At(value = "INVOKE", target = "maxUpStep()F"))
    private float apoth_dontFallOffACliff(Player player, Operation<Float> original) {
        return Math.min(original.call(player), 0.6F);
    }
}
