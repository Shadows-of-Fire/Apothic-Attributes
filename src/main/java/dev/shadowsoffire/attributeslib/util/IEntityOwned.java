package dev.shadowsoffire.attributeslib.util;

import net.minecraft.world.entity.LivingEntity;

public interface IEntityOwned {

    public LivingEntity getOwner();

    public void setOwner(LivingEntity owner);

}
