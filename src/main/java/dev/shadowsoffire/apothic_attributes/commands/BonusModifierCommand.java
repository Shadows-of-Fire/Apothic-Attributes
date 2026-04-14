package dev.shadowsoffire.apothic_attributes.commands;

import java.util.Arrays;
import java.util.Locale;

import org.spongepowered.include.com.google.common.base.Preconditions;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.api.ALObjects.BuiltInRegs;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class BonusModifierCommand {

    public static final SuggestionProvider<CommandSourceStack> SUGGEST_OP = (ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(Operation.values()).map(Operation::name), builder);

    public static final SuggestionProvider<CommandSourceStack> SUGGEST_SLOT = (ctx, builder) -> SharedSuggestionProvider
        .suggest(BuiltInRegs.ENTITY_SLOT_GROUP.keySet().stream().map(Identifier::toString), builder);

    public static final SuggestionProvider<CommandSourceStack> SUGGEST_ATTRIB = (ctx, builder) -> SharedSuggestionProvider.suggest(BuiltInRegistries.ATTRIBUTE.keySet().stream().map(Identifier::toString), builder);

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("add_bonus_modifier").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.argument("attribute", IdentifierArgument.id()).suggests(SUGGEST_ATTRIB)
                .then(Commands.argument("op", StringArgumentType.word()).suggests(SUGGEST_OP).then(Commands.argument("value", FloatArgumentType.floatArg())
                    .then(Commands.argument("slot", IdentifierArgument.id()).suggests(SUGGEST_SLOT).executes(c -> {
                        Player p = c.getSource().getPlayerOrException();
                        Holder<Attribute> attrib = BuiltInRegistries.ATTRIBUTE.get(c.getArgument("attribute", Identifier.class)).map(h -> (Holder<Attribute>) h).orElseThrow();
                        Operation op = Operation.valueOf(c.getArgument("op", String.class).toUpperCase(Locale.ROOT));
                        EntitySlotGroup slot = BuiltInRegs.ENTITY_SLOT_GROUP.get(c.getArgument("slot", Identifier.class)).map(Holder.Reference::value).orElse(null);
                        Preconditions.checkNotNull(slot, "Unknown slot: " + c.getArgument("slot", Identifier.class));
                        float value = c.getArgument("value", Float.class);
                        ItemStack stack = p.getMainHandItem();

                        StackAttributeModifiers bonus = stack.getOrDefault(ALObjects.Components.BONUS_STACK_ATTRIBUTE_MODIFIERS, StackAttributeModifiers.EMPTY);
                        AttributeModifier modif = new AttributeModifier(ApothicAttributes.loc("command_generated_" + p.level().getRandom().nextInt()), value, op);

                        stack.set(ALObjects.Components.BONUS_STACK_ATTRIBUTE_MODIFIERS, bonus.withModifierAdded(attrib, modif, slot));
                        return 0;
                    }))))));
    }

}
