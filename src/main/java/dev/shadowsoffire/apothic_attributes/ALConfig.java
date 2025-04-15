package dev.shadowsoffire.apothic_attributes;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import dev.shadowsoffire.apothic_attributes.repack.evalex.Expression;
import dev.shadowsoffire.placebo.config.Configuration;
import dev.shadowsoffire.placebo.util.Offset;
import dev.shadowsoffire.placebo.util.Offset.AnchorPoint;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class ALConfig {

    public static final String[] DEFAULT_BLOCKED_ATTRIBUTES = { "neoforge:nametag_distance", "neoforge:creative_flight", "apothic_attributes:elytra_flight", "apothic_attributes:ghost_health" };

    public static boolean enableAttributesGui = true;
    public static boolean enablePotionTooltips = true;
    public static Set<ResourceLocation> hiddenAttributes = new HashSet<>();
    public static float knowledgeMultiplier = 4.0F;
    public static Offset attributesGuiButtonOffset = new Offset(AnchorPoint.TOP_RIGHT, -3, 3);

    private static Optional<Expression> protExpr;
    private static Optional<Expression> aValueExpr;
    private static Optional<Expression> armorExpr;
    private static Optional<Expression> toughnessExpr;

    public static Configuration load() {
        Configuration cfg = new Configuration(ApothicAttributes.getConfigFile(ApothicAttributes.MODID));
        enableAttributesGui = cfg.getBoolean("Enable Attributes GUI", "general", true, "If the Attributes GUI is available.\nClient-authoritative.");
        enablePotionTooltips = cfg.getBoolean("Enable Potion Tooltips", "general", true, "If description tooltips will be added to potion items.\nClient-authoritative.");
        knowledgeMultiplier = cfg.getFloat("Ancient Knowledge Multiplier", "effects", 4.0F, 1.0F, 1024F, "The value (per-level) of the Experience Gained attribute modifier provided by Ancient Knowledge.\nSynced.");
        String[] hidden = cfg.getStringList("Hidden Attributes", "general", DEFAULT_BLOCKED_ATTRIBUTES, "A list of attributes that will be hidden from the Attributes GUI.\nClient-authoritative.");

        hiddenAttributes.clear();
        for (String name : hidden) {
            try {
                hiddenAttributes.add(ResourceLocation.parse(name));
            }
            catch (ResourceLocationException ex) {
                ApothicAttributes.LOGGER.error("Ignoring invalid \"Hidden Attributes\" config entry " + name, ex);
            }
        }

        attributesGuiButtonOffset = Offset.load("GUI Button Offset", "client", attributesGuiButtonOffset, cfg);

        protExpr = readConfigExpression(cfg, "Protection Formula", "combat_rules", "1 - min(0.025 * protPoints, 0.85)",
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

        aValueExpr = readConfigExpression(cfg, "A-Value Formula", "combat_rules", "if(damage < 20, 10, 10 + (damage - 20) / 2)",
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

        armorExpr = readConfigExpression(cfg, "Armor Formula", "combat_rules", "a / (a + armor)",
            """
                The armor damage reduction formula.
                Computed after Armor Pierce and Armor Shred are applied.
                Arguments:
                    'a' - The a-value computed by the a-value formula.
                    'damage' - The damage of the incoming attack.
                    'armor' - The armor value of the user after reductions.
                    'toughness' - The armor toughness value of the user.
                Output:
                    The percentage of damage taken after armor has been applied, from 0 (no damage taken) to 1 (full damage taken).
                Reference:
                    See https://github.com/ezylang/EvalEx#usage-examples for how to write expressions.
                Note:
                    The vanilla formula is: 1 - min(max(armor - damage / (2 + toughness / 4), armor / 5), 20) / 25
                """,
            "a", "damage", "armor", "toughness");

        toughnessExpr = readConfigExpression(cfg, "Armor Toughness Formula", "combat_rules", "min(toughness * 0.02, 0.6)",
            """
                The armor toughness formula.
                This is used to determine how armor toughness impacts enemy Armor Pierce / Armor Shred.
                Arguments:
                    'damage' - The damage of the incoming attack.
                    'armor' - The armor value of the user after reductions.
                    'toughness' - The armor toughness value of the user.
                Output:
                    The percentage by which enemy armor pierce/shred will be reduced, from 0 (no change) to 1 (preventing it completely).
                Reference:
                    See https://github.com/ezylang/EvalEx#usage-examples for how to write expressions.
                """,
            "damage", "armor", "toughness");

        if (cfg.hasChanged()) cfg.save();
        return cfg;
    }

    public static Optional<Expression> getAValueExpr() {
        return aValueExpr;
    }

    public static Optional<Expression> getProtExpr() {
        return protExpr;
    }

    public static Optional<Expression> getArmorExpr() {
        return armorExpr;
    }

    public static Optional<Expression> getToughnessExpr() {
        return toughnessExpr;
    }

    public static ResourceManagerReloadListener makeReloader() {
        return resman -> load();
    }

    /**
     * Parses an {@link Expression} from the config file with the specific parameters.
     * <p>
     * Expressions are orders of magnitudes more expensive to evaluate when compared to the relevant java code, so do not use them unless absolutely necessary.
     * 
     * @param args A list of argument names used by the expression, to sanity-check execution.
     * @return An optional containing the configured expression, or an empty optional if the default was used or an exception occurred.
     */
    private static Optional<Expression> readConfigExpression(Configuration cfg, String key, String group, String defaultValue, String comment, String... args) {
        String exprStr = cfg.getString(key, group, defaultValue, comment);

        if (exprStr.equals(defaultValue)) {
            return Optional.empty();
        }

        try {
            Expression expr = new Expression(exprStr);
            for (String arg : args) {
                expr.setVariable(arg, new BigDecimal(ThreadLocalRandom.current().nextInt(20)));
            }
            expr.eval();
            return Optional.of(expr);
        }
        catch (Exception ex) {
            ApothicAttributes.LOGGER.error("Ignoring invalid {} entry {} as the expression failed to evaluate.", key, exprStr);
            ex.printStackTrace();
            return Optional.empty();
        }
    }
}
