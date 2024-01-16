package dev.shadowsoffire.attributeslib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.client.gui.CuriosScreen;

import static dev.shadowsoffire.attributeslib.client.AttributesGui.TEXTURES;
import static dev.shadowsoffire.attributeslib.client.AttributesGui.WIDTH;

public class CuriosClientCompat {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void addAttribComponent(ScreenEvent.Init.Post e) {
        if (e.getScreen() instanceof CuriosScreen scn) {
            ImageButton button = new ImageButton(scn.getGuiLeft() + 63, scn.getGuiTop() + 10, 10, 10, WIDTH, 0, 10, TEXTURES, 256, 256, btn -> {
                if (Minecraft.getInstance().player == null) return;
                InventoryScreen invScn = new InventoryScreen(Minecraft.getInstance().player);
                AttributesGui.swappedFromCurios = true;
                scn.getMinecraft().setScreen(invScn);
            }, Component.translatable("attributeslib.gui.show_attributes"));
            e.addListener(button);
        }
    }
}
