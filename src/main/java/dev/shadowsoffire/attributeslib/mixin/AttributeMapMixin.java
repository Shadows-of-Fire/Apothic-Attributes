package dev.shadowsoffire.attributeslib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.shadowsoffire.attributeslib.api.AttributeChangedValueEvent;
import dev.shadowsoffire.attributeslib.util.IEntityOwned;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraftforge.common.MinecraftForge;

/**
 * For the {@link AttributeChangedValueEvent} to have the necessary entity context, the attribute map must be aware of the owning entity.<br>
 * Once that context is known, firing the event is a hook in {@link AttributeMap#onAttributeModified}.
 * <p>
 * The client event is posted from the client packet listener as this method is unreliable on the client due to how attribute sync is done.
 */
@Mixin(AttributeMap.class)
public class AttributeMapMixin implements IEntityOwned {

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

    /**
     * Serverside call site for {@link AttributeChangedValueEvent}. Uses the built-in hook {@link AttributeMap#onAttributeModified} and the stapled-in entity
     * context.
     * <p>
     * Not used on the client since it would cause it to be fired excessively many times during sync.
     */
    @Inject(at = @At(value = "HEAD"), method = "onAttributeModified(Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;)V", require = 1)
    public void apoth_attrModifiedEvent(AttributeInstance inst, CallbackInfo ci) {
        if (!owner.level().isClientSide) {
            // This call site is only valid on the server, because the client nukes and reapplies all attribute modifiers when received.
            double oldValue = ((AttributeInstanceAccessor) inst).getCachedValue();
            double newValue = inst.getValue(); // Calling getValue will compute the value once marked dirty.
            if (oldValue != newValue) {
                MinecraftForge.EVENT_BUS.post(new AttributeChangedValueEvent(getOwner(), inst, oldValue, newValue));
            }
        }
    }

}
