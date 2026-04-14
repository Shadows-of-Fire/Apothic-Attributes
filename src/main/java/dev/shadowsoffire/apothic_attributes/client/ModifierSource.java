package dev.shadowsoffire.apothic_attributes.client;

import java.util.Comparator;

import dev.shadowsoffire.apothic_attributes.util.Comparators;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

/**
 * A Modifier Source is a container object around any potential Attribute Modifier Source.<br>
 * It has the code necessary to render and compare the object for display in the Attributes screen.
 */
public abstract class ModifierSource<T> implements Comparable<ModifierSource<T>> {

    protected final ModifierSourceType<T> type;
    protected final Comparator<T> comparator;
    protected final T data;

    public ModifierSource(ModifierSourceType<T> type, Comparator<T> comparator, T data) {
        this.type = type;
        this.comparator = comparator;
        this.data = data;
    }

    /**
     * Render this ModifierSource as whatever visual representation it may take.
     */
    public abstract void render(GuiGraphicsExtractor gfx, Font font, int x, int y);

    public ModifierSourceType<T> getType() {
        return this.type;
    }

    public final T getData() {
        return this.data;
    }

    @Override
    public int compareTo(ModifierSource<T> o) {
        return this.comparator.compare(this.getData(), o.getData());
    }

    public static class ItemModifierSource extends ModifierSource<ItemStack> {

        @SuppressWarnings("deprecation")
        public ItemModifierSource(ItemStack data) {
            super(ModifierSourceType.EQUIPMENT,
                Comparator.comparing(Minecraft.getInstance().player::getEquipmentSlotForItem)
                    .reversed()
                    .thenComparing(Comparator.comparing(ItemStack::getItem, Comparators.idComparator(BuiltInRegistries.ITEM))),
                data);
        }

        @Override
        public void render(GuiGraphicsExtractor gfx, Font font, int x, int y) {
            var pose = gfx.pose();
            pose.pushMatrix();
            float scale = 0.5F;
            pose.scale(scale, scale);
            pose.translate(1 + x / scale, 1 + y / scale);
            gfx.fakeItem(this.data, 0, 0);
            pose.popMatrix();
        }

    }

    public static class EffectModifierSource extends ModifierSource<MobEffectInstance> {

        @SuppressWarnings("deprecation")
        public EffectModifierSource(MobEffectInstance data) {
            super(ModifierSourceType.MOB_EFFECT, Comparator.comparing(inst -> inst.getEffect().unwrapKey().get(), ResourceKey::compareTo), data);
        }

        @Override
        public void render(GuiGraphicsExtractor gfx, Font font, int x, int y) {
            Holder<MobEffect> effect = this.data.getEffect();
            Identifier sprite = Gui.getMobEffectSprite(effect);
            float scale = 0.5F;
            var pose = gfx.pose();
            pose.pushMatrix();
            pose.scale(scale, scale);
            pose.translate(x / scale, y / scale);
            gfx.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, 0, 0, 18, 18);
            pose.popMatrix();
        }

    }
}
