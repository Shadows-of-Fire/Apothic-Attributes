package dev.shadowsoffire.apothic_attributes.client;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.shadowsoffire.apothic_attributes.ALConfig;
import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.placebo.PlaceboClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.BooleanAttribute;
import net.neoforged.neoforge.common.extensions.IAttributeExtension;

public class AttributesGui implements Renderable, GuiEventListener {

    public static final ResourceLocation TEXTURES = ApothicAttributes.loc("textures/gui/attributes_gui.png");
    public static final WidgetSprites SWORD_BUTTON_SPRITES = new WidgetSprites(ApothicAttributes.loc("sword"), ApothicAttributes.loc("sword_highlighted"));
    public static final int ENTRY_HEIGHT = 22;
    public static final int MAX_ENTRIES = 6;
    public static final int WIDTH = 131;

    // There's only one player, so we can just happily track if this menu was open via static field.
    // It isn't persistent through sessions, but that's not a huge issue.
    public static boolean wasOpen = false;
    // Similar to the above, we use a static field to record where the scroll bar was.
    protected static float scrollOffset = 0;
    // Ditto.
    protected static boolean hideUnchanged = false;
    protected static boolean swappedFromCurios = false;

    protected final InventoryScreen parent;
    protected final Player player;
    protected final Font font = Minecraft.getInstance().font;
    protected final ImageButton toggleBtn;
    protected final ImageButton recipeBookButton;
    protected final HideUnchangedButton hideUnchangedBtn;

    protected int leftPos, topPos;
    protected boolean scrolling;
    protected int startIndex;
    protected List<AttributeInstance> data = new ArrayList<>();
    @Nullable
    protected AttributeInstance selected = null;
    protected boolean open = false;
    protected long lastRenderTick = -1;

    public AttributesGui(InventoryScreen parent) {
        this.parent = parent;
        this.player = Minecraft.getInstance().player;
        this.refreshData();
        this.leftPos = parent.getGuiLeft() - WIDTH;
        this.topPos = parent.getGuiTop();
        this.toggleBtn = new ImageButton(0, 0, 10, 10, SWORD_BUTTON_SPRITES, btn -> {
            this.toggleVisibility();
        }, Component.translatable("apothic_attributes.gui.show_attributes")){
            @Override
            public void setFocused(boolean pFocused) {}
        };
        if (this.parent.children().size() > 1) {
            GuiEventListener btn = this.parent.children().get(0);
            this.recipeBookButton = btn instanceof ImageButton imgBtn ? imgBtn : null;
        }
        else this.recipeBookButton = null;
        this.hideUnchangedBtn = new HideUnchangedButton(0, 0);
        ButtonPlacement.positionGuiButton(toggleBtn, ALConfig.attributesGuiButtonOffset, parent.getGuiLeft(), parent.getGuiTop());
    }

    @SuppressWarnings("deprecation")
    public void refreshData() {
        this.data.clear();
        BuiltInRegistries.ATTRIBUTE.holders()
            .map(this.player::getAttribute)
            .filter(Objects::nonNull)
            .filter(ai -> !ALConfig.hiddenAttributes.contains(ai.getAttribute().unwrapKey().get().location()))
            .filter(ai -> !hideUnchanged || (ai.getBaseValue() != ai.getValue()))
            .forEach(this.data::add);
        this.data.sort(this::compareAttrs);
        this.startIndex = (int) (scrollOffset * this.getOffScreenRows() + 0.5D);
    }

    public void toggleVisibility() {
        this.open = !this.open;
        if (this.open && this.parent.getRecipeBookComponent().isVisible()) {
            this.parent.getRecipeBookComponent().toggleVisibility();
        }
        this.hideUnchangedBtn.visible = this.open;

        int newLeftPos;
        if (this.open && this.parent.width >= 379) {
            newLeftPos = 177 + (this.parent.width - this.parent.imageWidth - 200) / 2;
        }
        else {
            newLeftPos = (this.parent.width - this.parent.imageWidth) / 2;
        }

        this.parent.leftPos = newLeftPos;
        this.leftPos = this.parent.getGuiLeft() - WIDTH;
        this.topPos = this.parent.getGuiTop();

        if (this.recipeBookButton != null) this.recipeBookButton.setPosition(this.parent.getGuiLeft() + 104, this.parent.height / 2 - 22);
        this.hideUnchangedBtn.setPosition(this.leftPos + 7, this.topPos + 151);
    }

