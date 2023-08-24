package dev.shadowsoffire.attributeslib.mixin;

import org.spongepowered.asm.mixin.Mixin;

import dev.shadowsoffire.attributeslib.util.IEntityOwned;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;

/**
 * For the Creative Flight Attribute, the Abilities must be aware of the owning Player so that the fields can be updated to reflect the attribute value.
 */
@Mixin(Abilities.class)
public class AbilitiesMixin implements IEntityOwned {

    protected LivingEntity owner;

    @Override
    public LivingEntity getOwner() {
        return owner;
    }

    @Override
    public void setOwner(LivingEntity owner) {
        if (this.owner != null) throw new UnsupportedOperationException("Cannot set the owner when it is already set.");
        if (owner == null) throw new UnsupportedOperationException("Cannot set the owner to null.");
        this.owner = owner;
    }

}
