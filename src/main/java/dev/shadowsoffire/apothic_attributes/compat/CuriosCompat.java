package dev.shadowsoffire.apothic_attributes.compat;

import java.util.Map;
import java.util.function.BiConsumer;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;

import dev.shadowsoffire.apothic_attributes.client.ModifierSource;
import dev.shadowsoffire.apothic_attributes.client.ModifierSource.ItemModifierSource;
import dev.shadowsoffire.apothic_attributes.client.ModifierSourceType;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiers;
import dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiersEvent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public class CuriosCompat {

    public static final BiMap<String, EntitySlotGroup> CURIOS_TO_APOTH = HashBiMap.create();

    static {
        if (!ModList.get().isLoaded("curios")) {
            throw new UnsupportedOperationException("This optional compat class requires Curios to be loaded.");
        }
    }

    public static void init() {
        ModifierSourceType.register(new ModifierSourceType<>(){

            @Override
            public void extract(LivingEntity entity, BiConsumer<AttributeModifier, ModifierSource<?>> map) {
                CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
                    Map<String, ICurioStacksHandler> curios = handler.getCurios();
                    for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
                        ICurioStacksHandler stacksHandler = entry.getValue();
                        String identifier = entry.getKey();
                        IDynamicStackHandler stackHandler = stacksHandler.getStacks();

                        for (int i = 0; i < stacksHandler.getSlots(); i++) {
                            SlotContext slotContext = new SlotContext(identifier, entity, i, false, true);
                            ItemStack stack = stackHandler.getStackInSlot(i);
                            if (!stack.isEmpty()) {
                                ResourceLocation id = CuriosApi.getSlotId(slotContext);
                                Multimap<Holder<Attribute>, AttributeModifier> modifiers = CuriosApi.getAttributeModifiers(slotContext, id, stack);
                                ModifierSource<?> src = new ItemModifierSource(stack);
                                modifiers.values().forEach(m -> map.accept(m, src));
                            }
                        }
                    }
                });
            }

            @Override
            public int getPriority() {
                return 20;
            }
        });

        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, CuriosCompat::stackAttrModifierCompat);
    }

    public static void stackAttrModifierCompat(CurioAttributeModifierEvent e) {
        EntitySlotGroup slotGroup = CURIOS_TO_APOTH.get(e.getSlotContext().identifier());
        if (slotGroup != null) {
            var builder = StackAttributeModifiers.builder();
            e.getModifiers().forEach((attr, modif) -> {
                builder.add(attr, modif, slotGroup);
            });

            var event = new StackAttributeModifiersEvent(e.getItemStack(), builder.build());
            NeoForge.EVENT_BUS.post(event);

            if (event.hasChanges()) {
                e.clearModifiers();
                StackAttributeModifiers newModifs = event.build();

                newModifs.forEach(slotGroup, (attr, modif) -> {
                    e.addModifier(attr, modif);
                });
            }
        }
    }
}
