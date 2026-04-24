package dev.shadowsoffire.apothic_attributes.client;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

public class AttributeModifierComponent implements ClientTooltipComponent {

    public static final Identifier TEXTURE = ApothicAttributes.loc("textures/gui/attribute_component.png");

    @Nullable
    private final ModifierSource<?> source;
    private final List<FormattedCharSequence> text;

    public AttributeModifierComponent(@Nullable ModifierSource<?> source, FormattedText text, Font font, int maxWidth) {
        this.source = source;
        this.text = font.split(text, maxWidth);
    }

    @Override
    public int getHeight(Font font) {
        return this.text.size() * 10;
    }

    @Override
    public int getWidth(Font font) {
        return this.text.stream().map(font::width).map(w -> w + 12).max(Integer::compareTo).get();
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor gfx) {
        gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, this.source == null ? 9 : 0, 0, 9, 9, 18, 9);
        if (this.source == null) return;
        this.source.render(gfx, font, x, y);
    }

    @Override
    public void extractText(GuiGraphicsExtractor gfx, Font font, int x, int y) {
        var line = this.text.get(0);
        gfx.text(font, line, x + 12, y, -1, true);
        for (int i = 1; i < this.text.size(); i++) {
            line = this.text.get(i);
            gfx.text(font, line, x, y + i * (font.lineHeight + 1), -1, true);
        }
    }

}
