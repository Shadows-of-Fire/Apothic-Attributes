package dev.shadowsoffire.apothic_attributes.util;

import net.minecraft.world.entity.LivingEntity;

public interface IEntityOwned {

    public LivingEntity getOwner();

    public void setOwner(LivingEntity owner);

}
