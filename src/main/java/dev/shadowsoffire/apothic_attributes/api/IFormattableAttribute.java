package dev.shadowsoffire.apothic_attributes.api;

import java.util.UUID;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * A Formattable Attribute is one which elects to have control over its tooltip representation.<br>
 * This interface also serves as the primary means of displaying attribute modifiers.
 */
public interface IFormattableAttribute {

    /**
     * Converts the value of an attribute modifier to the value that will be displayed.
     * <p>
     * For multiplication modifiers, this method is responsible for converting the value to percentage form.<br>
     * The only vanilla attribute which performs value formatting is Knockback Resistance.<br>
     *
     * @param op    The operation of the modifier. Null if we are just displaying the raw value and not a modifier.
     * @param value The value of the modifier.
     * @param flag  The tooltip flag.
     * @return The component form of the formatted value.
     */
    default MutableComponent toValueComponent(@Nullable Operation op, double value, TooltipFlag flag) {
        // Knockback Resistance and Swim Speed are percent-based attributes, but we can't registry replace attributes, so we do this here.
        // For Knockback Resistance, vanilla hardcodes a multiplier of 10 for addition values to hide numbers lower than 1,
        // but percent-based is the real desire.
        // For Swim Speed, the implementation is percent-based, but no additional tricks are performed.
        if (this == Attributes.KNOCKBACK_RESISTANCE || this == NeoForgeMod.SWIM_SPEED.value()) {
            return Component.translatable("apothic_attributes.value.percent", ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(value * 100));
        }
        // Speed has no metric, so displaying everything as percent works better for the user.
        // However, Speed also operates in that the default is 0.1, not 1, so we have to special-case it instead of including it above.
        if (this == Attributes.MOVEMENT_SPEED && isNullOrAddition(op)) {
            return Component.translatable("apothic_attributes.value.percent", ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(value * 1000));
        }
        String key = isNullOrAddition(op) ? "apothic_attributes.value.flat" : "apothic_attributes.value.percent";
        return Component.translatable(key, ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(isNullOrAddition(op) ? value : value * 100));
    }

    /**
     * Converts an attribute modifier into its tooltip representation.
     * <p>
     * This method does not handle formatting of "base" modifiers, such as Attack Damage or Attack Speed.
     * <p>
     *
     * @param modif The attribute modifier being converted to a component.
     * @param flag  The tooltip flag.
     * @return The component representation of the passed attribute modifier, with debug info appended if enabled.
     */
    default MutableComponent toComponent(AttributeModifier modif, TooltipFlag flag) {
        Attribute attr = this.ths();
        double value = modif.getAmount();

        MutableComponent comp;

        if (value > 0.0D) {
            comp = Component.translatable("apothic_attributes.modifier.plus", this.toValueComponent(modif.getOperation(), value, flag), Component.translatable(attr.getDescriptionId())).withStyle(ChatFormatting.BLUE);
        }
        else {
            value *= -1.0D;
            comp = Component.translatable("apothic_attributes.modifier.take", this.toValueComponent(modif.getOperation(), value, flag), Component.translatable(attr.getDescriptionId())).withStyle(ChatFormatting.RED);
        }

        return comp.append(this.getDebugInfo(modif, flag));
    }

    /**
     * Computes the additional debug information for a given attribute modifier, if the flag {@linkplain TooltipFlag#isAdvanced() is advanced}.
     * 
     * @param modif The attribute modifier being converted to a component.
     * @param flag  The tooltip flag.
     * @return The debug component, or {@link CommonComponents#EMPTY} if disabled.
     * @apiNote This information is automatically appended to {@link #toComponent(AttributeModifier, TooltipFlag)}.
     */
    default Component getDebugInfo(AttributeModifier modif, TooltipFlag flag) {
        Component debugInfo = CommonComponents.EMPTY;

        if (flag.isAdvanced()) {
            // Advanced Tooltips show the underlying operation and the "true" value. We offset MULTIPLY_TOTAL by 1 due to how the operation is calculated.
            double advValue = (modif.getOperation() == Operation.MULTIPLY_TOTAL ? 1 : 0) + modif.getAmount();
            String valueStr = ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(advValue);
            String txt = switch (modif.getOperation()) {
                case ADDITION -> advValue > 0 ? String.format("[+%s]", valueStr) : String.format("[%s]", valueStr);
                case MULTIPLY_BASE -> advValue > 0 ? String.format("[+%sx]", valueStr) : String.format("[%sx]", valueStr);
                case MULTIPLY_TOTAL -> String.format("[x%s]", valueStr);
            };
            debugInfo = Component.literal(" ")
                .append(Component.literal(txt).withStyle(ChatFormatting.GRAY));
        }
        return debugInfo;
    }

