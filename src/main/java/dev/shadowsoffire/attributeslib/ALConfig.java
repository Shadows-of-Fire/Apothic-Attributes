package dev.shadowsoffire.attributeslib;

import java.util.HashSet;
import java.util.Set;

import dev.shadowsoffire.placebo.config.Configuration;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class ALConfig {

    public static final String[] DEFAULT_BLOCKED_ATTRIBUTES = { "forge:nametag_distance", "attributeslib:creative_flight", "attributeslib:elytra_flight", "attributeslib:ghost_health" };

    public static boolean enableAttributesGui = true;
    public static boolean enablePotionTooltips = true;
    public static Set<ResourceLocation> hiddenAttributes = new HashSet<>();

    public static void load() {
        Configuration cfg = new Configuration(AttributesLib.MODID);
        enableAttributesGui = cfg.getBoolean("Enable Attributes GUI", "general", true, "If the Attributes GUI is available.");
        enablePotionTooltips = cfg.getBoolean("Enable Potion Tooltips", "general", true, "If description tooltips will be added to potion items.");
        String[] hidden = cfg.getStringList("Hidden Attributes", "general", DEFAULT_BLOCKED_ATTRIBUTES, "A list of attributes that will be hidden from the Attributes GUI.");

        hiddenAttributes.clear();
        for (String name : hidden) {
            try {
                hiddenAttributes.add(new ResourceLocation(name));
            }
            catch (ResourceLocationException ex) {
                AttributesLib.LOGGER.error("Ignoring invalid \"Hidden Attributes\" config entry " + name, ex);
            }
        }

        if (cfg.hasChanged()) cfg.save();
    }

    public static ResourceManagerReloadListener makeReloader() {
        return resman -> load();
    }
}
