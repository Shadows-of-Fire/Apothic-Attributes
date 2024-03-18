package dev.shadowsoffire.attributeslib.api;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.neoforged.bus.api.Event;

/**
 * This event is fired whenever the value of an attribute changes values.<br>
 * It is fired on both sides at different points:
 * <ul>
 * <li>On the Server, it is fired from {@link AttributeMap#onAttributeModified} which is the builtin callback hook for values changing.</li>
 * <li>On the Client, it is fired from {@link ClientPacketListener#handleUpdateAttributes} after all changes have been processed.</li>
 * </ul>
 */
public class AttributeChangedValueEvent extends Event {

    protected LivingEntity entity;
    protected AttributeInstance attrInst;
    protected double oldValue, newValue;

    public AttributeChangedValueEvent(LivingEntity entity, AttributeInstance attrInst, double oldValue, double newValue) {
        this.entity = entity;
        this.attrInst = attrInst;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * @return The Entity whose attribute was modified.
     */
    public LivingEntity getEntity() {
        return this.entity;
    }

    /**
     * @return The Attribute instance whose value has changed.
     */
    public AttributeInstance getAttributeInstance() {
        return this.attrInst;
    }

    /**
     * @return The old value of the attribute, before the change occurred.
     */
    public double getOldValue() {
        return this.oldValue;
    }

    /**
     * @return The new value of the attribute, after the change occurred.
     */
    public double getNewValue() {
        return this.newValue;
    }

}
