package dev.shadowsoffire.attributeslib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.shadowsoffire.attributeslib.api.ALObjects;
import dev.shadowsoffire.attributeslib.api.ALObjects.Attributes;
import dev.shadowsoffire.attributeslib.impl.AttributeEvents;
import dev.shadowsoffire.attributeslib.util.IEntityOwned;
import dev.shadowsoffire.attributeslib.util.IFlying;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

@Mixin(GameType.class)
public class GameTypeMixin {

    private boolean apoth_flying;

    @Inject(at = @At("HEAD"), method = "updatePlayerAbilities(Lnet/minecraft/world/entity/player/Abilities;)V")
    public void apoth_recordOldFlyingAttribs(Abilities abilities, CallbackInfo ci) {
        this.apoth_flying = abilities.flying;
    }

    /**
     * Responsible for applying the creative flight attribute modifier to creative players whenever the game type is updated.<br>
     * Supports {@link Attributes#CREATIVE_FLIGHT}.
     */
    @Inject(at = @At("TAIL"), method = "updatePlayerAbilities(Lnet/minecraft/world/entity/player/Abilities;)V")
    public void apoth_flightAttribModifier(Abilities abilities, CallbackInfo ci) {
        Player player = (Player) ((IEntityOwned) abilities).getOwner();
        AttributeEvents.applyCreativeFlightModifier(player, (GameType) (Object) this);
        if (player.getAttributeValue(ALObjects.Attributes.CREATIVE_FLIGHT.get()) > 0) {
            abilities.mayfly = true;
            abilities.flying = ((IFlying) player).getAndDestroyFlyingCache() || this.apoth_flying;
        }
    }

}
