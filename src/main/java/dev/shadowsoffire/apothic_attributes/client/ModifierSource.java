package dev.shadowsoffire.apothic_attributes.client;

import java.util.Comparator;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.shadowsoffire.apothic_attributes.util.Comparators;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
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
     * 
     * @param gfx
     * @param font
     * @param x
     * @param y
     */
    public abstract void render(GuiGraphics gfx, Font font, int x, int y);

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
            super(ModifierSourceType.EQUIPMENT, Comparator.comparing(LivingEntity::getEquipmentSlotForItem).reversed().thenComparing(Comparator.comparing(ItemStack::getItem, Comparators.idComparator(BuiltInRegistries.ITEM))), data);
        }

        @Override
        public void render(GuiGraphics gfx, Font font, int x, int y) {
            PoseStack pose = gfx.pose();
            pose.pushPose();
            float scale = 0.5F;
            pose.scale(scale, scale, 1);
            pose.translate(1 + x / scale, 1 + y / scale, 0);
            gfx.renderFakeItem(this.data, 0, 0);
            pose.popPose();
        }

    }

    public static class EffectModifierSource extends ModifierSource<MobEffectInstance> {

        @SuppressWarnings("deprecation")
        public EffectModifierSource(MobEffectInstance data) {
            super(ModifierSourceType.MOB_EFFECT, Comparator.comparing(MobEffectInstance::getEffect, Comparators.idComparator(BuiltInRegistries.MOB_EFFECT)), data);
        }

        @Override
        public void render(GuiGraphics gfx, Font font, int x, int y) {
            MobEffectTextureManager texMgr = Minecraft.getInstance().getMobEffectTextures();
            // We don't have an EffectRenderingInventoryScreen, so we'll just hope the texture is good enough.
            // var renderer = net.minecraftforge.client.extensions.common.IClientMobEffectExtensions.of(inst);
            // if (renderer.renderInventoryIcon(inst, this, pPoseStack, pRenderX + (p_194013_ ? 6 : 7), i, this.getBlitOffset())) {
            // i += pYOffset;
            // continue;
            // }
            MobEffect effect = this.data.getEffect();
                TextureAtlasSprite sprite = texMgr.get(effect);
                float scale = 0.5F;
                PoseStack stack = gfx.pose();
                stack.pushPose();
                stack.scale(scale, scale, 1);
                stack.translate(x / scale, y / scale, 0);
                gfx.blit(0, 0, 0, 18, 18, sprite);
                stack.popPose();
        }

    }
}
