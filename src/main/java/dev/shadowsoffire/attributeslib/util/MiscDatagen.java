package dev.shadowsoffire.attributeslib.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import dev.shadowsoffire.attributeslib.api.ALObjects;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.systems.mixes.JsonMix;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.FMLPaths;

public class MiscDatagen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Adapted from the old recipe code in Apotheosis
    public static void genPotionRecipes() {
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

    private static void addMix(Potion input, Item ingredient, Potion output) {
        ResourceLocation inKey = BuiltInRegistries.POTION.getKey(input);
        ResourceLocation outKey = BuiltInRegistries.POTION.getKey(output);
        write(new JsonMix<>(input, Ingredient.of(ingredient), output, JsonMix.Type.POTION), "brewing_mixes", outKey.getPath() + "_from_" + inKey.getPath());
    }

    @SuppressWarnings("unchecked")
    private static <T extends CodecProvider<T>> void write(T object, String type, String path) {
        File file = new File(FMLPaths.GAMEDIR.get().toFile(), "datagen/" + type + "/" + path + ".json");
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            JsonWriter jWriter = new JsonWriter(writer);
            jWriter.setIndent("    ");
            JsonElement json = ((Codec<T>) object.getCodec()).encodeStart(JsonOps.INSTANCE, object).get().left().get();
            GSON.toJson(json, jWriter);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
