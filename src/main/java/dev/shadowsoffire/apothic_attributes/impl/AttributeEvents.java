package dev.shadowsoffire.apothic_attributes.impl;

import java.util.Random;

import dev.shadowsoffire.apothic_attributes.ALConfig;
import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.api.ALObjects.Attachments;
import dev.shadowsoffire.apothic_attributes.api.AttributeHelper;
import dev.shadowsoffire.apothic_attributes.commands.BonusModifierCommand;
import dev.shadowsoffire.apothic_attributes.event.ApotheosisCommandEvent;
import dev.shadowsoffire.apothic_attributes.modifiers.EquipmentSlotCompat;
import dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiers;
import dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiersEvent;
import dev.shadowsoffire.apothic_attributes.payload.ConfigPayload;
import dev.shadowsoffire.apothic_attributes.payload.CritParticlePayload;
import dev.shadowsoffire.apothic_attributes.util.AttributesUtil;
import dev.shadowsoffire.apothic_attributes.util.AuxDmgTracker;
import dev.shadowsoffire.apothic_attributes.util.LEInvoker;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.AttributeUtil;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class AttributeEvents {

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
            double t = player.getAttribute(ALObjects.Attributes.DRAW_SPEED).getValue() - 1;
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void recordPreDamageHealth(LivingDamageEvent.Pre e) {
        e.getEntity().setData(Attachments.PRE_DAMAGE_HEALTH, e.getEntity().getHealth());
    }

    /**
     * This event handler manages the Life Steal and Overheal attributes.
     */
    @SubscribeEvent
    public void lifeStealOverheal(LivingDamageEvent.Post e) {
        if (e.getSource().getDirectEntity() instanceof LivingEntity attacker && AttributesUtil.isPhysicalDamage(e.getSource())) {
            float oldEntityHealth = e.getEntity().getData(Attachments.PRE_DAMAGE_HEALTH);
            float lifesteal = (float) attacker.getAttributeValue(ALObjects.Attributes.LIFE_STEAL);
            float dmg = Math.min(e.getNewDamage(), oldEntityHealth);
            if (lifesteal > 0.001) {
                attacker.heal(dmg * lifesteal);
            }
            float overheal = (float) attacker.getAttributeValue(ALObjects.Attributes.OVERHEAL);
            float maxOverheal = attacker.getMaxHealth() * 0.5F;
            if (overheal > 0 && attacker.getAbsorptionAmount() < maxOverheal) {
                // Overheal needs to bypass the max absorption attribute, which is used for natural absorption regeneration, but also clamps the total number of abs hearts.
                ((LEInvoker) attacker).apoth_setInternalAbsorption(Math.min(maxOverheal, attacker.getAbsorptionAmount() + dmg * overheal));
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
    public void meleeDamageAttributes(LivingIncomingDamageEvent e) {
        if (e.getEntity().level().isClientSide() || e.getEntity().isDeadOrDying()) return;
        if (noRecurse) return;
        noRecurse = true;
        if (e.getSource().getDirectEntity() instanceof LivingEntity attacker && AttributesUtil.isPhysicalDamage(e.getSource())) {
            LivingEntity target = e.getEntity();

            AuxDmgTracker.executeWith(target, tracker -> {
                float hpDmg = (float) attacker.getAttributeValue(ALObjects.Attributes.CURRENT_HP_DAMAGE) * target.getHealth();
                tracker.attackWith(attacker, target, ALObjects.DamageTypes.CURRENT_HP_DAMAGE, hpDmg, null);

                tracker.attackWith(attacker, target, ALObjects.DamageTypes.FIRE_DAMAGE, ALObjects.Attributes.FIRE_DAMAGE, AttributeEvents::applyPostFireDamage);

                tracker.attackWith(attacker, target, ALObjects.DamageTypes.COLD_DAMAGE, ALObjects.Attributes.COLD_DAMAGE, AttributeEvents::applyPostColdDamage);
            });

            if (target.isDeadOrDying()) {
                // Communicates back to PlayerMixin that the return value of hurt() should be true, so post-attack effects (like sweep attacks) are applied.
                target.getPersistentData().putBoolean("apoth.killed_by_aux_dmg", true);
                // We need to cancel the event if the target is already dead, otherwise the original attack will go through and call die() twice.
                e.setCanceled(true);
            }
        }
        noRecurse = false;
    }

    private static void applyPostFireDamage(LivingEntity attacker, LivingEntity target, DamageSource src, float dmg, float delta) {
        target.setRemainingFireTicks(target.getRemainingFireTicks() + (int) (10 * dmg));
    }

    private static void applyPostColdDamage(LivingEntity attacker, LivingEntity target, DamageSource src, float dmg, float delta) {
        int duration = (int) Math.min(150, 15 * dmg);
        int amp = Math.max(0, Mth.log2(Math.round(dmg / 5)));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, amp));
    }

    /**
     * Handles {@link ALObjects#CRIT_CHANCE} and {@link ALObjects#CRIT_DAMAGE}
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void apothCriticalStrike(LivingIncomingDamageEvent e) {
        LivingEntity attacker = e.getSource().getEntity() instanceof LivingEntity le ? le : null;
        if (attacker == null || e.getSource().is(ALObjects.Tags.CANNOT_CRITICALLY_STRIKE)) return;

        double critChance = attacker.getAttributeValue(ALObjects.Attributes.CRIT_CHANCE);
        float critDmg = (float) attacker.getAttributeValue(ALObjects.Attributes.CRIT_DAMAGE);

        RandomSource rand = e.getEntity().getRandom();

        float damage = e.getAmount();

        // Roll for crits. Each overcrit reduces the effectiveness by 15%
        // We stop rolling when crit chance fails or the crit damage would reduce the total damage dealt.
        // Multicrits are additive with previous crits.
        while (rand.nextFloat() <= critChance && critDmg > 1.0F) {
            critChance--;
            damage += e.getAmount() * (critDmg - 1);
            critDmg *= 0.85F;
        }

        if (damage > e.getAmount() && !attacker.level().isClientSide()) {
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) attacker.level(), e.getEntity().chunkPosition(), new CritParticlePayload(e.getEntity().getId()));
        }

        e.setAmount(damage);
    }

    /**
     * Handles {@link ALObjects#CRIT_DAMAGE}'s interactions with vanilla critical strikes.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void vanillaCritDmg(CriticalHitEvent e) {
        float critDmg = (float) e.getEntity().getAttributeValue(ALObjects.Attributes.CRIT_DAMAGE);
        if (e.isVanillaCritical()) {
            e.setDamageMultiplier(Math.max(e.getDamageMultiplier(), critDmg));
        }
    }

    /**
     * Handles {@link ALObjects#MINING_SPEED}
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void breakSpd(BreakSpeed e) {
        e.setNewSpeed(e.getNewSpeed() * (float) e.getEntity().getAttributeValue(ALObjects.Attributes.MINING_SPEED));
    }

    /**
     * This event, and {@linkplain #mobXp(LivingExperienceDropEvent) the event below} handle {@link ALObjects#EXPERIENCE_GAINED}
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void blockBreak(BlockDropsEvent e) {
        if (e.getBreaker() instanceof LivingEntity living) {
            double xpMult = living.getAttributeValue(ALObjects.Attributes.EXPERIENCE_GAINED);
            e.setDroppedExperience((int) (e.getDroppedExperience() * xpMult));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void mobXp(LivingExperienceDropEvent e) {
        Player player = e.getAttackingPlayer();
        if (player == null) return;
        double xpMult = e.getAttackingPlayer().getAttributeValue(ALObjects.Attributes.EXPERIENCE_GAINED);
        e.setDroppedExperience((int) (e.getDroppedExperience() * xpMult));
    }

    /**
     * Handles {@link ALObjects#HEALING_RECEIVED}
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void heal(LivingHealEvent e) {
        float factor = (float) e.getEntity().getAttributeValue(ALObjects.Attributes.HEALING_RECEIVED);
        e.setAmount(e.getAmount() * factor);
        if (e.getAmount() <= 0) e.setCanceled(true);
    }

    /**
     * Handles {@link ALObjects#ARROW_DAMAGE} and {@link ALObjects#ARROW_VELOCITY}
     */
    @SubscribeEvent
    public void arrow(EntityJoinLevelEvent e) {
        if (e.getEntity() instanceof AbstractArrow arrow) {
            if (arrow.level().isClientSide() || arrow.getPersistentData().getBooleanOr("apothic_attributes.arrow.done", false)) return;
            if (arrow.getOwner() instanceof LivingEntity le) {
                arrow.setBaseDamage(arrow.baseDamage * le.getAttributeValue(ALObjects.Attributes.ARROW_DAMAGE));
                arrow.setDeltaMovement(arrow.getDeltaMovement().scale(le.getAttributeValue(ALObjects.Attributes.ARROW_VELOCITY)));
            }
            arrow.getPersistentData().putBoolean("apothic_attributes.arrow.done", true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void projDmg(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        if (src.getDirectEntity() instanceof Projectile && src.getEntity() instanceof LivingEntity projOwner) {
            double projDmgMult = projOwner.getAttributeValue(ALObjects.Attributes.PROJECTILE_DAMAGE);
            e.setAmount(e.getAmount() * (float) projDmgMult);
        }
    }

    /**
     * Handles {@link ALObjects#DODGE_CHANCE} for melee attacks.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void dodge(LivingIncomingDamageEvent e) {
        LivingEntity target = e.getEntity();
        if (target.level().isClientSide()) return;
        Entity attacker = e.getSource().getDirectEntity();
        if (attacker instanceof Player player) {
            double atkRange = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
            double atkRangeSqr = atkRange * atkRange;
            if (attacker.distanceToSqr(target) <= atkRangeSqr && isDodging(target)) {
                this.onDodge(target);
                e.setCanceled(true);
            }
        }
        else if (attacker instanceof Mob mob) {
            if (mob.isWithinMeleeAttackRange(target) && isDodging(target)) {
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
            // We can skip the distance check for projectiles, as "Projectile Impact" means the projectile is on the target.
            if (isDodging(lvTarget)) {
                this.onDodge(lvTarget);
                e.setCanceled(true);
            }
        }
    }

    private void onDodge(LivingEntity target) {
        target.level().playSound(null, target, ALObjects.Sounds.DODGE, SoundSource.NEUTRAL, 1, 0.7F + target.getRandom().nextFloat() * 0.3F);
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

    @SuppressWarnings("removal")
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void bonusModifiers(ItemAttributeModifierEvent e) {
        ItemStack stack = e.getItemStack();
        ItemAttributeModifiers bonus = stack.get(ALObjects.Components.BONUS_ATTRIBUTE_MODIFIERS);
        if (bonus != null) {
            bonus.modifiers().forEach(entry -> {
                e.addModifier(entry.attribute(), entry.modifier(), entry.slot());
            });
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void stackAttrModifierCompat(ItemAttributeModifierEvent e) {
        var event = new StackAttributeModifiersEvent(e.getItemStack(), StackAttributeModifiers.fromVanilla(e.build()));
        NeoForge.EVENT_BUS.post(event);

        if (event.hasChanges()) {
            e.clearModifiers();
            StackAttributeModifiers newModifs = event.build();
            for (StackAttributeModifiers.Entry entry : newModifs.modifiers()) {
                EquipmentSlotGroup vanilla = EquipmentSlotCompat.toVanilla(entry.slots());
                if (vanilla != null) {
                    e.addModifier(entry.attribute(), entry.modifier(), vanilla);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void bonusStackModifiers(StackAttributeModifiersEvent e) {
        ItemStack stack = e.getItemStack();
        StackAttributeModifiers bonus = stack.get(ALObjects.Components.BONUS_STACK_ATTRIBUTE_MODIFIERS);
        if (bonus != null) {
            bonus.modifiers().forEach(entry -> {
                e.addModifier(entry.attribute(), entry.modifier(), entry.slots());
            });
        }
    }

    /**
     * Adds a fake modifier to show Attack Range to weapons with Attack Damage.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void affixModifiers(ItemAttributeModifierEvent e) {
        boolean hasBaseAD = AttributeHelper.getModifiers(e.getModifiers(), Attributes.ATTACK_DAMAGE).filter(entry -> entry.modifier().id().equals(AttributeUtil.BASE_ATTACK_DAMAGE_ID)).findAny().isPresent();
        if (hasBaseAD) {
            boolean hasBaseAR = AttributeHelper.getModifiers(e.getModifiers(), Attributes.ENTITY_INTERACTION_RANGE).filter(entry -> entry.modifier().id().equals(AttributeUtil.BASE_ENTITY_REACH_ID)).findAny().isPresent();
            if (!hasBaseAR) {
                e.addModifier(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(AttributeUtil.BASE_ENTITY_REACH_ID, 0, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            }
        }

        if (e.getItemStack().has(DataComponents.GLIDER) && e.getModifiers().stream().noneMatch(entry -> entry.attribute().equals(ALObjects.Attributes.ELYTRA_FLIGHT))) {
            e.addModifier(ALObjects.Attributes.ELYTRA_FLIGHT, new AttributeModifier(ApothicAttributes.loc("elytra_item_flight"), 1, Operation.ADD_VALUE), EquipmentSlotGroup.CHEST);
        }
    }

    @SubscribeEvent
    public void reloads(AddServerReloadListenersEvent e) {
        e.addListener(ApothicAttributes.loc("al_config"), ALConfig.makeReloader());
    }

    @SubscribeEvent
    public void cmds(RegisterCommandsEvent e) {
        var builder = Commands.literal("apoth");
        NeoForge.EVENT_BUS.post(new ApotheosisCommandEvent(builder, e.getBuildContext()));
        e.getDispatcher().register(builder);
    }

    @SubscribeEvent
    public void cmds(ApotheosisCommandEvent e) {
        BonusModifierCommand.register(e.getRoot());
    }

    @SubscribeEvent
    public void sync(OnDatapackSyncEvent e) {
        if (e.getPlayer() != null) {
            PacketDistributor.sendToPlayer(e.getPlayer(), new ConfigPayload());
        }
        else {
            PacketDistributor.sendToAllPlayers(new ConfigPayload());
        }
    }

    @SubscribeEvent
    public void tickDmgTrackers(EntityTickEvent.Post e) {
        if (!e.getEntity().level().isClientSide() && e.getEntity().hasData(ALObjects.Attachments.AUX_DMG_TRACKER)) {
            AuxDmgTracker tracker = e.getEntity().getData(ALObjects.Attachments.AUX_DMG_TRACKER);
            tracker.tick();
        }
    }

    /**
     * Random used for dodge calculations.<br>
     * This random is seeded with the target entity's tick count before use.
     */
    private static Random dodgeRand = new Random();

    /**
     * Computes the dodge random seed for the entity. This seed is only unique for the current tick, so that
     * multiple damage instances in the same tick are all dodged.
     * <p>
     * Without this, it would be possible for multiple-instances attacks to only be partially dodged.
     * 
     * @param target The entity being attecked who is rolling to dodge.
     * @return The random seed to use when computing the dodge roll
     */
    public static int computeDodgeSeed(LivingEntity target) {
        int delta = 0x9E3779B9;
        int base = target.tickCount + target.getUUID().hashCode();
        return base + delta + (base << 6) + (base >> 2);
    }

    /**
     * Checks if the target entity will dodge attacks in the current tick, by checking the {@link ALObjects.Attributes#DODGE_CHANCE} value and rolling a random.
     * 
     * @param target The entity being attecked who is rolling to dodge.
     * @return True if the target may dodge, false otherwise.
     */
    public static boolean isDodging(LivingEntity target) {
        double chance = target.getAttributeValue(ALObjects.Attributes.DODGE_CHANCE);
        dodgeRand.setSeed(computeDodgeSeed(target));
        return dodgeRand.nextFloat() <= chance;
    }
}