    /**
     * Gets the specific UUID that represents a "base" (green) modifier for this attribute.
     *
     * @param modif The attribute modifier being checked.
     * @param flag  The tooltip flag.
     * @return The UUID of the "base" modifier, or null, if no such modifier may exist.
     */
    @Nullable
    default UUID getBaseUUID() {
        if (this == Attributes.ATTACK_DAMAGE) return AttributeHelper.BASE_ATTACK_DAMAGE;
        else if (this == Attributes.ATTACK_SPEED) return AttributeHelper.BASE_ATTACK_SPEED;
        else if (this == NeoForgeMod.ENTITY_REACH.value()) return AttributeHelper.BASE_ENTITY_REACH;
        return null;
    }

    /**
     * Converts an attribute modifier into its tooltip representation.
     * <p>
     * This method does not handle formatting of "base" modifiers, such as Attack Damage or Attack Speed.
     * <p>
     *
     * @param modif The attribute modifier being converted to a component.
     * @param flag  The tooltip flag.
     * @return The component representation of the passed attribute modifier.
     */
    default MutableComponent toBaseComponent(double value, double entityBase, boolean merged, TooltipFlag flag) {
        Attribute attr = this.ths();

        Component debugInfo = CommonComponents.EMPTY;

        if (flag.isAdvanced() && !merged) {
            // Advanced Tooltips cause us to emit the entity's base value and the base value of the item.
            debugInfo = Component.literal(" ")
                .append(Component.translatable(ApothicAttributes.MODID + ".adv.base", ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(entityBase), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(value - entityBase)).withStyle(ChatFormatting.GRAY));
        }

        MutableComponent comp = Component.translatable("attribute.modifier.equals.0", ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(value), Component.translatable(attr.getDescriptionId()));

        return comp.append(debugInfo);
    }

    /**
     * Certain attributes, such as Attack Damage, are increased by an Enchantment that doesn't actually apply
     * an attribute modifier.<br>
     * This method allows for including certain additional variables in the computation of "base" attribute values.
     *
     * @param stack The stack in question.
     * @return Any bonus value to be applied to the attribute's value, after all modifiers have been applied.
     */
    default double getBonusBaseValue(ItemStack stack) {
        if (this == Attributes.ATTACK_DAMAGE) return EnchantmentHelper.getDamageBonus(stack, MobType.UNDEFINED);
        return 0;
    }

    /**
     * This method is invoked when {@link #getBonusBaseValue(ItemStack)} returns a value higher than zero.<br>
     * It is responsible for adding tooltip lines that explain where the bonus values from {@link #getBonusBaseValue(ItemStack)} are from.
     *
     * @param stack   The stack in question.
     * @param tooltip The tooltip consumer.
     * @param flag    The tooltip flag.
     */
    default void addBonusTooltips(ItemStack stack, Consumer<Component> tooltip, TooltipFlag flag) {
        if (this == Attributes.ATTACK_DAMAGE) {
            float sharpness = EnchantmentHelper.getDamageBonus(stack, MobType.UNDEFINED);
            Component debugInfo = CommonComponents.EMPTY;
            if (flag.isAdvanced()) {
                // Show the user that this fake modifier is from Sharpness.
                debugInfo = Component.literal(" ").append(Component.translatable(ApothicAttributes.MODID + ".adv.sharpness_bonus", sharpness).withStyle(ChatFormatting.GRAY));
            }
            MutableComponent comp = AttributeHelper.list()
                .append(Component.translatable("attribute.modifier.plus.0", ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(sharpness), Component.translatable(this.ths().getDescriptionId())).withStyle(ChatFormatting.BLUE));
            tooltip.accept(comp.append(debugInfo));
        }
    }

    default Attribute ths() {
        return (Attribute) this;
    }

    /**
     * Helper method to invoke {@link #toComponent(AttributeModifier, TooltipFlag)}.
     */
    public static MutableComponent toComponent(Attribute attr, AttributeModifier modif, TooltipFlag flag) {
        return ((IFormattableAttribute) attr).toComponent(modif, flag);
    }

    /**
     * Helper method to invoke {@link #toValueComponent(Operation, double, TooltipFlag)}.
     */
    public static MutableComponent toValueComponent(Attribute attr, Operation op, double value, TooltipFlag flag) {
        return ((IFormattableAttribute) attr).toValueComponent(op, value, flag);
    }

    /**
     * Helper method to invoke {@link #toBaseComponent(double, double, boolean, TooltipFlag)}
     */
    public static MutableComponent toBaseComponent(Attribute attr, double value, double entityBase, boolean merged, TooltipFlag flag) {
        return ((IFormattableAttribute) attr).toBaseComponent(value, entityBase, merged, flag);
    }

    static boolean isNullOrAddition(@Nullable Operation op) {
        return op == null || op == Operation.ADDITION;
    }

}
