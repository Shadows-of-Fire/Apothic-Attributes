package dev.shadowsoffire.apothic_attributes.api;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * Stores per-entity cooldown timestamps keyed by an arbitrary {@link Identifier}.
 * <p>
 * Stored as a synced attachment on all living entities.
 * External callers should use {@link AbilityCooldowns} instead of this class, as this does not respect cooldown reduction.
 */
public class CooldownTracker {

    /**
     * Absolute max limit on how long a cooldown tick timer may last (4 hours).
     * <p>
     * Cooldowns that were started more than this amount of time ago will be pruned.
     */
    public static final int MAX_COOLDOWN_TICKS = 20 * 60 * 60 * 4;

    public static final MapCodec<CooldownTracker> CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.LONG).fieldOf("entries")
        .xmap(CooldownTracker::new, t -> t.entries);

    public static final StreamCodec<RegistryFriendlyByteBuf, CooldownTracker> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_LONG), t -> t.entries,
        CooldownTracker::new);

    private final Object2LongMap<Identifier> entries;

    public CooldownTracker() {
        this(Map.of());
    }

    public CooldownTracker(Map<Identifier, Long> entries) {
        this.entries = new Object2LongOpenHashMap<>(entries);
        this.entries.defaultReturnValue(-1);
    }

    /**
     * Returns true if {@code id} was triggered less than {@code cooldownTicks} ago, given the current {@code gameTime}.
     */
    public boolean isOnCooldown(Identifier id, int cooldownTicks, long gameTime) {
        long lastApplied = this.entries.getLong(id);
        return lastApplied != -1 && lastApplied + cooldownTicks >= gameTime;
    }

    /**
     * Records that {@code id} fired at {@code gameTime}.
     */
    public void startCooldown(Identifier id, long gameTime) {
        this.entries.put(id, gameTime);
    }

    /**
     * Removes the cooldown entry for {@code id}, if present.
     */
    public void clear(Identifier id) {
        this.entries.removeLong(id);
    }

    /**
     * Returns the number of ticks remaining on {@code id}, or 0 if not on cooldown.
     */
    public long getRemaining(Identifier id, int cooldownTicks, long gameTime) {
        long lastApplied = this.entries.getLong(id);
        if (lastApplied == -1) return 0;
        long remaining = (lastApplied + cooldownTicks) - gameTime;
        return Math.max(0, remaining);
    }

    /**
     * Drops entries that expired more than {@link #MAX_COOLDOWN_TICKS} before {@code gameTime}.
     * Returns true if any entries were removed.
     */
    public boolean prune(long gameTime) {
        return this.entries.values().removeIf(lastApplied -> lastApplied + MAX_COOLDOWN_TICKS < gameTime);
    }

}
