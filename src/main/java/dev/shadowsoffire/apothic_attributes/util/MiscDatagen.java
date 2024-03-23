package dev.shadowsoffire.apothic_attributes.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.serialization.Codec;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.systems.mixes.JsonMix;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;

// TODO: Implement this correctly
public class MiscDatagen implements DataProvider {

    private final Path outputDir;
    private CachedOutput cachedOutput;
    private List<CompletableFuture<?>> futures = new ArrayList<>();

    public MiscDatagen(Path outputDir) {
        this.outputDir = outputDir;
        var map = (Object2IntOpenHashMap<String>) DataProvider.FIXED_ORDER_FIELDS;
        map.put("mix_type", 0);
    }

    // Adapted from the old recipe code in Apotheosis
    public void genPotionRecipes() {
        addMix(Potions.AWKWARD, Items.SHULKER_SHELL, ALObjects.Potions.RESISTANCE.get());
        addMix(ALObjects.Potions.RESISTANCE.get(), Items.REDSTONE, ALObjects.Potions.LONG_RESISTANCE.get());
        addMix(ALObjects.Potions.RESISTANCE.get(), Items.GLOWSTONE_DUST, ALObjects.Potions.STRONG_RESISTANCE.get());

        addMix(ALObjects.Potions.RESISTANCE.get(), Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.SUNDERING.get());
        addMix(ALObjects.Potions.LONG_RESISTANCE.get(), Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.LONG_SUNDERING.get());
        addMix(ALObjects.Potions.STRONG_RESISTANCE.get(), Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.STRONG_SUNDERING.get());
        addMix(ALObjects.Potions.SUNDERING.get(), Items.REDSTONE, ALObjects.Potions.LONG_SUNDERING.get());
        addMix(ALObjects.Potions.SUNDERING.get(), Items.GLOWSTONE_DUST, ALObjects.Potions.STRONG_SUNDERING.get());

        addMix(Potions.AWKWARD, Items.GOLDEN_APPLE, ALObjects.Potions.ABSORPTION.get());
        addMix(ALObjects.Potions.ABSORPTION.get(), Items.REDSTONE, ALObjects.Potions.LONG_ABSORPTION.get());
        addMix(ALObjects.Potions.ABSORPTION.get(), Items.GLOWSTONE_DUST, ALObjects.Potions.STRONG_ABSORPTION.get());

        addMix(Potions.AWKWARD, Items.MUSHROOM_STEW, ALObjects.Potions.HASTE.get());
        addMix(ALObjects.Potions.HASTE.get(), Items.REDSTONE, ALObjects.Potions.LONG_HASTE.get());
        addMix(ALObjects.Potions.HASTE.get(), Items.GLOWSTONE_DUST, ALObjects.Potions.STRONG_HASTE.get());

        addMix(ALObjects.Potions.HASTE.get(), Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.FATIGUE.get());
        addMix(ALObjects.Potions.LONG_HASTE.get(), Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.LONG_FATIGUE.get());
        addMix(ALObjects.Potions.STRONG_HASTE.get(), Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.STRONG_FATIGUE.get());
        addMix(ALObjects.Potions.FATIGUE.get(), Items.REDSTONE, ALObjects.Potions.LONG_FATIGUE.get());
        addMix(ALObjects.Potions.FATIGUE.get(), Items.GLOWSTONE_DUST, ALObjects.Potions.STRONG_FATIGUE.get());

        // if (ALObjects.Items.SKULL_FRAGMENT.isPresent()) addMix(Potions.AWKWARD, ALObjects.Items.SKULL_FRAGMENT.get(), ALObjects.Potions.WITHER.get());
        addMix(Potions.AWKWARD, Items.WITHER_SKELETON_SKULL, ALObjects.Potions.WITHER.get());
        addMix(ALObjects.Potions.WITHER.get(), Items.REDSTONE, ALObjects.Potions.LONG_WITHER.get());
        addMix(ALObjects.Potions.WITHER.get(), Items.GLOWSTONE_DUST, ALObjects.Potions.STRONG_WITHER.get());

        addMix(Potions.AWKWARD, Items.EXPERIENCE_BOTTLE, ALObjects.Potions.KNOWLEDGE.get());
        addMix(ALObjects.Potions.KNOWLEDGE.get(), Items.REDSTONE, ALObjects.Potions.LONG_KNOWLEDGE.get());
        addMix(ALObjects.Potions.KNOWLEDGE.get(), Items.EXPERIENCE_BOTTLE, ALObjects.Potions.STRONG_KNOWLEDGE.get());

        // addMix(Potions.AWKWARD, ALObjects.Items.LUCKY_FOOT.get(), Potions.LUCK);

        addMix(Potions.AWKWARD, Items.SWEET_BERRIES, ALObjects.Potions.VITALITY.get());
        addMix(ALObjects.Potions.VITALITY.get(), Items.REDSTONE, ALObjects.Potions.LONG_VITALITY.get());
        addMix(ALObjects.Potions.VITALITY.get(), Items.GLOWSTONE_DUST, ALObjects.Potions.STRONG_VITALITY.get());

        addMix(ALObjects.Potions.VITALITY.get(), Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.GRIEVOUS.get());
        addMix(ALObjects.Potions.LONG_VITALITY.get(), Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.LONG_GRIEVOUS.get());
        addMix(ALObjects.Potions.STRONG_VITALITY.get(), Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.STRONG_GRIEVOUS.get());
        addMix(ALObjects.Potions.GRIEVOUS.get(), Items.REDSTONE, ALObjects.Potions.LONG_GRIEVOUS.get());
        addMix(ALObjects.Potions.GRIEVOUS.get(), Items.GLOWSTONE_DUST, ALObjects.Potions.STRONG_GRIEVOUS.get());

        addMix(Potions.SLOW_FALLING, Items.FERMENTED_SPIDER_EYE, ALObjects.Potions.LEVITATION.get());
        addMix(ALObjects.Potions.LEVITATION.get(), Items.POPPED_CHORUS_FRUIT, ALObjects.Potions.FLYING.get());
        addMix(ALObjects.Potions.FLYING.get(), Items.REDSTONE, ALObjects.Potions.LONG_FLYING.get());
        addMix(ALObjects.Potions.LONG_FLYING.get(), Items.REDSTONE, ALObjects.Potions.EXTRA_LONG_FLYING.get());
    }

    private void addMix(Potion input, Item ingredient, Potion output) {
        ResourceLocation inKey = BuiltInRegistries.POTION.getKey(input);
        ResourceLocation outKey = BuiltInRegistries.POTION.getKey(output);
        write(new JsonMix<>(input, Ingredient.of(ingredient), output, JsonMix.Type.POTION), "brewing_mixes", outKey.getPath() + "_from_" + inKey.getPath());
    }

    @SuppressWarnings("unchecked")
    private <T extends CodecProvider<T>> void write(T object, String type, String path) {
        this.futures.add(DataProvider.saveStable(this.cachedOutput, (Codec<T>) object.getCodec(), object, outputDir.resolve(type + "/" + path + ".json")));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput pOutput) {
        this.cachedOutput = pOutput;
        genPotionRecipes();
        return CompletableFuture.allOf(this.futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return ApothicAttributes.MODID;
    }

}
