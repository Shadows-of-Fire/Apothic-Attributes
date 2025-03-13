package dev.shadowsoffire.apothic_attributes;

import java.io.File;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.client.AttributesLibClient;
import dev.shadowsoffire.apothic_attributes.compat.CuriosCompat;
import dev.shadowsoffire.apothic_attributes.impl.AttributeEvents;
import dev.shadowsoffire.apothic_attributes.payload.ConfigPayload;
import dev.shadowsoffire.apothic_attributes.payload.CritParticlePayload;
import dev.shadowsoffire.apothic_attributes.util.MiscDatagen;
import dev.shadowsoffire.placebo.network.PayloadHelper;
import dev.shadowsoffire.placebo.registry.DeferredHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

@Mod(ApothicAttributes.MODID)
public class ApothicAttributes {

    public static final String MODID = "apothic_attributes";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static final DeferredHelper R = DeferredHelper.create(MODID);
    private static final File configDir = new File(FMLPaths.CONFIGDIR.get().toFile(), "apotheosis");

    /**
     * Static record of {@link Player#getAttackStrengthScale(float)} for use in damage events.<br>
     * Recorded in the {@link PlayerAttackEvent} and valid for the entire chain, when a player attacks.
     */
    private static float localAtkStrength = 1;

    public ApothicAttributes(IEventBus bus) {
        bus.register(this);
        NeoForge.EVENT_BUS.register(new AttributeEvents());
        NeoForge.EVENT_BUS.addListener(ApothicAttributes::trackCooldown);
        if (FMLEnvironment.dist.isClient()) {
            NeoForge.EVENT_BUS.register(new AttributesLibClient());
            bus.register(AttributesLibClient.ModBusSub.class);
        }

        PayloadHelper.registerPayload(new CritParticlePayload.Provider());
        ALObjects.bootstrap(bus);
        ALConfig.load();
        NeoForgeMod.enableMergedAttributeTooltips();
    }

    @SubscribeEvent
    public void init(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {
            MobEffects.BLINDNESS.value().addAttributeModifier(Attributes.FOLLOW_RANGE, loc("blindness"), -0.75, Operation.ADD_MULTIPLIED_TOTAL);
            // TODO: Update to show in GUI without applying attribute to entity
            // if (MobEffects.SLOW_FALLING.getAttributeModifiers().isEmpty()) {
            // MobEffects.SLOW_FALLING.addAttributeModifier(ForgeMod.ENTITY_GRAVITY.get(), "A5B6CF2A-2F7C-31EF-9022-7C3E7D5E6ABA", -0.07, Operation.ADDITION);
            // }
        });
        PayloadHelper.registerPayload(new ConfigPayload.Provider());
    }

    // TODO - Update impls to reflect new default values.
    @SubscribeEvent
    public void applyAttribs(EntityAttributeModificationEvent e) {
        e.getTypes().forEach(type -> {
            addAll(type, e::add,
                ALObjects.Attributes.DRAW_SPEED,
                ALObjects.Attributes.CRIT_CHANCE,
                ALObjects.Attributes.CRIT_DAMAGE,
                ALObjects.Attributes.COLD_DAMAGE,
                ALObjects.Attributes.FIRE_DAMAGE,
                ALObjects.Attributes.LIFE_STEAL,
                ALObjects.Attributes.CURRENT_HP_DAMAGE,
                ALObjects.Attributes.OVERHEAL,
                ALObjects.Attributes.GHOST_HEALTH,
                ALObjects.Attributes.MINING_SPEED,
                ALObjects.Attributes.ARROW_DAMAGE,
                ALObjects.Attributes.ARROW_VELOCITY,
                ALObjects.Attributes.EXPERIENCE_GAINED,
                ALObjects.Attributes.HEALING_RECEIVED,
                ALObjects.Attributes.ARMOR_PIERCE,
                ALObjects.Attributes.ARMOR_SHRED,
                ALObjects.Attributes.PROJECTILE_DAMAGE,
                ALObjects.Attributes.PROT_PIERCE,
                ALObjects.Attributes.PROT_SHRED,
                ALObjects.Attributes.DODGE_CHANCE,
                ALObjects.Attributes.ELYTRA_FLIGHT);
        });
    }

    @SafeVarargs
    private static void addAll(EntityType<? extends LivingEntity> type, BiConsumer<EntityType<? extends LivingEntity>, Holder<Attribute>> add, Holder<Attribute>... attribs) {
        for (Holder<Attribute> a : attribs)
            add.accept(type, a);
    }

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent e) {
        AttributeSupplier playerAttribs = DefaultAttributes.getSupplier(EntityType.PLAYER);
        BuiltInRegistries.ATTRIBUTE.holders().forEach(attr -> {
            if (playerAttribs.hasAttribute(attr)) {
                attr.value().setSyncable(true);
            }
        });
        if (ModList.get().isLoaded("curios")) {
            e.enqueueWork(CuriosCompat::init);
        }
    }

    @SubscribeEvent
    public void data(GatherDataEvent e) {
        MiscDatagen gen = new MiscDatagen(e.getGenerator().getPackOutput().getOutputFolder(Target.DATA_PACK).resolve(MODID), e.getLookupProvider());
        e.getGenerator().addProvider(true, gen);
    }

    public static File getConfigFile(String path) {
        return new File(configDir, path + ".cfg");
    }

    /**
     * Gets the local attack strength of an entity.
     * <p>
     * For players, this is recorded in {@link AttackEntityEvent} and is valid for other damage events.
     * <p>
     * For non-players, this value is always 1.
     */
    public static float getLocalAtkStrength(Entity entity) {
        if (entity instanceof Player) {
            return localAtkStrength;
        }
        return 1;
    }

    /**
     * Gets the current tooltip flag.
     * 
     * @return If called on the client, the current tooltip flag, otherwise {@link TooltipFlag#NORMAL}
     */
    public static TooltipFlag getTooltipFlag() {
        if (FMLEnvironment.dist.isClient()) {
            return ClientAccess.getTooltipFlag();
        }
        return TooltipFlag.NORMAL;
    }

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    /**
     * Constructs a mutable component with a lang key of the form "type.modid.path", using {@link ApothicAttributes#MODID}.
     *
     * @param type The type of language key, "misc", "info", "title", etc...
     * @param path The path of the language key.
     * @param args Translation arguments passed to the created translatable component.
     */
    public static MutableComponent lang(String type, String path, Object... args) {
        return Component.translatable(langKey(type, path), args);
    }

    public static String langKey(String type, String path) {
        return type + "." + MODID + "." + path;
    }

    private static class ClientAccess {
        static TooltipFlag getTooltipFlag() {
            return Minecraft.getInstance().options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;
        }
    }

    private static void trackCooldown(AttackEntityEvent e) {
        Player p = e.getEntity();
        ApothicAttributes.localAtkStrength = p.getAttackStrengthScale(0.5F);
    }
}