    protected int compareAttrs(AttributeInstance a1, AttributeInstance a2) {
        String name = I18n.get(a1.getAttribute().value().getDescriptionId());
        String name2 = I18n.get(a2.getAttribute().value().getDescriptionId());
        return name.compareTo(name2);
    }

    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        if (!this.open) return false;
        return this.isHovering(0, 0, WIDTH, 166, pMouseX, pMouseY);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        ButtonPlacement.positionGuiButton(this.toggleBtn, ALConfig.attributesGuiButtonOffset, this.parent.getGuiLeft(), this.parent.getGuiTop());
        if (this.parent.getRecipeBookComponent().isVisible()) this.open = false;
        wasOpen = this.open;
        if (!this.open) return;

        if (this.lastRenderTick != PlaceboClient.ticks) {
            this.lastRenderTick = PlaceboClient.ticks;
            this.refreshData();
        }

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURES);
        int left = this.leftPos;
        int top = this.topPos;
        gfx.blit(TEXTURES, left, top, 0, 0, WIDTH, 166);
        int scrollbarPos = (int) (117 * scrollOffset);
        gfx.blit(TEXTURES, left + 111, top + 16 + scrollbarPos, 244, this.isScrollBarActive() ? 0 : 15, 12, 15);
        int idx = this.startIndex;
        while (idx < this.startIndex + MAX_ENTRIES && idx < this.data.size()) {
            this.renderEntry(gfx, this.data.get(idx), this.leftPos + 8, this.topPos + 16 + ENTRY_HEIGHT * (idx - this.startIndex), mouseX, mouseY);
            idx++;
        }
        this.renderTooltip(gfx, mouseX, mouseY);
        gfx.drawString(font, Component.translatable("apothic_attributes.gui.attributes"), this.leftPos + 8, this.topPos + 5, 0x404040, false);
        gfx.drawString(font, ApothicAttributes.lang("text", "hide_unchanged"), this.leftPos + 20, this.topPos + 152, 0x404040, false);
    }

    @SuppressWarnings("deprecation")
    protected void renderTooltip(GuiGraphics gfx, int mouseX, int mouseY) {
        AttributeInstance inst = this.getHoveredSlot(mouseX, mouseY);
        if (inst != null) {
            Attribute attr = inst.getAttribute().value();
            boolean isDynamic = inst.getAttribute().is(ALObjects.Tags.DYNAMIC_BASE_ATTRIBUTES);

            List<Component> list = new ArrayList<>();
            MutableComponent name = Component.translatable(attr.getDescriptionId()).withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withUnderlined(true));

            if (isDynamic) {
                name.append(CommonComponents.SPACE);
                name.append(Component.translatable("apothic_attributes.gui.dynamic").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withUnderlined(false)));
            }

            if (ApothicAttributes.getTooltipFlag().isAdvanced()) {
                Style style = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withUnderlined(false);
                name.append(Component.literal(" [" + BuiltInRegistries.ATTRIBUTE.getKey(attr) + "]").withStyle(style));
            }

            list.add(name);

            String key = attr.getDescriptionId() + ".desc";

            if (I18n.exists(key)) {
                Component txt = Component.translatable(key).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC);
                list.add(txt);
            }
            else if (ApothicAttributes.getTooltipFlag().isAdvanced()) {
                Component txt = Component.literal(key).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
                list.add(txt);
            }

            int color = getValueColor(inst, ChatFormatting.GRAY.getColor());

            Component valueComp = attr.toValueComponent(null, inst.getValue(), ApothicAttributes.getTooltipFlag()).withColor(color);
            Component baseComp = attr.toValueComponent(null, inst.getBaseValue(), ApothicAttributes.getTooltipFlag()).withStyle(ChatFormatting.GRAY);

            if (!isDynamic) {
                list.add(CommonComponents.EMPTY);
                list.add(Component.translatable("apothic_attributes.gui.current", valueComp).withStyle(ChatFormatting.GRAY));

                Component base = Component.translatable("apothic_attributes.gui.base", baseComp).withStyle(ChatFormatting.GRAY);

                if (attr instanceof RangedAttribute ra) {
                    Component min = attr.toValueComponent(null, ra.getMinValue(), ApothicAttributes.getTooltipFlag());
                    min = Component.translatable("apothic_attributes.gui.min", min);
                    Component max = attr.toValueComponent(null, ra.getMaxValue(), ApothicAttributes.getTooltipFlag());
                    max = Component.translatable("apothic_attributes.gui.max", max);
                    list.add(Component.translatable("%s \u2507 %s \u2507 %s", base, min, max).withStyle(ChatFormatting.GRAY));
                }
                else {
                    list.add(base);
                }
            }

            List<ClientTooltipComponent> finalTooltip = new ArrayList<>(list.size());
            for (Component txt : list) {
                this.addComp(txt, finalTooltip);
            }

            if (inst.getModifiers().stream().anyMatch(modif -> modif.amount() != 0)) {
                this.addComp(CommonComponents.EMPTY, finalTooltip);
                this.addComp(Component.translatable("apothic_attributes.gui.modifiers").withStyle(ChatFormatting.GOLD), finalTooltip);

                Map<ResourceLocation, ModifierSource<?>> modifiersToSources = new HashMap<>();

                for (ModifierSourceType<?> type : ModifierSourceType.getTypes()) {
                    type.extract(this.player, (modif, source) -> modifiersToSources.put(modif.id(), source));
                }

                MutableComponent[] opValues = new MutableComponent[3];
                double[] numericValues = new double[3];

                for (Operation op : Operation.values()) {
                    double baseValue = op == Operation.ADD_MULTIPLIED_TOTAL ? 1 : 0;
                    List<AttributeModifier> modifiers = new ArrayList<>(inst.getModifiers(op).values());
                    double opValue = modifiers.stream().mapToDouble(AttributeModifier::amount).reduce(baseValue, (res, elem) -> op == Operation.ADD_MULTIPLIED_TOTAL ? res * (1 + elem) : res + elem);

                    modifiers.sort(ModifierSourceType.compareBySource(modifiersToSources));
                    for (AttributeModifier modif : modifiers) {
                        if (modif.amount() != 0) {
                            Component comp = attr.toComponent(modif, ApothicAttributes.getTooltipFlag());
                            var src = modifiersToSources.get(modif.id());
                            finalTooltip.add(new AttributeModifierComponent(src, comp, this.font, this.leftPos - 16));
                        }
                    }

                    color = getValueColor(attr, opValue, baseValue, ChatFormatting.GRAY.getColor());
                    Component valueComp2 = attr.toValueComponent(op, opValue, ApothicAttributes.getTooltipFlag()).withStyle(Style.EMPTY.withColor(color));
                    MutableComponent comp = Component.translatable("apothic_attributes.gui." + op.name().toLowerCase(Locale.ROOT), valueComp2).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);

                    opValues[op.ordinal()] = comp;
                    numericValues[op.ordinal()] = opValue;
                }

                this.addComp(CommonComponents.EMPTY, finalTooltip);
                this.addComp(Component.translatable("apothic_attributes.gui.formula").withStyle(ChatFormatting.GOLD), finalTooltip);

                Component base = isDynamic ? Component.translatable("apothic_attributes.gui.formula.base") : baseComp;
                Component value = isDynamic ? Component.translatable("apothic_attributes.gui.formula.value") : valueComp;

                Component formula = buildFormula(base, value, numericValues, attr);
                this.addComp(formula, finalTooltip);
            }
            else if (isDynamic) {
                this.addComp(CommonComponents.EMPTY, finalTooltip);
                this.addComp(Component.translatable("apothic_attributes.gui.no_modifiers").withStyle(ChatFormatting.GOLD), finalTooltip);
            }

            gfx.renderTooltipInternal(font, finalTooltip, this.leftPos - 16 - finalTooltip.stream().map(c -> c.getWidth(this.font)).max(Integer::compare).get(), mouseY, DefaultTooltipPositioner.INSTANCE);
        }
    }

    private void addComp(Component comp, List<ClientTooltipComponent> finalTooltip) {
        if (comp == CommonComponents.EMPTY) {
            finalTooltip.add(ClientTooltipComponent.create(comp.getVisualOrderText()));
        }
        else {
            for (FormattedText fTxt : this.font.getSplitter().splitLines(comp, this.leftPos - 16, comp.getStyle())) {
                finalTooltip.add(ClientTooltipComponent.create(Language.getInstance().getVisualOrder(fTxt)));
            }
        }
    }

    private void renderEntry(GuiGraphics gfx, AttributeInstance inst, int x, int y, int mouseX, int mouseY) {
        boolean hover = this.getHoveredSlot(mouseX, mouseY) == inst;
        gfx.blit(TEXTURES, x, y, 142, hover ? ENTRY_HEIGHT : 0, 100, ENTRY_HEIGHT);

        Component txt = Component.translatable(inst.getAttribute().value().getDescriptionId());
        int splitWidth = 60;
        List<FormattedCharSequence> lines = this.font.split(txt, splitWidth);
        // We can only actually display two lines here, but we need to forcibly create two lines and then scale down.
        while (lines.size() > 2) {
            splitWidth += 10;
            lines = this.font.split(txt, splitWidth);
        }

        PoseStack stack = gfx.pose();

        stack.pushPose();
        float scale = 1;
        int maxWidth = lines.stream().map(this.font::width).max(Integer::compareTo).get();
        if (maxWidth > 66) {
            scale = 66F / maxWidth;
            stack.scale(scale, scale, 1);
        }

        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            float width = this.font.width(line) * scale;
            float lineX = (x + 1 + (68 - width) / 2) / scale;
            float lineY = (y + (lines.size() == 1 ? 7 : 2) + i * 10) / scale;
            gfx.drawString(font, line, lineX, lineY, 0x404040, false);
        }
        stack.popPose();
        stack.pushPose();

        MutableComponent value = inst.getAttribute().value().toValueComponent(null, inst.getValue(), TooltipFlag.Default.NORMAL);

        if (inst.getAttribute().is(ALObjects.Tags.DYNAMIC_BASE_ATTRIBUTES)) {
            value = Component.literal("\uFFFD");
        }

        scale = 1;
        if (this.font.width(value) > 27) {
            scale = 27F / this.font.width(value);
            stack.scale(scale, scale, 1);
        }

        int color = getValueColor(inst, ChatFormatting.WHITE.getColor());
        gfx.drawString(font, value, (int) ((x + 72 + (27 - this.font.width(value) * scale) / 2) / scale), (int) ((y + 7) / scale), color, true);
        stack.popPose();
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (!this.open || !this.isScrollBarActive()) return false;
        this.scrolling = false;
        int left = this.leftPos + 111;
        int top = this.topPos + 15;
        if (pMouseX >= left && pMouseX < left + 12 && pMouseY >= top && pMouseY < top + 155) {
            this.scrolling = true;
            int i = this.topPos + 15;
            int j = i + 138;
            scrollOffset = ((float) pMouseY - i - 7.5F) / (j - i - 15.0F);
            scrollOffset = Mth.clamp(scrollOffset, 0.0F, 1.0F);
            this.startIndex = (int) (scrollOffset * this.getOffScreenRows() + 0.5D);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (!this.open) return false;
        if (this.scrolling && this.isScrollBarActive()) {
            int i = this.topPos + 15;
            int j = i + 138;
            scrollOffset = ((float) pMouseY - i - 7.5F) / (j - i - 15.0F);
            scrollOffset = Mth.clamp(scrollOffset, 0.0F, 1.0F);
            this.startIndex = (int) (scrollOffset * this.getOffScreenRows() + 0.5D);
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        if (!this.open) return false;
        if (this.isScrollBarActive()) {
            int i = this.getOffScreenRows();
            scrollOffset = (float) (scrollOffset - pScrollY / i);
            scrollOffset = Mth.clamp(scrollOffset, 0.0F, 1.0F);
            this.startIndex = (int) (scrollOffset * i + 0.5D);
            return true;
        }
        return false;
    }

    private boolean isScrollBarActive() {
        return this.data.size() > MAX_ENTRIES;
    }

    protected int getOffScreenRows() {
        return Math.max(0, this.data.size() - MAX_ENTRIES);
    }

    @Nullable
    public AttributeInstance getHoveredSlot(int mouseX, int mouseY) {
        for (int i = 0; i < MAX_ENTRIES; i++) {
            if (this.startIndex + i < this.data.size()) {
                if (this.isHovering(8, 14 + ENTRY_HEIGHT * i, 100, ENTRY_HEIGHT, mouseX, mouseY)) return this.data.get(this.startIndex + i);
            }
        }
        return null;
    }

    protected boolean isHovering(int pX, int pY, int pWidth, int pHeight, double pMouseX, double pMouseY) {
        int i = this.leftPos;
        int j = this.topPos;
        pMouseX -= i;
        pMouseY -= j;
        return pMouseX >= pX - 1 && pMouseX < pX + pWidth + 1 && pMouseY >= pY - 1 && pMouseY < pY + pHeight + 1;
    }

    private static DecimalFormat f = IAttributeExtension.FORMAT;

    public static String format(int n) {
        int log = (int) StrictMath.log10(n);
        if (log <= 4) return String.valueOf(n);
        if (log == 5) return f.format(n / 1000D) + "K";
        if (log <= 8) return f.format(n / 1000000D) + "M";
        else return f.format(n / 1000000000D) + "B";
    }

    public static int getValueColor(AttributeInstance inst, int fallbackColor) {
        return getValueColor(inst.getAttribute().value(), inst.getValue(), inst.getBaseValue(), fallbackColor);
    }

    public static int getValueColor(Attribute attr, double value, double base, int fallbackColor) {
        if (value == base) {
            return fallbackColor;
        }

        if (attr instanceof RangedAttribute) {
            boolean isPositive = value > base;
            return translateColor(attr.getStyle(isPositive));
        }
        else if (attr instanceof BooleanAttribute) {
            boolean isPositive = value > 0;
            return translateColor(attr.getStyle(isPositive));
        }

        return fallbackColor;
    }

    /**
     * Translates the {@link ChatFormatting} values used by {@link Attribute#getStyle(boolean)} to colors better visible in the Attributes GUI.
     */
    private static int translateColor(ChatFormatting color) {
        return switch (color) {
            case BLUE -> 0x55DD55;
            case RED -> 0xFF6060;
            case GRAY -> 0xFFFFFF;
            default -> color.getColor();
        };
    }

    /**
     * Builds a component containing the mathematical representation of the attribute calculations.
     * 
     * @param base          A component of the base value. May be a string if the attribute is dynamic.
     * @param value         A component of the final value. May be a string if the attribute is dynamic.
     * @param numericValues The modifier totals, in operation ordinal order (add, mulBase, mulTotal)
     * @return A component holding the formula with colors already applied.
     */
    public static Component buildFormula(Component base, Component value, double[] numericValues, Attribute attr) {
        double add = numericValues[0];
        double mulBase = numericValues[1];
        double mulTotal = numericValues[2];

        boolean isAddNeg = add < 0;
        boolean isMulNeg = mulBase < 0;

        String addSym = isAddNeg ? "-" : "+";
        add = Math.abs(add);

        String mulBaseSym = isMulNeg ? "-" : "+";
        mulBase = Math.abs(mulBase);

        String addStr = f.format(add);
        String mulBaseStr = f.format(mulBase);
        String mulTotalStr = f.format(mulTotal);

        String formula = "%2$s";

        if (add != 0) {
            ChatFormatting color = getColor(attr, isAddNeg);
            formula = formula + " " + colored(addSym + " " + addStr, color);
        }

        if (mulBase != 0) {
            String withParens = add == 0 ? formula : "(%s)".formatted(formula);
            ChatFormatting color = getColor(attr, isMulNeg);
            formula = withParens + " " + colored(mulBaseSym + " " + mulBaseStr + " * ", color) + withParens;
        }

        if (mulTotal != 1) {
            String withParens = add == 0 && mulBase == 0 ? formula : "(%s)".formatted(formula);
            ChatFormatting color = getColor(attr, mulTotal < 1);
            formula = colored(mulTotalStr + " * ", color) + withParens;
        }

        return Component.translatable("%1$s = " + formula, value, base).withStyle(ChatFormatting.GRAY);
    }

    /**
     * Extracts the color from the sentiment, translating blue to yellow in the process.
     */
    private static ChatFormatting getColor(Attribute attr, boolean isNegative) {
        ChatFormatting color = attr.getStyle(!isNegative);
        return color == ChatFormatting.BLUE ? ChatFormatting.YELLOW : color;
    }

    /**
     * Colors a string using legacy formatting codes. Terminates the string with {@link ChatFormatting#RESET}.
     */
    private static String colored(String str, ChatFormatting color) {
        return "" + ChatFormatting.PREFIX_CODE + color.getChar() + str + ChatFormatting.PREFIX_CODE + ChatFormatting.RESET.getChar();
    }

    public class HideUnchangedButton extends AbstractButton {

        public HideUnchangedButton(int pX, int pY) {
            super(pX, pY, 10, 10, ApothicAttributes.lang("button", "hide_unchanged"));
            this.visible = false;
        }

        @Override
        public void onPress() {
            hideUnchanged = !hideUnchanged;
        }

        @Override
        public void renderWidget(GuiGraphics gfx, int pMouseX, int pMouseY, float pPartialTick) {
            int u = 131, v = 20;
            int vOffset = hideUnchanged ? 0 : 10;
            if (this.isHovered) {
                vOffset += 20;
            }

            RenderSystem.enableDepthTest();
            PoseStack pose = gfx.pose();
            pose.pushPose();
            pose.translate(0, 0, 100);
            gfx.blit(TEXTURES, this.getX(), this.getY(), u, v + vOffset, 10, 10, 256, 256);
            pose.popPose();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
            this.defaultButtonNarrationText(pNarrationElementOutput);
        }

    }

    @Override
    public void setFocused(boolean pFocused) {}

    @Override
    public boolean isFocused() {
        return false;
    }

}
