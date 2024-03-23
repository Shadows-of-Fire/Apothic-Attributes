package dev.shadowsoffire.apothic_attributes.util;

import net.minecraft.world.entity.player.Abilities;

/**
 * Class used to cache and manipulate the value of {@link Abilities#flying} so it doesn't get dropped on respawn or login.
 */
public interface IFlying {

    /**
     * If the user was flying the last time {@link Abilities#flying} was read from disk.<br>
     * Nukes the cached value after retrieving it so that future calls are invalid.
     */
    public boolean getAndDestroyFlyingCache();

    /**
     * Marks the player as previously flying so that the {@link Abilities#flying} state is restored.
     */
    public void markFlying();

}
