package dev.shadowsoffire.apothic_attributes.util;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class AuxDmgTracker {

    public static final Codec<AuxDmgTracker> CODEC = Codec.unboundedMap(ResourceKey.codec(Registries.DAMAGE_TYPE), Entry.CODEC).xmap(AuxDmgTracker::new, tracker -> tracker.data);

    private final IdentityHashMap<ResourceKey<DamageType>, Entry> data = new IdentityHashMap<>(3);

    private transient int origInvulTime = 0;
    private transient float origLastHurt = 0;

    public AuxDmgTracker() {}

    public AuxDmgTracker(Map<ResourceKey<DamageType>, Entry> data) {
        this.data.putAll(data);
    }

    /**
     * Retrieves the aux damage tracker for the given entity and runs an execution pipeline with it.
     * <p>
     * If you use this, you do not need to manually call {@link #start(LivingEntity)} and {@link #end(LivingEntity)}.
     */
    public static void executeWith(LivingEntity entity, Consumer<AuxDmgTracker> consumer) {
        AuxDmgTracker tracker = entity.getData(ALObjects.Attachments.AUX_DMG_TRACKER);
        tracker.start(entity);
        try {
            consumer.accept(tracker);
        }
        finally {
            tracker.end(entity);
        }
    }

    /**
     * Calls {@link #attackWith(LivingEntity, LivingEntity, ResourceKey, float, PostAttackEffect)} using
     * the attacker's value of {@code attribute} as the damage amount.
     */
    public boolean attackWith(LivingEntity attacker, LivingEntity target, ResourceKey<DamageType> type, Holder<Attribute> attribute, @Nullable PostAttackEffect callback) {
        float damage = (float) attacker.getAttributeValue(attribute);
        return attackWith(attacker, target, type, damage, callback);
    }

    /**
     * Attacks the target entity with the given damage type and damage amount.
     * 
     * @param attacker The attacking entity
     * @param target   The target entity
     * @param type     A resource key for the damage type, which will be resolved through the Level's registry access.
     * @param damage   The amount of damage to deal. This is multiplied by the attacker's attack strength(s) (including sweeping if applicable).
     * @param callback A post-attack callback to run after the attack is complete.
     * @return If the attack was successful. This will be true if the target was not already dead, and the damage was greater than 0.001.
     */
    public boolean attackWith(LivingEntity attacker, LivingEntity target, ResourceKey<DamageType> type, float damage, @Nullable PostAttackEffect callback) {
        float atkStrength = ApothicAttributes.getLocalAtkStrength(attacker);
        debugLog("Attacking {} with {}: damage = {}, atkStrength = {}", target, type.location(), damage, atkStrength);
        if (damage > 0.001 && atkStrength >= 0.55F && !target.isDeadOrDying()) {
            this.setup(target, type);
            float dmg = modifyDamage(attacker, target, atkStrength * damage);
            float health = target.getHealth();
            if (target.hurt(src(type, attacker), dmg)) {
                float delta = health - target.getHealth();
                debugLog("Attack successful: {} -> {}: type = {}, damage = {}, atkStrength = {}", attacker, target, type.location(), dmg, atkStrength);
                if (callback != null) {
                    callback.apply(attacker, target, src(type, attacker), dmg, delta);
                }
                this.record(target, type);
                return true;
            }
        }

        debugLog("Skipping attack: damage = {}, atkStrength = {}, target.isDeadOrDying() = {}", damage, atkStrength, target.isDeadOrDying());
        return false;
    }

    /**
     * Starts an aux damage pipeline by recording the original values of {@link Entity#invulnerableTime} and {@link LivingEntity#lastHurt}.
     */
    public void start(LivingEntity entity) {
        this.origInvulTime = entity.invulnerableTime;
        this.origLastHurt = entity.lastHurt;
        debugLog("Starting aux damage pipeline for {}: invulTime = {}, lastHurt = {}", entity, this.origInvulTime, this.origLastHurt);
    }

    /**
     * Sets up the entity to be attacked with the given damage type.
     * This will set the entity's {@link Entity#invulnerableTime} and {@link LivingEntity#lastHurt} to the values stored in the aux damage tracker.
     */
    public void setup(LivingEntity entity, ResourceKey<DamageType> type) {
        Entry entry = getData(type);
        entity.invulnerableTime = entry.time();
        entity.lastHurt = entry.lastHurt();
        debugLog("Setup values for {} / {}: invulTime = {}, lastHurt = {}", entity, type.location(), entry.time(), entry.lastHurt());
    }

    /**
     * Records the post-attack values for the given damage type.
     * This will store the entity's {@link Entity#invulnerableTime} and {@link LivingEntity#lastHurt} in the aux damage tracker.
     */
    public void record(LivingEntity entity, ResourceKey<DamageType> type) {
        Entry entry = new Entry(entity.invulnerableTime, entity.lastHurt);
        data.put(type, entry);
        debugLog("Recorded values for {} / {}: invulTime = {}, lastHurt = {}", entity, type.location(), entry.time(), entry.lastHurt());
    }

    /**
     * Ends an aux damage pipeline by restoring the known values of {@link} {@link Entity#invulnerableTime} and {@link LivingEntity#lastHurt}.
     */
    public void end(LivingEntity entity) {
        entity.invulnerableTime = this.origInvulTime;
        entity.lastHurt = this.origLastHurt;
        debugLog("Ending aux damage pipeline for {}: invulTime = {}, lastHurt = {}", entity, this.origInvulTime, this.origLastHurt);
    }

    public void tick() {
        this.data.values().forEach(entry -> {
            if (entry.time > 0) {
                entry.time--;
            }
        });
    }

    public Entry getData(ResourceKey<DamageType> type) {
        return data.getOrDefault(type, Entry.DEFAULT);
    }

    private static DamageSource src(ResourceKey<DamageType> type, LivingEntity entity) {
        return entity.level().damageSources().source(type, entity);
    }

    /**
     * If we're in the middle of a sweep attack, we need to scale down the attack strength based on the sweeping ratio.
     * PlayerMixin is responsible for raising apoth.hit_by_sweep_attack.
     */
    private static float modifyDamage(LivingEntity attacker, LivingEntity target, float damage) {
        if (target.getPersistentData().getBoolean("apoth.hit_by_sweep_attack") && attacker.getAttributes().hasAttribute(Attributes.SWEEPING_DAMAGE_RATIO)) {
            float realDmg = Math.min(damage, 1 + (float) attacker.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * damage);
            debugLog("Sweep attack detected. Modifying damage for {} from {} to {}.", attacker, damage, realDmg);
            return realDmg;
        }
        return damage;
    }

    private static final Marker MARKER = MarkerManager.getMarker(AuxDmgTracker.class.getSimpleName());

    private static void debugLog(String msg, Object... args) {
        if (ApothicAttributes.DEBUG_AUX_DMG) {
            ApothicAttributes.LOGGER.debug(MARKER, msg, args);
        }
    }

    /**
     * Used to provide a callback for post-attack effects when calling
     * {@link AuxDmgTracker#attackWith(LivingEntity, LivingEntity, ResourceKey, float, PostAttackEffect)}.
     */
    public static interface PostAttackEffect {
        /**
         * Called upon a successful attack with an aux damage type, after {@link LivingEntity#hurt(DamageSource, float)} has returned true.
         * 
         * @param attacker The attacking entity
         * @param target   The target entity
         * @param src      The damage source used to attack the target
         * @param damage   The amount of damage that was passed to hurt(), not the actual damage dealt.
         * @param delta    The difference between the target's health before and after the attack.
         */
        void apply(LivingEntity attacker, LivingEntity target, DamageSource src, float damage, float delta);
    }

    public static class Entry {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("time").forGetter(Entry::time),
                Codec.floatRange(0, Float.MAX_VALUE).fieldOf("lastHurt").forGetter(Entry::lastHurt))
            .apply(inst, Entry::new));

        public static final Entry DEFAULT = new Entry(0, 0);

        protected int time;
        protected final float lastHurt;

        public Entry(int time, float lastHurt) {
            this.time = Math.max(0, time);
            this.lastHurt = Math.max(0, lastHurt);
        }

        public int time() {
            return time;
        }

        public float lastHurt() {
            return lastHurt;
        }
    }
}
