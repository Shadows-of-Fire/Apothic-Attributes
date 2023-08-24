package dev.shadowsoffire.attributeslib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.shadowsoffire.attributeslib.api.ALObjects.Attributes;
import dev.shadowsoffire.attributeslib.impl.AttributeEvents;
import dev.shadowsoffire.attributeslib.util.IEntityOwned;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

@Mixin(GameType.class)
public class GameTypeMixin {

    /**
     * Responsible for applying the creative flight attribute modifier to creative players whenever the game type is updated.<br>
     * Supports {@link Attributes#CREATIVE_FLIGHT}.
     */
    @Inject(at = @At("TAIL"), method = "updatePlayerAbilities(Lnet/minecraft/world/entity/player/Abilities;)V")
    public void apoth_updatePlayerAttribute(Abilities abilities, CallbackInfo ci) {
        AttributeEvents.applyCreativeFlightModifier((Player) ((IEntityOwned) abilities).getOwner(), (GameType) (Object) this);
    }

}
