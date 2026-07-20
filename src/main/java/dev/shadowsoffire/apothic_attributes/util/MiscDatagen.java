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
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
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
    private final CompletableFuture<HolderLookup.Provider> regs;
    private CachedOutput cachedOutput;
    private List<CompletableFuture<?>> futures = new ArrayList<>();

    public MiscDatagen(Path outputDir, CompletableFuture<HolderLookup.Provider> regs) {
        this.outputDir = outputDir;
        this.regs = regs;
        var map = (Object2IntOpenHashMap<String>) DataProvider.FIXED_ORDER_FIELDS;
        map.put("mix_type", 0);
    }

    // Adapted from the old recipe code in Apotheosis
    public void genPotionRecipes() {
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

        // if (ALObjects.Items.SKULL_FRAGMENT.isPresent()) addMix(Potions.AWKWARD, ALObjects.Items.SKULL_FRAGMENT, ALObjects.Potions.WITHER);
        addMix(Potions.AWKWARD, Items.WITHER_SKELETON_SKULL, ALObjects.Potions.WITHER);
        addMix(ALObjects.Potions.WITHER, Items.REDSTONE, ALObjects.Potions.LONG_WITHER);
        addMix(ALObjects.Potions.WITHER, Items.GLOWSTONE_DUST, ALObjects.Potions.STRONG_WITHER);

        addMix(Potions.AWKWARD, Items.EXPERIENCE_BOTTLE, ALObjects.Potions.KNOWLEDGE);
        addMix(ALObjects.Potions.KNOWLEDGE, Items.REDSTONE, ALObjects.Potions.LONG_KNOWLEDGE);
        addMix(ALObjects.Potions.KNOWLEDGE, Items.EXPERIENCE_BOTTLE, ALObjects.Potions.STRONG_KNOWLEDGE);

        // addMix(Potions.AWKWARD, ALObjects.Items.LUCKY_FOOT, Potions.LUCK);

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
        ResourceLocation inKey = input.unwrapKey().get().location();
        ResourceLocation outKey = output.unwrapKey().get().location();
        write(new JsonMix<>(input, Ingredient.of(ingredient), output, JsonMix.Type.POTION), "brewing_mixes", outKey.getPath() + "_from_" + inKey.getPath());
    }

    @SuppressWarnings("unchecked")
    private <T extends CodecProvider<T>> void write(T object, String type, String path) {
        this.futures.add(this.regs.thenCompose(registries -> {
            return DataProvider.<T>saveStable(this.cachedOutput, registries, (Codec<T>) object.getCodec(), object, outputDir.resolve(type + "/" + path + ".json"));
        }));
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
