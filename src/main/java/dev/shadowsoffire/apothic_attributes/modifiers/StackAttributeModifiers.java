package dev.shadowsoffire.apothic_attributes.modifiers;

import java.util.List;
import java.util.function.BiConsumer;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * More generic version of {@link StackAttributeModifiers}, which supports modifiers for slots outside of {@link EquipmentSlotGroup}.
 */
public record StackAttributeModifiers(List<StackAttributeModifiers.Entry> modifiers, boolean showInTooltip) {

    public static final StackAttributeModifiers EMPTY = new StackAttributeModifiers(List.of(), true);

    private static final Codec<StackAttributeModifiers> FULL_CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            StackAttributeModifiers.Entry.CODEC.listOf().fieldOf("modifiers").forGetter(StackAttributeModifiers::modifiers),
            Codec.BOOL.optionalFieldOf("show_in_tooltip", Boolean.valueOf(true)).forGetter(StackAttributeModifiers::showInTooltip))
        .apply(inst, StackAttributeModifiers::new));

    public static final Codec<StackAttributeModifiers> CODEC = Codec.withAlternative(
        FULL_CODEC, StackAttributeModifiers.Entry.CODEC.listOf(), p_332621_ -> new StackAttributeModifiers(p_332621_, true));

    public static final StreamCodec<RegistryFriendlyByteBuf, StackAttributeModifiers> STREAM_CODEC = StreamCodec.composite(
        StackAttributeModifiers.Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
        StackAttributeModifiers::modifiers,
        ByteBufCodecs.BOOL,
        StackAttributeModifiers::showInTooltip,
        StackAttributeModifiers::new);

    public StackAttributeModifiers withTooltip(boolean showInTooltip) {
        return new StackAttributeModifiers(this.modifiers, showInTooltip);
    }

    public StackAttributeModifiers withModifierAdded(Holder<Attribute> attribute, AttributeModifier modifier, EntitySlotGroup slots) {
        ImmutableList.Builder<StackAttributeModifiers.Entry> builder = ImmutableList.builderWithExpectedSize(this.modifiers.size() + 1);

        for (StackAttributeModifiers.Entry itemattributemodifiers$entry : this.modifiers) {
            if (!itemattributemodifiers$entry.matches(attribute, modifier.id())) {
                builder.add(itemattributemodifiers$entry);
            }
        }

        builder.add(new StackAttributeModifiers.Entry(attribute, modifier, slots));
        return new StackAttributeModifiers(builder.build(), this.showInTooltip);
    }

    public void forEach(EntitySlotGroup slots, BiConsumer<Holder<Attribute>, AttributeModifier> action) {
        for (StackAttributeModifiers.Entry entry : this.modifiers) {
            if (entry.slots.equals(slots)) {
                action.accept(entry.attribute, entry.modifier);
            }
        }
    }

    public void forEach(Holder<EntityEquipmentSlot> slot, BiConsumer<Holder<Attribute>, AttributeModifier> action) {
        for (StackAttributeModifiers.Entry entry : this.modifiers) {
            if (entry.slots.test(slot)) {
                action.accept(entry.attribute, entry.modifier);
            }
        }
    }

    public static StackAttributeModifiers fromVanilla(ItemAttributeModifiers modifiers) {
        Builder builder = builder();
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), EquipmentSlotCompat.fromVanilla(entry.slot()));
        }
        return builder.build();
    }

    public static StackAttributeModifiers.Builder builder() {
        return new StackAttributeModifiers.Builder();
    }

    public static class Builder {
        private final ImmutableList.Builder<StackAttributeModifiers.Entry> entries = ImmutableList.builder();

        Builder() {}

        public StackAttributeModifiers.Builder add(Holder<Attribute> attribute, AttributeModifier modifier, EntitySlotGroup slots) {
            this.entries.add(new StackAttributeModifiers.Entry(attribute, modifier, slots));
            return this;
        }

        public StackAttributeModifiers build() {
            return new StackAttributeModifiers(this.entries.build(), true);
        }
    }

    public static record Entry(Holder<Attribute> attribute, AttributeModifier modifier, EntitySlotGroup slots) {

        public static final Codec<StackAttributeModifiers.Entry> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(
            p_348388_ -> p_348388_.group(
                Attribute.CODEC.fieldOf("type").forGetter(StackAttributeModifiers.Entry::attribute),
                AttributeModifier.MAP_CODEC.forGetter(StackAttributeModifiers.Entry::modifier),
                EntitySlotGroup.CODEC.optionalFieldOf("slot", ALObjects.EquipmentSlotGroups.ANY).forGetter(StackAttributeModifiers.Entry::slots))
                .apply(p_348388_, StackAttributeModifiers.Entry::new)));

        public static final StreamCodec<RegistryFriendlyByteBuf, StackAttributeModifiers.Entry> STREAM_CODEC = StreamCodec.composite(
            Attribute.STREAM_CODEC, StackAttributeModifiers.Entry::attribute,
            AttributeModifier.STREAM_CODEC, StackAttributeModifiers.Entry::modifier,
            EntitySlotGroup.STREAM_CODEC, StackAttributeModifiers.Entry::slots,
            StackAttributeModifiers.Entry::new);

        public boolean matches(Holder<Attribute> attribute, Identifier id) {
            return attribute.equals(this.attribute) && this.modifier.is(id);
        }
    }
}
