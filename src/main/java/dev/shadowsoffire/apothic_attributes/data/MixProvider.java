package dev.shadowsoffire.apothic_attributes.data;

import java.util.concurrent.CompletableFuture;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.placebo.systems.mixes.JsonMix;
import dev.shadowsoffire.placebo.systems.mixes.MixRegistry;
import dev.shadowsoffire.placebo.util.data.DynamicRegistryProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;

public class MixProvider extends DynamicRegistryProvider<JsonMix<?>> {

    public MixProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, MixRegistry.INSTANCE);
    }

    @Override
    public String getName() {
        return "Apothic Attributes Brewing Mixes";
    }

    @Override
    public void generate() {
        addMix(Potions.AWKWARD, Items.SHULKER_SHELL, ALObjects.Potions.RESISTANCE);
        addMix(ALObjects.Potions.RESISTANCE, Items.REDSTONE, ALObjects.Potions.LONG_RESISTANCE);
        addMix(ALObjects.Potions.RESISTANCE, Items.GLOWSTONE_DUST, ALObjects.Potions.STRONG_RESISTANCE);

        addMix(ALObjects.Potions.RESISTANCE, Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.SUNDERING);
        addMix(ALObjects.Potions.LONG_RESISTANCE, Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.LONG_SUNDERING);
        addMix(ALObjects.Potions.STRONG_RESISTANCE, Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.STRONG_SUNDERING);
        addMix(ALObjects.Potions.SUNDERING, Items.REDSTONE, ALObjects.Potions.LONG_SUNDERING);
        addMix(ALObjects.Potions.SUNDERING, Items.GLOWSTONE_DUST, ALObjects.Potions.STRONG_SUNDERING);

        addMix(Potions.AWKWARD, Items.GOLDEN_APPLE, ALObjects.Potions.ABSORPTION);
        addMix(ALObjects.Potions.ABSORPTION, Items.REDSTONE, ALObjects.Potions.LONG_ABSORPTION);
        addMix(ALObjects.Potions.ABSORPTION, Items.GLOWSTONE_DUST, ALObjects.Potions.STRONG_ABSORPTION);

        addMix(Potions.AWKWARD, Items.MUSHROOM_STEW, ALObjects.Potions.HASTE);
        addMix(ALObjects.Potions.HASTE, Items.REDSTONE, ALObjects.Potions.LONG_HASTE);
        addMix(ALObjects.Potions.HASTE, Items.GLOWSTONE_DUST, ALObjects.Potions.STRONG_HASTE);

        addMix(ALObjects.Potions.HASTE, Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.FATIGUE);
        addMix(ALObjects.Potions.LONG_HASTE, Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.LONG_FATIGUE);
        addMix(ALObjects.Potions.STRONG_HASTE, Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.STRONG_FATIGUE);
        addMix(ALObjects.Potions.FATIGUE, Items.REDSTONE, ALObjects.Potions.LONG_FATIGUE);
        addMix(ALObjects.Potions.FATIGUE, Items.GLOWSTONE_DUST, ALObjects.Potions.STRONG_FATIGUE);

        addMix(Potions.AWKWARD, Items.WITHER_SKELETON_SKULL, ALObjects.Potions.WITHER);
        addMix(ALObjects.Potions.WITHER, Items.REDSTONE, ALObjects.Potions.LONG_WITHER);
        addMix(ALObjects.Potions.WITHER, Items.GLOWSTONE_DUST, ALObjects.Potions.STRONG_WITHER);

        addMix(Potions.AWKWARD, Items.EXPERIENCE_BOTTLE, ALObjects.Potions.KNOWLEDGE);
        addMix(ALObjects.Potions.KNOWLEDGE, Items.REDSTONE, ALObjects.Potions.LONG_KNOWLEDGE);
        addMix(ALObjects.Potions.KNOWLEDGE, Items.EXPERIENCE_BOTTLE, ALObjects.Potions.STRONG_KNOWLEDGE);

        addMix(Potions.AWKWARD, Items.SWEET_BERRIES, ALObjects.Potions.VITALITY);
        addMix(ALObjects.Potions.VITALITY, Items.REDSTONE, ALObjects.Potions.LONG_VITALITY);
        addMix(ALObjects.Potions.VITALITY, Items.GLOWSTONE_DUST, ALObjects.Potions.STRONG_VITALITY);

        addMix(ALObjects.Potions.VITALITY, Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.GRIEVOUS);
        addMix(ALObjects.Potions.LONG_VITALITY, Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.LONG_GRIEVOUS);
        addMix(ALObjects.Potions.STRONG_VITALITY, Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.STRONG_GRIEVOUS);
        addMix(ALObjects.Potions.GRIEVOUS, Items.REDSTONE, ALObjects.Potions.LONG_GRIEVOUS);
        addMix(ALObjects.Potions.GRIEVOUS, Items.GLOWSTONE_DUST, ALObjects.Potions.STRONG_GRIEVOUS);

        addMix(Potions.SLOW_FALLING, Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.LEVITATION);
        addMix(ALObjects.Potions.LEVITATION, Items.POPPED_CHORUS_FRUIT, ALObjects.Potions.FLYING);
        addMix(ALObjects.Potions.FLYING, Items.REDSTONE, ALObjects.Potions.LONG_FLYING);
        addMix(ALObjects.Potions.LONG_FLYING, Items.REDSTONE, ALObjects.Potions.EXTRA_LONG_FLYING);
    }

    private void addMix(Holder<Potion> input, Item ingredient, Holder<Potion> output) {
        Identifier inKey = input.unwrapKey().get().identifier();
        Identifier outKey = output.unwrapKey().get().identifier();
        Identifier id = ApothicAttributes.loc(outKey.getPath() + "_from_" + inKey.getPath());
        this.add(id, new JsonMix<>(input, Ingredient.of(ingredient), output, JsonMix.Type.POTION));
    }

}
