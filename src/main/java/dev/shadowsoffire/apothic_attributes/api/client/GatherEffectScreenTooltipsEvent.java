package dev.shadowsoffire.apothic_attributes.api.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.Event;

/**
 * This event is called when a {@link EffectRenderingInventoryScreen} draws the tooltip lines for a hovered {@link MobEffectInstance}.<br>
 * It can be used to modify the tooltip.
 * <p>
 * This event is only fired on the {@linkplain Dist#CLIENT physical client}.
 */
public class GatherEffectScreenTooltipsEvent extends Event {

    protected final EffectRenderingInventoryScreen<?> screen;
    protected final MobEffectInstance effectInst;
    protected final List<Component> tooltip;

    public GatherEffectScreenTooltipsEvent(EffectRenderingInventoryScreen<?> screen, MobEffectInstance effectInst, List<Component> tooltip) {
        this.screen = screen;
        this.effectInst = effectInst;
        this.tooltip = new ArrayList<>(tooltip);
    }

    /**
     * @return The screen which will be rendering the tooltip lines.
     */
    public EffectRenderingInventoryScreen<?> getScreen() {
        return this.screen;
    }

    /**
     * @return The effect whose tooltip is being drawn.
     */
    public MobEffectInstance getEffectInstance() {
        return this.effectInst;
    }

    /**
     * @return A mutable list of tooltip lines.
     */
    public List<Component> getTooltip() {
        return this.tooltip;
    }

}
