package dev.shadowsoffire.attributeslib;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import dev.shadowsoffire.placebo.config.Configuration;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import repack.evalex.Expression;

public class ALConfig {

    public static final String[] DEFAULT_BLOCKED_ATTRIBUTES = { "forge:nametag_distance", "attributeslib:creative_flight", "attributeslib:elytra_flight", "attributeslib:ghost_health" };

    public static boolean enableAttributesGui = true;
    public static boolean enablePotionTooltips = true;
    public static Set<ResourceLocation> hiddenAttributes = new HashSet<>();

    private static Expression protectionFormula;
    private static Expression aValueFormula;
    private static Expression armorFormula;

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

        protectionFormula = readConfigExpression(cfg, "Protection Formula", "combat_rules", "1 - min(0.025 * protPoints, 0.85)",
            """
                The protection damage reduction formula.
                Computed after Prot Pierce and Prot Shred are applied.
                Arguments:
                    'protPoints' - The number of protection points the user has after reductions.
                Output:
                    The percentage of damage taken after protection has been applied, from 0 (no damage taken) to 1 (full damage taken).
                Reference:
                    See https://github.com/ezylang/EvalEx#usage-examples for how to write expressions.
                """,
            "protPoints");

        aValueFormula = readConfigExpression(cfg, "A-Value Formula", "combat_rules", "if(damage < 20, 10, 10 + (damage - 20) / 2)",
            """
                The a-value formula, which computes an intermediate used in the armor formula.
                Arguments:
                    'damage' - The damage of the incoming attack.
                Output:
                    The a-value, which will be supplied as an argument to the armor formula.
                Reference:
                    See https://github.com/ezylang/EvalEx#usage-examples for how to write expressions.
                """,
            "damage");

        armorFormula = readConfigExpression(cfg, "Armor Formula", "combat_rules", "a / (a + armor)",
            """
                The armor damage reduction formula.
                Computed after Armor Pierce and Armor Shred are applied.
                Arguments:
                    'a' - The a-value computed by the a-value formula.
                    'damage' - The damage of the incoming attack.
                    'armor' - The armor value of the user after reductions.
                Output:
                    The percentage of damage taken after armor has been applied, from 0 (no damage taken) to 1 (full damage taken).
                Reference:
                    See https://github.com/ezylang/EvalEx#usage-examples for how to write expressions.
                """,
            "a", "damage", "armor");

        if (cfg.hasChanged()) cfg.save();
    }

    // Default: damage < 20 ? 10 : 10 + (damage - 20) / 2)
    public static float getAValue(float damage) {
        aValueFormula.setVariable("damage", new BigDecimal(damage));
        return aValueFormula.eval().floatValue();
    }

    // Default: 1 - min(0.025 * protPoints, 0.85)
    public static float getProtDamageReduction(float protPoints) {
        protectionFormula.setVariable("protPoints", new BigDecimal(protPoints));
        return protectionFormula.eval().floatValue();
    }

    // Default: a / (a + armor)
    public static float getArmorDamageReduction(float damage, float armor) {
        float a = getAValue(damage);
        armorFormula.setVariable("a", new BigDecimal(a)).setVariable("damage", new BigDecimal(damage)).setVariable("armor", new BigDecimal(armor));
        return armorFormula.eval().floatValue();
    }

    public static ResourceManagerReloadListener makeReloader() {
        return resman -> load();
    }

    /**
     * Parses an {@link Expression} from the config file with the specific parameters.
     * 
     * @param args A list of argument names used by the expression, to sanity-check execution.
     */
    private static Expression readConfigExpression(Configuration cfg, String key, String group, String defaultValue, String comment, String... args) {
        String exprStr = cfg.getString(key, group, defaultValue, comment);

        try {
            Expression expr = new Expression(exprStr);
            for (String arg : args) {
                expr.setVariable(arg, new BigDecimal(ThreadLocalRandom.current().nextInt(20)));
            }
            expr.eval();
            return expr;
        }
        catch (Exception ex) {
            AttributesLib.LOGGER.error("Ignoring invalid {} entry {} as the expression failed to evaluate.", key, exprStr);
            ex.printStackTrace();
            return new Expression(defaultValue);
        }
    }
}
