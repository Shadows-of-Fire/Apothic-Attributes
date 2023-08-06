package dev.shadowsoffire.attributeslib.mixin;

import org.spongepowered.asm.mixin.Mixin;

import dev.shadowsoffire.attributeslib.api.IFormattableAttribute;
import net.minecraft.world.entity.ai.attributes.Attribute;

@Mixin(Attribute.class)
public class AttributeMixin implements IFormattableAttribute {

}
