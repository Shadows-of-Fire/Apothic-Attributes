package dev.shadowsoffire.attributeslib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.extensions.IEntityExtension;

@Mixin(value = IEntityExtension.class, remap = false)
public interface IEntityExtensionMixin {

    /**
     * This mixin overwrites {@link IEntityExtension#getStepHeight()} to completely phase-out {@link Entity#maxUpStep} for players.
     * <p>
     * This ensures that the attribute value accurately reflects the player's Step Height, instead of requiring additional correction.
     *
     * @author Shadows
     * @reason Ensuring that, for players, the value of the Step Height attribute matches the true step height value.
     */
    @Overwrite(remap = false)
    @SuppressWarnings("deprecation")
    default float getStepHeight() {
        float legacyStep = ((Entity) this).maxUpStep();
        if (this instanceof Player player) return (float) player.getAttributeValue(NeoForgeMod.STEP_HEIGHT.value());

        if (this instanceof LivingEntity living) {
            AttributeInstance stepHeightAttribute = living.getAttribute(NeoForgeMod.STEP_HEIGHT.value());
            if (stepHeightAttribute != null) {
                return (float) Math.max(0, legacyStep + stepHeightAttribute.getValue());
            }
        }
        return legacyStep;
    }

}
