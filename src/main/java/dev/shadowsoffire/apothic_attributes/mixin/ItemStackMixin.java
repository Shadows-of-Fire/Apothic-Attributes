package dev.shadowsoffire.apothic_attributes.mixin;

import java.util.List;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

@Mixin(value = ItemStack.class, remap = false)
public class ItemStackMixin {

    // Injects just before ItemStack.TooltipPart.MODIFIERS is written to the tooltip to remember where to rewind to.
    @Inject(method = "getTooltipLines(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;", at = @At(value = "INVOKE", ordinal = 4, target = "net/minecraft/world/item/ItemStack.shouldShowInTooltip(ILnet/minecraft/world/item/ItemStack$TooltipPart;)Z"), locals = LocalCapture.CAPTURE_FAILHARD, require = 1)
    public void apoth_tooltipMarker(@Nullable Player pPlayer, TooltipFlag pIsAdvanced, CallbackInfoReturnable<List<Component>> cir, List<Component> list) {
        list.add(Component.literal("APOTH_REMOVE_MARKER"));
    }

    // Injects just after ItemStack.TooltipPart.MODIFIERS is written to the tooltip to remember where to rewind to.
    @Inject(method = "getTooltipLines(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;", at = @At(value = "INVOKE", ordinal = 1, target = "net/minecraft/world/item/ItemStack.hasTag()Z"), locals = LocalCapture.CAPTURE_FAILHARD, require = 1)
    public void apoth_tooltipMarker2(@Nullable Player pPlayer, TooltipFlag pIsAdvanced, CallbackInfoReturnable<List<Component>> cir, List<Component> list) {
        list.add(Component.literal("APOTH_REMOVE_MARKER_2"));
    }
}
