package dev.shadowsoffire.apothic_attributes.api;

import static dev.shadowsoffire.apothic_attributes.ApothicAttributes.R;

import java.util.function.Supplier;

import org.jetbrains.annotations.ApiStatus;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.impl.BooleanAttribute;
import dev.shadowsoffire.apothic_attributes.impl.PercentBasedAttribute;
import dev.shadowsoffire.apothic_attributes.mobfx.BleedingEffect;
import dev.shadowsoffire.apothic_attributes.mobfx.DetonationEffect;
import dev.shadowsoffire.apothic_attributes.mobfx.FlyingEffect;
import dev.shadowsoffire.apothic_attributes.mobfx.GrievousEffect;
import dev.shadowsoffire.apothic_attributes.mobfx.KnowledgeEffect;
import dev.shadowsoffire.apothic_attributes.mobfx.SunderingEffect;
import dev.shadowsoffire.apothic_attributes.mobfx.VitalityEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForgeMod;

public class ALObjects {

    public static class Attributes {

        /**
         * Flat armor penetration. Base value = (0.0) = 0 armor reduced during damage calculations.
         */
        public static final Supplier<Attribute> ARMOR_PIERCE = R.attribute("armor_pierce", () -> new RangedAttribute("apothic_attributes:armor_pierce", 0.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Percentage armor reduction. Base value = (0.0) = 0% of armor reduced during damage calculations.
         */
        public static final Supplier<Attribute> ARMOR_SHRED = R.attribute("armor_shred", () -> new PercentBasedAttribute("apothic_attributes:armor_shred", 0.0D, 0.0D, 2.0D).setSyncable(true));

        /**
         * Arrow Damage. Base value = (1.0) = 100% default arrow damage
         */
        public static final Supplier<Attribute> ARROW_DAMAGE = R.attribute("arrow_damage", () -> new PercentBasedAttribute("apothic_attributes:arrow_damage", 1.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Arrow Velocity. Base value = (1.0) = 100% default arrow velocity
         * <p>
         * Arrow damage scales with the velocity as well as {@link #ARROW_DAMAGE} and the base damage of the arrow entity.
         */
        public static final Supplier<Attribute> ARROW_VELOCITY = R.attribute("arrow_velocity", () -> new PercentBasedAttribute("apothic_attributes:arrow_velocity", 1.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Bonus magic damage that slows enemies hit. Base value = (0.0) = 0 damage
         */
        public static final Supplier<Attribute> COLD_DAMAGE = R.attribute("cold_damage", () -> new RangedAttribute("apothic_attributes:cold_damage", 0.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Chance that any attack will critically strike. Base value = (0.05) = 5% chance to critically strike.<br>
         * Not related to vanilla (jump) critical strikes.
         */
        public static final Supplier<Attribute> CRIT_CHANCE = R.attribute("crit_chance", () -> new PercentBasedAttribute("apothic_attributes:crit_chance", 0.05D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Amount of damage caused by critical strikes. Base value = (1.5) = 150% normal damage dealt.<br>
         * Also impacts vanilla (jump) critical strikes.
         */
        public static final Supplier<Attribute> CRIT_DAMAGE = R.attribute("crit_damage", () -> new PercentBasedAttribute("apothic_attributes:crit_damage", 1.5D, 1.0D, 100.0D).setSyncable(true));

        /**
         * Bonus physical damage dealt equal to enemy's current health. Base value = (0.0) = 0%
         */
        public static final Supplier<Attribute> CURRENT_HP_DAMAGE = R.attribute("current_hp_damage", () -> new PercentBasedAttribute("apothic_attributes:current_hp_damage", 0.0D, 0.0D, 1.0D).setSyncable(true));

        /**
         * Chance to dodge incoming melee damage. Base value = (0.0) = 0% chance to dodge.<br>
         * "Melee" damage is considered as damage from another entity within the player's attack range.
         * <p>
         * This includes projectile attacks, as long as the projectile actually impacts the player.
         */
        public static final Supplier<Attribute> DODGE_CHANCE = R.attribute("dodge_chance", () -> new PercentBasedAttribute("apothic_attributes:dodge_chance", 0.0D, 0.0D, 1.0D).setSyncable(true));

        /**
         * How fast a ranged weapon is charged. Base Value = (1.0) = 100% default draw speed.
         */
        public static final Supplier<Attribute> DRAW_SPEED = R.attribute("draw_speed", () -> new PercentBasedAttribute("apothic_attributes:draw_speed", 1.0D, 0.0D, 4.0D).setSyncable(true));

        /**
         * Experience mulitplier, from killing mobs or breaking ores. Base value = (1.0) = 100% xp gained.
         */
        public static final Supplier<Attribute> EXPERIENCE_GAINED = R.attribute("experience_gained", () -> new PercentBasedAttribute("apothic_attributes:experience_gained", 1.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Bonus magic damage that burns enemies hit. Base value = (0.0) = 0 damage
         */
        public static final Supplier<Attribute> FIRE_DAMAGE = R.attribute("fire_damage", () -> new RangedAttribute("apothic_attributes:fire_damage", 0.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Extra health that regenerates when not taking damage. Base value = (0.0) = 0 damage
         */
        public static final Supplier<Attribute> GHOST_HEALTH = R.attribute("ghost_health", () -> new RangedAttribute("apothic_attributes:ghost_health", 0.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Adjusts all healing received. Base value = (1.0) = 100% xp gained.
         */
        public static final Supplier<Attribute> HEALING_RECEIVED = R.attribute("healing_received", () -> new PercentBasedAttribute("apothic_attributes:healing_received", 1.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Percent of physical damage converted to health. Base value = (0.0) = 0%
         */
        public static final Supplier<Attribute> LIFE_STEAL = R.attribute("life_steal", () -> new PercentBasedAttribute("apothic_attributes:life_steal", 0.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Mining Speed. Base value = (1.0) = 100% default break speed
         */
        public static final Supplier<Attribute> MINING_SPEED = R.attribute("mining_speed", () -> new PercentBasedAttribute("apothic_attributes:mining_speed", 1.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Percent of physical damage converted to absorption hearts. Base value = (0.0) = 0%
         */
        public static final Supplier<Attribute> OVERHEAL = R.attribute("overheal", () -> new PercentBasedAttribute("apothic_attributes:overheal", 0.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Flat protection penetration. Base value = (0.0) = 0 protection points bypassed during damage calculations.
         */
        public static final Supplier<Attribute> PROT_PIERCE = R.attribute("prot_pierce", () -> new RangedAttribute("apothic_attributes:prot_pierce", 0.0D, 0.0D, 34.0D).setSyncable(true));

        /**
         * Percentage protection reduction. Base value = (0.0) = 0% of protection points bypassed during damage calculations.
         */
        public static final Supplier<Attribute> PROT_SHRED = R.attribute("prot_shred", () -> new PercentBasedAttribute("apothic_attributes:prot_shred", 0.0D, 0.0D, 1.0D).setSyncable(true));

        /**
         * Boolean attribute for if elytra flight is enabled. Default value = false.
         */
        public static final Supplier<Attribute> ELYTRA_FLIGHT = R.attribute("elytra_flight", () -> new BooleanAttribute("apothic_attributes:elytra_flight", false).setSyncable(true));

        /**
         * Boolean attribute for if creative flight is enabled. Default value = false.
         * 
         * @deprecated Use {@link NeoForgeMod#CREATIVE_FLIGHT}.
         */
        @Deprecated(forRemoval = true, since = "1.20.4")
        public static final Supplier<Attribute> CREATIVE_FLIGHT = NeoForgeMod.CREATIVE_FLIGHT::value;

        @ApiStatus.Internal
        public static void bootstrap() {}
    }

    public static class MobEffects extends net.minecraft.world.effect.MobEffects { // Hack to bring vanilla things in-scope

        /**
         * Bleeding inflicts 1 + level damage every two seconds. Things that apply bleeding usually stack.
         */
        public static final Supplier<BleedingEffect> BLEEDING = R.effect("bleeding", BleedingEffect::new);

        /**
         * Flaming Detonation, when it expires, consumes all fire ticks and deals armor-piercing damage based on the duration.
         */
        public static final Supplier<DetonationEffect> DETONATION = R.effect("detonation", DetonationEffect::new);

        /**
         * Grievous Wounds reduces healing received by 40%/level.
         */
        public static final Supplier<GrievousEffect> GRIEVOUS = R.effect("grievous", GrievousEffect::new);

        /**
         * Ancient Knowledge multiplies experience dropped by mobs by level * {@link MobFxLib#knowledgeMult}.<br>
         * The multiplier is configurable.
         */
        public static final Supplier<KnowledgeEffect> KNOWLEDGE = R.effect("knowledge", KnowledgeEffect::new);

        /**
         * Sundering is the inverse of resistance. It increases damage taken by 20%/level.<br>
         * Each point of sundering cancels out a single point of resistance, if present.
         */
        public static final Supplier<SunderingEffect> SUNDERING = R.effect("sundering", SunderingEffect::new);

        /**
         * Bursting Vitality increases healing received by 20%/level.
         */
        public static final Supplier<VitalityEffect> VITALITY = R.effect("vitality", VitalityEffect::new);

        /**
         * Grants Creative Flight
         */
        public static final Supplier<FlyingEffect> FLYING = R.effect("flying", FlyingEffect::new);

        @ApiStatus.Internal
        public static void bootstrap() {}
    }

    public static class Particles {

        public static final Supplier<SimpleParticleType> APOTH_CRIT = R.particle("apoth_crit", () -> new SimpleParticleType(false));

        @ApiStatus.Internal
        public static void bootstrap() {}

    }

    public static class Sounds {

        public static final Holder<SoundEvent> DODGE = R.sound("dodge");

        @ApiStatus.Internal
        public static void bootstrap() {}

    }

    public static class DamageTypes {

        /**
         * Damage type used by {@link MobEffects#BLEEDING}. Bypasses armor.
         */
        public static final ResourceKey<DamageType> BLEEDING = ResourceKey.create(Registries.DAMAGE_TYPE, ApothicAttributes.loc("bleeding"));

        /**
         * Damage type used by {@link MobEffects#DETONATION}. Bypasses armor, and is marked as magic damage.
         */
        public static final ResourceKey<DamageType> DETONATION = ResourceKey.create(Registries.DAMAGE_TYPE, ApothicAttributes.loc("detonation"));

        /**
         * Damage type used by {@link Attributes#CURRENT_HP_DAMAGE}. Same properties as generic physical damage. Has attacker context.
         */
        public static final ResourceKey<DamageType> CURRENT_HP_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ApothicAttributes.loc("current_hp_damage"));

        /**
         * Damage type used by {@link Attributes#FIRE_DAMAGE}. Bypasses armor, and is marked as magic damage. Has attacker context.<br>
         * Not marked as fire damage until fire resistance is reworked to not block all fire damage.
         */
        public static final ResourceKey<DamageType> FIRE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ApothicAttributes.loc("fire_damage"));

        /**
         * Damage type used by {@link Attributes#COLD_DAMAGE}. Bypasses armor, and is marked as magic damage. Has attacker context.
         */
        public static final ResourceKey<DamageType> COLD_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ApothicAttributes.loc("cold_damage"));

        @ApiStatus.Internal
        public static void bootstrap() {}
    }

    public static final class Potions {
        public static final Supplier<Potion> RESISTANCE = R.singlePotion("resistance", () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 3600));
        public static final Supplier<Potion> LONG_RESISTANCE = R.singlePotion("long_resistance", () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 9600));
        public static final Supplier<Potion> STRONG_RESISTANCE = R.singlePotion("strong_resistance", () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1800, 1));

        public static final Supplier<Potion> ABSORPTION = R.singlePotion("absorption", () -> new MobEffectInstance(MobEffects.ABSORPTION, 1200, 1));
        public static final Supplier<Potion> LONG_ABSORPTION = R.singlePotion("long_absorption", () -> new MobEffectInstance(MobEffects.ABSORPTION, 3600, 1));
        public static final Supplier<Potion> STRONG_ABSORPTION = R.singlePotion("strong_absorption", () -> new MobEffectInstance(MobEffects.ABSORPTION, 600, 3));

        public static final Supplier<Potion> HASTE = R.singlePotion("haste", () -> new MobEffectInstance(MobEffects.DIG_SPEED, 3600));
        public static final Supplier<Potion> LONG_HASTE = R.singlePotion("long_haste", () -> new MobEffectInstance(MobEffects.DIG_SPEED, 9600));
        public static final Supplier<Potion> STRONG_HASTE = R.singlePotion("strong_haste", () -> new MobEffectInstance(MobEffects.DIG_SPEED, 1800, 1));

        public static final Supplier<Potion> FATIGUE = R.singlePotion("fatigue", () -> new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 3600));
        public static final Supplier<Potion> LONG_FATIGUE = R.singlePotion("long_fatigue", () -> new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 9600));
        public static final Supplier<Potion> STRONG_FATIGUE = R.singlePotion("strong_fatigue", () -> new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 1800, 1));

        public static final Supplier<Potion> WITHER = R.singlePotion("wither", () -> new MobEffectInstance(MobEffects.WITHER, 3600));
        public static final Supplier<Potion> LONG_WITHER = R.singlePotion("long_wither", () -> new MobEffectInstance(MobEffects.WITHER, 9600));
        public static final Supplier<Potion> STRONG_WITHER = R.singlePotion("strong_wither", () -> new MobEffectInstance(MobEffects.WITHER, 1800, 1));

        public static final Supplier<Potion> SUNDERING = R.singlePotion("sundering", () -> new MobEffectInstance(MobEffects.SUNDERING.get(), 3600));
        public static final Supplier<Potion> LONG_SUNDERING = R.singlePotion("long_sundering", () -> new MobEffectInstance(MobEffects.SUNDERING.get(), 9600));
        public static final Supplier<Potion> STRONG_SUNDERING = R.singlePotion("strong_sundering", () -> new MobEffectInstance(MobEffects.SUNDERING.get(), 1800, 1));

        public static final Supplier<Potion> KNOWLEDGE = R.singlePotion("knowledge", () -> new MobEffectInstance(MobEffects.KNOWLEDGE.get(), 2400));
        public static final Supplier<Potion> LONG_KNOWLEDGE = R.singlePotion("long_knowledge", () -> new MobEffectInstance(ALObjects.MobEffects.KNOWLEDGE.get(), 4800));
        public static final Supplier<Potion> STRONG_KNOWLEDGE = R.singlePotion("strong_knowledge", () -> new MobEffectInstance(ALObjects.MobEffects.KNOWLEDGE.get(), 1200, 3));

        public static final Supplier<Potion> VITALITY = R.singlePotion("vitality", () -> new MobEffectInstance(ALObjects.MobEffects.VITALITY.get(), 4800));
        public static final Supplier<Potion> LONG_VITALITY = R.singlePotion("long_vitality", () -> new MobEffectInstance(ALObjects.MobEffects.VITALITY.get(), 14400));
        public static final Supplier<Potion> STRONG_VITALITY = R.singlePotion("strong_vitality", () -> new MobEffectInstance(ALObjects.MobEffects.VITALITY.get(), 3600, 1));

        public static final Supplier<Potion> GRIEVOUS = R.singlePotion("grievous", () -> new MobEffectInstance(MobEffects.GRIEVOUS.get(), 4800));
        public static final Supplier<Potion> LONG_GRIEVOUS = R.singlePotion("long_grievous", () -> new MobEffectInstance(MobEffects.GRIEVOUS.get(), 14400));
        public static final Supplier<Potion> STRONG_GRIEVOUS = R.singlePotion("strong_grievous", () -> new MobEffectInstance(MobEffects.GRIEVOUS.get(), 3600, 1));

        public static final Supplier<Potion> LEVITATION = R.singlePotion("levitation", () -> new MobEffectInstance(MobEffects.LEVITATION, 2400));

        public static final Supplier<Potion> FLYING = R.singlePotion("flying", () -> new MobEffectInstance(MobEffects.FLYING.get(), 9600));
        public static final Supplier<Potion> LONG_FLYING = R.singlePotion("long_flying", () -> new MobEffectInstance(MobEffects.FLYING.get(), 18000));
        public static final Supplier<Potion> EXTRA_LONG_FLYING = R.singlePotion("extra_long_flying", () -> new MobEffectInstance(MobEffects.FLYING.get(), 36000));

        @ApiStatus.Internal
        public static void bootstrap() {}
    }

    @ApiStatus.Internal
    public static void bootstrap(IEventBus bus) {
        Attributes.bootstrap();
        MobEffects.bootstrap();
        Particles.bootstrap();
        Sounds.bootstrap();
        DamageTypes.bootstrap();
        Potions.bootstrap();
        bus.register(R);
    }
}
