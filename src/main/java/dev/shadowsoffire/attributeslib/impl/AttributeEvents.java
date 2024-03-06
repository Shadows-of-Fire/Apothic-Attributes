package dev.shadowsoffire.attributeslib.impl;

import java.util.Random;

import dev.shadowsoffire.attributeslib.AttributesLib;
import dev.shadowsoffire.attributeslib.api.ALObjects;
import dev.shadowsoffire.attributeslib.api.AttributeChangedValueEvent;
import dev.shadowsoffire.attributeslib.api.AttributeHelper;
import dev.shadowsoffire.attributeslib.api.IFormattableAttribute;
import dev.shadowsoffire.attributeslib.packet.CritParticleMessage;
import dev.shadowsoffire.attributeslib.util.AttributesUtil;
import dev.shadowsoffire.attributeslib.util.IFlying;
import dev.shadowsoffire.placebo.network.PacketDistro;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AttributeEvents {

    @SubscribeEvent
    public void fixChangedAttributes(PlayerLoggedInEvent e) {
        AttributeMap map = e.getEntity().getAttributes();
        map.getInstance(ForgeMod.STEP_HEIGHT_ADDITION.get()).setBaseValue(0.6);
    }

    private boolean canBenefitFromDrawSpeed(ItemStack stack) {
        return stack.getItem() instanceof ProjectileWeaponItem || stack.getItem() instanceof TridentItem;
    }

    /**
     * This event handler is the implementation for {@link ALObjects#DRAW_SPEED}.<br>
     * Each full point of draw speed provides an extra using tick per game tick.<br>
     * Each partial point of draw speed provides an extra using tick periodically.
     */
    @SubscribeEvent
    public void drawSpeed(LivingEntityUseItemEvent.Tick e) {
        if (e.getEntity() instanceof Player player) {
            double t = player.getAttribute(ALObjects.Attributes.DRAW_SPEED.get()).getValue() - 1;
            if (t == 0 || !this.canBenefitFromDrawSpeed(e.getItem())) return;

            // Handle negative draw speed.
            int offset = -1;
            if (t < 0) {
                offset = 1;
                t = -t;
            }

            while (t > 1) { // Every 100% triggers an immediate extra tick
                e.setDuration(e.getDuration() + offset);
                t--;
            }

            if (t > 0.5F) { // Special case 0.5F so that values in (0.5, 1) don't round to 1.
                if (e.getEntity().tickCount % 2 == 0) e.setDuration(e.getDuration() + offset);
                t -= 0.5F;
            }

            int mod = (int) Math.floor(1 / Math.min(1, t));
            if (e.getEntity().tickCount % mod == 0) e.setDuration(e.getDuration() + offset);
            t--;
        }
    }

    /**
     * This event handler manages the Life Steal and Overheal attributes.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void lifeStealOverheal(LivingHurtEvent e) {
        if (e.getSource().getDirectEntity() instanceof LivingEntity attacker && AttributesUtil.isPhysicalDamage(e.getSource())) {
            float lifesteal = (float) attacker.getAttributeValue(ALObjects.Attributes.LIFE_STEAL.get());
            float dmg = Math.min(e.getAmount(), e.getEntity().getHealth());
            if (lifesteal > 0.001) {
                attacker.heal(dmg * lifesteal);
            }
            float overheal = (float) attacker.getAttributeValue(ALObjects.Attributes.OVERHEAL.get());
            float maxOverheal = attacker.getMaxHealth() * 0.5F;
            if (overheal > 0 && attacker.getAbsorptionAmount() < maxOverheal) {
                attacker.setAbsorptionAmount(Math.min(maxOverheal, attacker.getAbsorptionAmount() + dmg * overheal));
            }
        }
    }

    /**
     * Recursion guard for {@link #meleeDamageAttributes(LivingAttackEvent)}.<br>
     * Doesn't need to be ThreadLocal as attack logic is main-thread only.
     */
    private static boolean noRecurse = false;

    /**
     * Applies the following melee damage attributes:<br>
     * <ul>
     * <li>{@link ALObjects#CURRENT_HP_DAMAGE}</li>
     * <li>{@link ALObjects#FIRE_DAMAGE}</li>
     * <li>{@link ALObjects#COLD_DAMAGE}</li>
     * </ul>
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void meleeDamageAttributes(LivingAttackEvent e) {
        if (e.getEntity().level().isClientSide || e.getEntity().isDeadOrDying()) return;
        if (noRecurse) return;
        noRecurse = true;
        if (e.getSource().getDirectEntity() instanceof LivingEntity attacker && AttributesUtil.isPhysicalDamage(e.getSource())) {
            float hpDmg = (float) attacker.getAttributeValue(ALObjects.Attributes.CURRENT_HP_DAMAGE.get());
            float fireDmg = (float) attacker.getAttributeValue(ALObjects.Attributes.FIRE_DAMAGE.get());
            float coldDmg = (float) attacker.getAttributeValue(ALObjects.Attributes.COLD_DAMAGE.get());
            LivingEntity target = e.getEntity();
            int time = target.invulnerableTime;
            target.invulnerableTime = 0;
            if (hpDmg > 0.001 && AttributesLib.localAtkStrength >= 0.85F) {
                target.hurt(src(ALObjects.DamageTypes.CURRENT_HP_DAMAGE, attacker), AttributesLib.localAtkStrength * hpDmg * target.getHealth());
            }
            target.invulnerableTime = 0;
            if (fireDmg > 0.001 && AttributesLib.localAtkStrength >= 0.55F) {
                target.hurt(src(ALObjects.DamageTypes.FIRE_DAMAGE, attacker), AttributesLib.localAtkStrength * fireDmg);
                target.setRemainingFireTicks(target.getRemainingFireTicks() + (int) (10 * fireDmg));
            }
            target.invulnerableTime = 0;
            if (coldDmg > 0.001 && AttributesLib.localAtkStrength >= 0.55F) {
                target.hurt(src(ALObjects.DamageTypes.COLD_DAMAGE, attacker), AttributesLib.localAtkStrength * coldDmg);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) (15 * coldDmg), Mth.floor(coldDmg / 5)));
            }
            target.invulnerableTime = time;
            if (target.isDeadOrDying()) {
                target.getPersistentData().putBoolean("apoth.killed_by_aux_dmg", true);
            }
        }
        noRecurse = false;
    }

    private static DamageSource src(ResourceKey<DamageType> type, LivingEntity entity) {
        return entity.level().damageSources().source(type, entity);
    }

    /**
     * Handles {@link ALObjects#CRIT_CHANCE} and {@link ALObjects#CRIT_DAMAGE}
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void apothCriticalStrike(LivingHurtEvent e) {
        LivingEntity attacker = e.getSource().getEntity() instanceof LivingEntity le ? le : null;
        if (attacker == null) return;

        double critChance = attacker.getAttributeValue(ALObjects.Attributes.CRIT_CHANCE.get());
        float critDmg = (float) attacker.getAttributeValue(ALObjects.Attributes.CRIT_DAMAGE.get());

        RandomSource rand = e.getEntity().getRandom();

        float critMult = 1.0F;

        // Roll for crits. Each overcrit reduces the effectiveness by 15%
        // We stop rolling when crit chance fails or the crit damage would reduce the total damage dealt.
        while (rand.nextFloat() <= critChance && critDmg > 1.0F) {
            critChance--;
            critMult *= critDmg;
            critDmg *= 0.85F;
        }

        e.setAmount(e.getAmount() * critMult);

        if (critMult > 1 && !attacker.level().isClientSide) {
            PacketDistro.sendToTracking(AttributesLib.CHANNEL, new CritParticleMessage(e.getEntity().getId()), (ServerLevel) attacker.level(), e.getEntity().blockPosition());
        }
    }

    /**
     * Handles {@link ALObjects#CRIT_DAMAGE}'s interactions with vanilla critical strikes.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void vanillaCritDmg(CriticalHitEvent e) {
        float critDmg = (float) e.getEntity().getAttributeValue(ALObjects.Attributes.CRIT_DAMAGE.get());
        if (e.isVanillaCritical()) {
            e.setDamageModifier(Math.max(e.getDamageModifier(), critDmg));
        }
    }

    /**
     * Handles {@link ALObjects#MINING_SPEED}
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void breakSpd(BreakSpeed e) {
        e.setNewSpeed(e.getNewSpeed() * (float) e.getEntity().getAttributeValue(ALObjects.Attributes.MINING_SPEED.get()));
    }

    /**
     * This event, and {@linkplain #mobXp(LivingExperienceDropEvent) the event below} handle {@link ALObjects#EXPERIENCE_GAINED}
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void blockBreak(BreakEvent e) {
        double xpMult = e.getPlayer().getAttributeValue(ALObjects.Attributes.EXPERIENCE_GAINED.get());
        e.setExpToDrop((int) (e.getExpToDrop() * xpMult));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void mobXp(LivingExperienceDropEvent e) {
        Player player = e.getAttackingPlayer();
        if (player == null) return;
        double xpMult = e.getAttackingPlayer().getAttributeValue(ALObjects.Attributes.EXPERIENCE_GAINED.get());
        e.setDroppedExperience((int) (e.getDroppedExperience() * xpMult));
    }

    /**
     * Handles {@link ALObjects#HEALING_RECEIVED}
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void heal(LivingHealEvent e) {
        float factor = (float) e.getEntity().getAttributeValue(ALObjects.Attributes.HEALING_RECEIVED.get());
        e.setAmount(e.getAmount() * factor);
        if (e.getAmount() <= 0) e.setCanceled(true);
    }

    /**
     * Handles {@link ALObjects#ARROW_DAMAGE} and {@link ALObjects#ARROW_VELOCITY}
     */
    @SubscribeEvent
    public void arrow(EntityJoinLevelEvent e) {
        if (e.getEntity() instanceof AbstractArrow arrow) {
            if (arrow.level().isClientSide || arrow.getPersistentData().getBoolean("attributeslib.arrow.done")) return;
            if (arrow.getOwner() instanceof LivingEntity le) {
                arrow.setBaseDamage(arrow.getBaseDamage() * le.getAttributeValue(ALObjects.Attributes.ARROW_DAMAGE.get()));
                arrow.setDeltaMovement(arrow.getDeltaMovement().scale(le.getAttributeValue(ALObjects.Attributes.ARROW_VELOCITY.get())));
            }
            arrow.getPersistentData().putBoolean("attributeslib.arrow.done", true);
        }
    }

    /**
     * Copied from {@link MeleeAttackGoal#getAttackReachSqr}
     */
    private static double getAttackReachSqr(Entity attacker, LivingEntity pAttackTarget) {
        return attacker.getBbWidth() * 2.0F * attacker.getBbWidth() * 2.0F + pAttackTarget.getBbWidth();
    }

    /**
     * Random used for dodge calculations.<br>
     * This random is seeded with the target entity's tick count before use.
     */
    private static Random dodgeRand = new Random();

    /**
     * Handles {@link ALObjects#DODGE_CHANCE} for melee attacks.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void dodge(LivingAttackEvent e) {
        LivingEntity target = e.getEntity();
        if (target.level().isClientSide) return;
        Entity attacker = e.getSource().getDirectEntity();
        if (attacker instanceof LivingEntity) {
            double dodgeChance = target.getAttributeValue(ALObjects.Attributes.DODGE_CHANCE.get());
            double atkRangeSqr = attacker instanceof Player p ? p.getEntityReach() * p.getEntityReach() : getAttackReachSqr(attacker, target);
            dodgeRand.setSeed(target.tickCount);
            if (attacker.distanceToSqr(target) <= atkRangeSqr && dodgeRand.nextFloat() <= dodgeChance) {
                this.onDodge(target);
                e.setCanceled(true);
            }
        }
    }

    /**
     * Handles {@link ALObjects#DODGE_CHANCE} for projectiles.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void dodge(ProjectileImpactEvent e) {
        Entity target = e.getRayTraceResult() instanceof EntityHitResult entRes ? entRes.getEntity() : null;
        if (target instanceof LivingEntity lvTarget) {
            double dodgeChance = lvTarget.getAttributeValue(ALObjects.Attributes.DODGE_CHANCE.get());
            // We can skip the distance check for projectiles, as "Projectile Impact" means the projectile is on the target.
            dodgeRand.setSeed(target.tickCount);
            if (dodgeRand.nextFloat() <= dodgeChance) {
                this.onDodge(lvTarget);
                e.setCanceled(true);
            }
        }
    }

    private void onDodge(LivingEntity target) {
        target.level().playSound(null, target, ALObjects.Sounds.DODGE.get(), SoundSource.NEUTRAL, 1, 0.7F + target.getRandom().nextFloat() * 0.3F);
        if (target.level() instanceof ServerLevel sl) {
            double height = target.getBbHeight();
            double width = target.getBbWidth();
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, target.getX() - width / 4, target.getY(), target.getZ() - width / 4, 6, -width / 4, height / 8, -width / 4, 0);
        }
    }

    /**
     * Fix for https://github.com/MinecraftForge/MinecraftForge/issues/9370
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void fixMCF9370(ProjectileImpactEvent e) {
        if (e.isCanceled()) {
            Entity target = e.getRayTraceResult() instanceof EntityHitResult entRes ? entRes.getEntity() : null;
            Projectile proj = e.getProjectile();
            if (target != null && proj instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) {
                if (arrow.piercingIgnoreEntityIds == null) {
                    arrow.piercingIgnoreEntityIds = new IntOpenHashSet(arrow.getPierceLevel());
                }
                arrow.piercingIgnoreEntityIds.add(target.getId());
            }
        }
    }

    /**
     * Adds a fake modifier to show Attack Range to weapons with Attack Damage.
     */
    @SubscribeEvent
    public void affixModifiers(ItemAttributeModifierEvent e) {
        boolean hasBaseAD = e.getModifiers().get(Attributes.ATTACK_DAMAGE).stream().filter(m -> ((IFormattableAttribute) Attributes.ATTACK_DAMAGE).getBaseUUID().equals(m.getId())).findAny().isPresent();
        if (hasBaseAD) {
            boolean hasBaseAR = e.getModifiers().get(ForgeMod.ENTITY_REACH.get()).stream().filter(m -> ((IFormattableAttribute) ForgeMod.ENTITY_REACH.get()).getBaseUUID().equals(m.getId())).findAny().isPresent();
            if (!hasBaseAR) {
                e.addModifier(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(AttributeHelper.BASE_ENTITY_REACH, () -> "attributeslib:fake_base_range", 0, Operation.ADDITION));
            }
        }
        if (e.getSlotType() == EquipmentSlot.CHEST && e.getItemStack().getItem() instanceof ElytraItem && !e.getModifiers().containsKey(ALObjects.Attributes.ELYTRA_FLIGHT.get())) {
            e.addModifier(ALObjects.Attributes.ELYTRA_FLIGHT.get(), new AttributeModifier(AttributeHelper.ELYTRA_FLIGHT_UUID, () -> "attributeslib:elytra_item_flight", 1, Operation.ADDITION));
        }
    }

    @SubscribeEvent
    public void trackCooldown(AttackEntityEvent e) {
        Player p = e.getEntity();
        AttributesLib.localAtkStrength = p.getAttackStrengthScale(0.5F);
    }

    @SubscribeEvent
    public void valueChanged(AttributeChangedValueEvent e) {
        // AttributesLib.LOGGER.info("Attribute {} changed value from {} to {}!", e.getAttributeInstance().getAttribute().getDescriptionId(), e.getOldValue(),
        // e.getNewValue());
        if (e.getAttributeInstance().getAttribute() == ALObjects.Attributes.CREATIVE_FLIGHT.get() && e.getEntity() instanceof ServerPlayer player) {

            boolean changed = false;

            if (((IFlying) player).getAndDestroyFlyingCache()) {
                player.getAbilities().flying = true;
                changed = true;
            }

            if (e.getNewValue() > 0) {
                player.getAbilities().mayfly = true;
                changed = true;
            }
            else if (e.getOldValue() > 0 && e.getNewValue() <= 0) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                changed = true;
            }

            if (changed) player.onUpdateAbilities();
        }
    }

    public static void applyCreativeFlightModifier(Player player, GameType newType) {
        AttributeInstance inst = player.getAttribute(ALObjects.Attributes.CREATIVE_FLIGHT.get());
        if (newType == GameType.CREATIVE || newType == GameType.SPECTATOR) {
            if (inst.getModifier(AttributeHelper.CREATIVE_FLIGHT_UUID) == null) {
                inst.addTransientModifier(new AttributeModifier(AttributeHelper.CREATIVE_FLIGHT_UUID, () -> "attributeslib:creative_flight", 1, Operation.ADDITION));
            }
        }
        else {
            inst.removeModifier(AttributeHelper.CREATIVE_FLIGHT_UUID);
        }
    }
}
