/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package dev.shadowsoffire.apothic_attributes.modifiers;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

/**
 * This event is fired when the attributes for an item stack are queried (for any reason) through {@link ItemStack#getAttributeModifiers()}.
 * <br>
 * This event is fired regardless of if the stack has {@link DataComponents#ATTRIBUTE_MODIFIERS} or not. If your attribute should be
 * ignored when attributes are overridden, you can check for the presence of the component.
 * <p>
 * This event may be fired on both the logical server and logical client.
 */
public class StackAttributeModifiersEvent extends Event {
    private final ItemStack stack;
    private final StackAttributeModifiers defaultModifiers;
    private StackAttributeModifiersBuilder builder;

    @ApiStatus.Internal
    public StackAttributeModifiersEvent(ItemStack stack, StackAttributeModifiers defaultModifiers) {
        this.stack = stack;
        this.defaultModifiers = defaultModifiers;
    }

    /**
     * {@return the item stack whose attribute modifiers are being computed}
     */
    public ItemStack getItemStack() {
        return this.stack;
    }

    /**
     * {@return the default attribute modifiers before changes made by the event}
     */
    public StackAttributeModifiers getDefaultModifiers() {
        return this.defaultModifiers;
    }

    /**
     * Returns an unmodifiable view of the attribute modifier entries. Do not use the returned value to create an {@link StackAttributeModifiers}
     * since the underlying list is not immutable.
     * <p>
     * If you need an {@link StackAttributeModifiers}, you may need to call {@link #build()}
     * 
     * @apiNote Use other methods from this event to adjust the modifiers.
     */
    public List<StackAttributeModifiers.Entry> getModifiers() {
        return this.builder == null ? this.defaultModifiers.modifiers() : this.builder.getEntryView();
    }

    /**
     * Adds a new attribute modifier to the given stack. Two modifiers with the same id may not exist for the same attribute, and this method will fail if one
     * exists.
     * 
     * @param attribute The attribute the modifier is for
     * @param modifier  The new attribute modifier
     * @param slot      The equipment slots for which the modifier should apply
     * @return True if the modifier was added, false if it was already present
     * @apiNote Modifiers must have a unique and consistent {@link Identifier} id, or the modifier will not be removed when the item is unequipped.
     */
    public boolean addModifier(Holder<Attribute> attribute, AttributeModifier modifier, EntitySlotGroup slot) {
        return getBuilder().addModifier(attribute, modifier, slot);
    }

    /**
     * Removes an attribute modifier for the target attribute by id
     * 
     * @return True if an attribute modifier was removed, false otherwise
     */
    public boolean removeModifier(Holder<Attribute> attribute, Identifier id) {
        return getBuilder().removeModifier(attribute, id);
    }

    /**
     * Adds a new attribute modifier to the given stack, optionally replacing any existing modifiers with the same id.
     * 
     * @param attribute The attribute the modifier is for
     * @param modifier  The new attribute modifier
     * @param slot      The equipment slots for which the modifier should apply
     * @apiNote Modifiers must have a unique and consistent {@link Identifier} id, or the modifier will not be removed when the item is unequipped.
     */
    public void replaceModifier(Holder<Attribute> attribute, AttributeModifier modifier, EntitySlotGroup slot) {
        getBuilder().replaceModifier(attribute, modifier, slot);
    }

    /**
     * Removes modifiers based on a condition.
     * 
     * @return true if any modifiers were removed
     */
    public boolean removeIf(Predicate<StackAttributeModifiers.Entry> condition) {
        return getBuilder().removeIf(condition);
    }

    /**
     * Removes all modifiers for the given attribute.
     * 
     * @return true if any modifiers were removed
     */
    public boolean removeAllModifiersFor(Holder<Attribute> attribute) {
        return getBuilder().removeIf(entry -> entry.attribute().equals(attribute));
    }

    /**
     * Removes all modifiers for all attributes.
     */
    public void clearModifiers() {
        getBuilder().clear();
    }

    /**
     * Builds a new {@link StackAttributeModifiers} with the results of this event, returning the
     * {@linkplain #getDefaultModifiers() default modifiers} if no changes were made.
     */
    public StackAttributeModifiers build() {
        return this.builder == null ? this.defaultModifiers : this.builder.build(this.defaultModifiers.showInTooltip());
    }

