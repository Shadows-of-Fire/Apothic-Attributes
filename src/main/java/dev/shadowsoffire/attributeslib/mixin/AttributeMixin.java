package dev.shadowsoffire.attributeslib.mixin;

import org.spongepowered.asm.mixin.Mixin;

import dev.shadowsoffire.attributeslib.api.IFormattableAttribute;
import net.minecraft.world.entity.ai.attributes.Attribute;

@Mixin(value = Attribute.class, remap = false)
public class AttributeMixin implements IFormattableAttribute {

}
