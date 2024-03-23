package dev.shadowsoffire.apothic_attributes.client;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public class AttributeModifierComponent implements ClientTooltipComponent {

    public static final ResourceLocation TEXTURE = ApothicAttributes.loc("textures/gui/attribute_component.png");

    @Nullable
    private final ModifierSource<?> source;
    private final List<FormattedCharSequence> text;

    public AttributeModifierComponent(@Nullable ModifierSource<?> source, FormattedText text, Font font, int maxWidth) {
        this.source = source;
        this.text = font.split(text, maxWidth);
    }

    @Override
    public int getHeight() {
        return this.text.size() * 10;
    }

    @Override
    public int getWidth(Font font) {
        return this.text.stream().map(font::width).map(w -> w + 12).max(Integer::compareTo).get();
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics gfx) {
        gfx.blit(TEXTURE, x, y, 0, this.source == null ? 9 : 0, 0, 9, 9, 18, 9);
        if (this.source == null) return;
        this.source.render(gfx, font, x, y);
    }

    @Override
    public void renderText(Font font, int pX, int pY, Matrix4f pMatrix4f, BufferSource pBufferSource) {
        var line = this.text.get(0);
        font.drawInBatch(line, pX + 12, pY, -1, true, pMatrix4f, pBufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
        for (int i = 1; i < this.text.size(); i++) {
            line = this.text.get(i);
            font.drawInBatch(line, pX, pY + i * (font.lineHeight + 1), -1, true, pMatrix4f, pBufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
        }
    }

}
