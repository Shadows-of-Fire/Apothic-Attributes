package dev.shadowsoffire.apothic_attributes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dev.shadowsoffire.apothic_attributes.api.AttributeChangedValueEvent;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

@Mixin(value = AttributeInstance.class, remap = false)
public interface AttributeInstanceAccessor {

    /**
     * Exposes {@link AttributeInstance#cachedValue} so we can read it while posting {@link AttributeChangedValueEvent}.<br>
     * Due to the hook location, this value will always be the value that was computed prior to application of the latest
     * modifier (the one causing the change).
     */
    @Accessor
    public double getCachedValue();

}
