package dev.shadowsoffire.apothic_attributes.client;

import dev.shadowsoffire.placebo.util.Offset;
import dev.shadowsoffire.placebo.util.Offset.Box;
import net.minecraft.client.gui.components.ImageButton;

public class ButtonPlacement {

    public static final Box PLAYER_SIZE = new Box(51, 72);
    public static final Box BUTTON_SIZE = new Box(10, 10);

    public static final int PLAYER_LEFT_OFFSET = 25;
    public static final int PLAYER_TOP_OFFSET = 7;

    public static void positionGuiButton(ImageButton button, Offset offset, int left, int top) {
        int x = left + PLAYER_LEFT_OFFSET + offset.getX(PLAYER_SIZE, BUTTON_SIZE);
        int y = top + PLAYER_TOP_OFFSET + offset.getY(PLAYER_SIZE, BUTTON_SIZE);
        button.setPosition(x, y);
    }

}
