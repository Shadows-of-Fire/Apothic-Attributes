package dev.shadowsoffire.attributeslib.util;

import net.minecraft.world.entity.player.Abilities;

public interface IFlying {

    /**
     * If the user was flying the last time {@link Abilities#flying} was read from disk.<br>
     * Nukes the cached value after retrieving it so that future calls are invalid.
     */
    public boolean getAndDestroyFlyingCache();

}
