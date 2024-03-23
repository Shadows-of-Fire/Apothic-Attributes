package dev.shadowsoffire.attributeslib.impl;

import org.jetbrains.annotations.Nullable;

import dev.shadowsoffire.attributeslib.api.IFormattableAttribute;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.TooltipFlag;

/**
 * A Boolean Attribute is one which displays modifiers as "Enables" or "Forcibly Disables".<br>
 * For these attributes, you should only use the following modifier values:
 * <ul>
 * <li>A value of 1 with {@link Operation#ADDITION} to enable the effect.</li>
 * <li>A value of -1 with {@link Operation#MULTIPLY_TOTAL} to forcibly disable the effect.</li>
 * </ul>
 * This behavior allows for multiple enables to coexist, not removing the effect unless all enabling modifiers are removed.<br>
 * Additionally, it permits forcibly disabling the attribute through multiply total.
 * <p>
 * Modifiers not using one of the specified modifiers noted above will display as an error condition.
 */
public class BooleanAttribute extends RangedAttribute implements IFormattableAttribute {

    public BooleanAttribute(String pDescriptionId, boolean defaultValue) {
        super(pDescriptionId, defaultValue ? 1 : 0, 0, Double.MAX_VALUE);
    }

    @Override
    public MutableComponent toValueComponent(@Nullable Operation op, double value, TooltipFlag flag) {
        if (op == null) {
            return Component.translatable("attributeslib.value.boolean." + (value > 0 ? "enabled" : "disabled"));
        }
        else if (op == Operation.ADDITION && (int) value == 1) {
            return Component.translatable("attributeslib.value.boolean.enable");
        }
        else if (op == Operation.MULTIPLY_TOTAL && (int) value == -1) {
            return Component.translatable("attributeslib.value.boolean.force_disable");
        }
        else return Component.translatable("attributeslib.value.boolean.invalid");
    }

    @Override
    public MutableComponent toComponent(AttributeModifier modif, TooltipFlag flag) {
        Attribute attr = this.ths();
        double value = modif.getAmount();

        MutableComponent comp;

        if (value > 0.0D) {
            comp = Component.translatable("attributeslib.modifier.bool", this.toValueComponent(modif.getOperation(), value, flag), Component.translatable(attr.getDescriptionId())).withStyle(ChatFormatting.BLUE);
        }
        else {
            value *= -1.0D;
            comp = Component.translatable("attributeslib.modifier.bool", this.toValueComponent(modif.getOperation(), value, flag), Component.translatable(attr.getDescriptionId())).withStyle(ChatFormatting.RED);
        }

        return comp.append(this.getDebugInfo(modif, flag));
    }

    @Override
    public double sanitizeValue(double value) {
        return Math.max(value, 0);
    }

}
