package dev.shadowsoffire.apothic_attributes.util;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

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
     * Starts an aux damage pipeline by recording the original values of {@link Entity#invulnerableTime} and {@link LivingEntity#lastHurt}.
     */
    public void start(LivingEntity entity) {
        this.origInvulTime = entity.invulnerableTime;
        this.origLastHurt = entity.lastHurt;
    }

    /**
     * Sets up the entity to be attacked with the given damage type.
     * This will set the entity's {@link Entity#invulnerableTime} and {@link LivingEntity#lastHurt} to the values stored in the aux damage tracker.
     */
    public void setup(LivingEntity entity, ResourceKey<DamageType> type) {
        Entry entry = getData(type);
        entity.invulnerableTime = entry.time();
        entity.lastHurt = entry.lastHurt();
    }

    /**
     * Records the post-attack values for the given damage type.
     * This will store the entity's {@link Entity#invulnerableTime} and {@link LivingEntity#lastHurt} in the aux damage tracker.
     */
    public void record(LivingEntity entity, ResourceKey<DamageType> type) {
        Entry entry = new Entry(entity.invulnerableTime, entity.lastHurt);
        data.put(type, entry);
    }

    /**
     * Ends an aux damage pipeline by restoring the known values of {@link} {@link Entity#invulnerableTime} and {@link LivingEntity#lastHurt}.
     */
    public void end(LivingEntity entity) {
        entity.invulnerableTime = this.origInvulTime;
        entity.lastHurt = this.origLastHurt;
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