    /**
     * Returns true if any changes have been made to the attribute modifiers as a result of this event.
     */
    public boolean hasChanges() {
        return this.builder != null;
    }

    /**
     * Returns the builder used for adjusting the attribute modifiers, creating it if it does not already exist.
     */
    private StackAttributeModifiersBuilder getBuilder() {
        if (this.builder == null) {
            this.builder = new StackAttributeModifiersBuilder(this.defaultModifiers);
        }

        return this.builder;
    }

    /**
     * Advanced version of {@link StackAttributeModifiers.Builder} which supports removal and better sanity-checking.
     * <p>
     * The original builder only supports additions and does not guarantee that no duplicate modifiers exist for a given id.
     */
    private static class StackAttributeModifiersBuilder {
        private List<StackAttributeModifiers.Entry> entries;
        private Map<Key, StackAttributeModifiers.Entry> entriesByKey;

        StackAttributeModifiersBuilder(StackAttributeModifiers defaultModifiers) {
            this.entries = new LinkedList<>();
            this.entriesByKey = new HashMap<>(defaultModifiers.modifiers().size());

            for (StackAttributeModifiers.Entry entry : defaultModifiers.modifiers()) {
                entries.add(entry);
                entriesByKey.put(new Key(entry.attribute(), entry.modifier().id()), entry);
            }
        }

        /**
         * {@return an unmodifiable view of the underlying entry list}
         */
        List<StackAttributeModifiers.Entry> getEntryView() {
            return Collections.unmodifiableList(this.entries);
        }

        /**
         * Attempts to add a new modifier, refusing if one is already present with the same id.
         * 
         * @return true if the modifier was added
         */
        boolean addModifier(Holder<Attribute> attribute, AttributeModifier modifier, EntitySlotGroup slot) {
            Key key = new Key(attribute, modifier.id());
            if (entriesByKey.containsKey(key)) {
                return false;
            }

            StackAttributeModifiers.Entry entry = new StackAttributeModifiers.Entry(attribute, modifier, slot);
            entries.add(entry);
            entriesByKey.put(key, entry);
            return true;
        }

        /**
         * Removes a modifier for the target attribute with the given id.
         * 
         * @return true if a modifier was removed
         */
        boolean removeModifier(Holder<Attribute> attribute, Identifier id) {
            StackAttributeModifiers.Entry entry = entriesByKey.remove(new Key(attribute, id));

            if (entry != null) {
                entries.remove(entry);
                return true;
            }

            return false;
        }

        /**
         * Adds a modifier to the list, replacing any existing modifiers with the same id.
         * 
         * @return the previous modifier, or null if there was no previous modifier with the same id
         */
        @Nullable
        StackAttributeModifiers.Entry replaceModifier(Holder<Attribute> attribute, AttributeModifier modifier, EntitySlotGroup slot) {
            Key key = new Key(attribute, modifier.id());
            StackAttributeModifiers.Entry entry = new StackAttributeModifiers.Entry(attribute, modifier, slot);
            if (entriesByKey.containsKey(key)) {
                StackAttributeModifiers.Entry previousEntry = entriesByKey.get(key);
                int index = entries.indexOf(previousEntry);
                if (index != -1) {
                    entries.set(index, entry);
                }
                else { // This should never happen, but it can't hurt to have anyways
                    entries.add(entry);
                }
                entriesByKey.put(key, entry);
                return previousEntry;
            }
            else {
                entries.add(entry);
                entriesByKey.put(key, entry);
                return null;
            }
        }

        /**
         * Removes modifiers based on a condition.
         * 
         * @return true if any modifiers were removed
         */
        boolean removeIf(Predicate<StackAttributeModifiers.Entry> condition) {
            this.entries.removeIf(condition);
            return this.entriesByKey.values().removeIf(condition);
        }

        void clear() {
            this.entries.clear();
            this.entriesByKey.clear();
        }

        public StackAttributeModifiers build(boolean showInTooltip) {
            return new StackAttributeModifiers(ImmutableList.copyOf(this.entries), showInTooltip);
        }

        /**
         * Internal key class. Attribute modifiers are unique by id for each Attribute.
         */
        private static record Key(Holder<Attribute> attr, Identifier id) {

        }
    }
}
