package dev.shadowsoffire.attributeslib.client;

import dev.shadowsoffire.attributeslib.ALConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import top.theillusivec4.curios.client.gui.CuriosScreen;

public class CuriosClientCompat {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void addAttribComponent(ScreenEvent.Init.Post e) {
        if (ALConfig.enableAttributesGui && e.getScreen() instanceof CuriosScreen scn) {
            ImageButton button = new ImageButton(scn.getGuiLeft() + 63, scn.getGuiTop() + 10, 10, 10, AttributesGui.SWORD_BUTTON_SPRITES, btn -> {
                if (Minecraft.getInstance().player == null) return;
                InventoryScreen invScn = new InventoryScreen(Minecraft.getInstance().player);
                AttributesGui.swappedFromCurios = true;
                scn.getMinecraft().setScreen(invScn);
                btn.setFocused(false);
            }, Component.translatable("attributeslib.gui.show_attributes")){
                @Override
                public void setFocused(boolean pFocused) {}
            };
            e.addListener(button);
        }
    }
}
