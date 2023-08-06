package dev.shadowsoffire.attributeslib.api;

import static dev.shadowsoffire.attributeslib.AttributesLib.R;

import org.jetbrains.annotations.ApiStatus;

import dev.shadowsoffire.attributeslib.AttributesLib;
import dev.shadowsoffire.attributeslib.impl.PercentBasedAttribute;
import dev.shadowsoffire.attributeslib.mobfx.BleedingEffect;
import dev.shadowsoffire.attributeslib.mobfx.DetonationEffect;
import dev.shadowsoffire.attributeslib.mobfx.GrievousEffect;
import dev.shadowsoffire.attributeslib.mobfx.KnowledgeEffect;
import dev.shadowsoffire.attributeslib.mobfx.SunderingEffect;
import dev.shadowsoffire.attributeslib.mobfx.VitalityEffect;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.RegistryObject;

public class ALObjects {

    public static class Attributes {

        /**
         * Flat armor penetration. Base value = (0.0) = 0 armor reduced during damage calculations.
         */
        public static final RegistryObject<Attribute> ARMOR_PIERCE = R.attribute("armor_pierce", () -> new RangedAttribute("attributeslib:armor_pierce", 0.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Percentage armor reduction. Base value = (0.0) = 0% of armor reduced during damage calculations.
         */
        public static final RegistryObject<Attribute> ARMOR_SHRED = R.attribute("armor_shred", () -> new PercentBasedAttribute("attributeslib:armor_shred", 0.0D, 0.0D, 2.0D).setSyncable(true));

        /**
         * Arrow Damage. Base value = (1.0) = 100% default arrow damage
         */
        public static final RegistryObject<Attribute> ARROW_DAMAGE = R.attribute("arrow_damage", () -> new PercentBasedAttribute("attributeslib:arrow_damage", 1.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Arrow Velocity. Base value = (1.0) = 100% default arrow velocity
         * <p>
         * Arrow damage scales with the velocity as well as {@link #ARROW_DAMAGE} and the base damage of the arrow entity.
         */
        public static final RegistryObject<Attribute> ARROW_VELOCITY = R.attribute("arrow_velocity", () -> new PercentBasedAttribute("attributeslib:arrow_velocity", 1.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Bonus magic damage that slows enemies hit. Base value = (0.0) = 0 damage
         */
        public static final RegistryObject<Attribute> COLD_DAMAGE = R.attribute("cold_damage", () -> new RangedAttribute("attributeslib:cold_damage", 0.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Chance that any attack will critically strike. Base value = (0.05) = 5% chance to critically strike.<br>
         * Not related to vanilla (jump) critical strikes.
         */
        public static final RegistryObject<Attribute> CRIT_CHANCE = R.attribute("crit_chance", () -> new PercentBasedAttribute("attributeslib:crit_chance", 0.05D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Amount of damage caused by critical strikes. Base value = (1.5) = 150% normal damage dealt.<br>
         * Also impacts vanilla (jump) critical strikes.
         */
        public static final RegistryObject<Attribute> CRIT_DAMAGE = R.attribute("crit_damage", () -> new PercentBasedAttribute("attributeslib:crit_damage", 1.5D, 1.0D, 100.0D).setSyncable(true));

        /**
         * Bonus physical damage dealt equal to enemy's current health. Base value = (0.0) = 0%
         */
        public static final RegistryObject<Attribute> CURRENT_HP_DAMAGE = R.attribute("current_hp_damage", () -> new PercentBasedAttribute("attributeslib:current_hp_damage", 0.0D, 0.0D, 1.0D).setSyncable(true));

        /**
         * Chance to dodge incoming melee damage. Base value = (0.0) = 0% chance to dodge.<br>
         * "Melee" damage is considered as damage from another entity within the player's attack range.
         * <p>
         * This includes projectile attacks, as long as the projectile actually impacts the player.
         */
        public static final RegistryObject<Attribute> DODGE_CHANCE = R.attribute("dodge_chance", () -> new PercentBasedAttribute("attributeslib:dodge_chance", 0.0D, 0.0D, 1.0D).setSyncable(true));

        /**
         * How fast a ranged weapon is charged. Base Value = (1.0) = 100% default draw speed.
         */
        public static final RegistryObject<Attribute> DRAW_SPEED = R.attribute("draw_speed", () -> new PercentBasedAttribute("attributeslib:draw_speed", 1.0D, 0.0D, 4.0D).setSyncable(true));

        /**
         * Experience mulitplier, from killing mobs or breaking ores. Base value = (1.0) = 100% xp gained.
         */
        public static final RegistryObject<Attribute> EXPERIENCE_GAINED = R.attribute("experience_gained", () -> new PercentBasedAttribute("attributeslib:experience_gained", 1.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Bonus magic damage that burns enemies hit. Base value = (0.0) = 0 damage
         */
        public static final RegistryObject<Attribute> FIRE_DAMAGE = R.attribute("fire_damage", () -> new RangedAttribute("attributeslib:fire_damage", 0.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Extra health that regenerates when not taking damage. Base value = (0.0) = 0 damage
         */
        public static final RegistryObject<Attribute> GHOST_HEALTH = R.attribute("ghost_health", () -> new RangedAttribute("attributeslib:ghost_health", 0.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Adjusts all healing received. Base value = (1.0) = 100% xp gained.
         */
        public static final RegistryObject<Attribute> HEALING_RECEIVED = R.attribute("healing_received", () -> new PercentBasedAttribute("attributeslib:healing_received", 1.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Percent of physical damage converted to health. Base value = (0.0) = 0%
         */
        public static final RegistryObject<Attribute> LIFE_STEAL = R.attribute("life_steal", () -> new PercentBasedAttribute("attributeslib:life_steal", 0.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Mining Speed. Base value = (1.0) = 100% default break speed
         */
        public static final RegistryObject<Attribute> MINING_SPEED = R.attribute("mining_speed", () -> new PercentBasedAttribute("attributeslib:mining_speed", 1.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Percent of physical damage converted to absorption hearts. Base value = (0.0) = 0%
         */
        public static final RegistryObject<Attribute> OVERHEAL = R.attribute("overheal", () -> new PercentBasedAttribute("attributeslib:overheal", 0.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Flat protection penetration. Base value = (0.0) = 0 protection points bypassed during damage calculations.
         */
        public static final RegistryObject<Attribute> PROT_PIERCE = R.attribute("prot_pierce", () -> new RangedAttribute("attributeslib:prot_pierce", 0.0D, 0.0D, 34.0D).setSyncable(true));

        /**
         * Percentage protection reduction. Base value = (0.0) = 0% of protection points bypassed during damage calculations.
         */
        public static final RegistryObject<Attribute> PROT_SHRED = R.attribute("prot_shred", () -> new PercentBasedAttribute("attributeslib:prot_shred", 0.0D, 0.0D, 1.0D).setSyncable(true));

        @ApiStatus.Internal
        public static void bootstrap() {}
    }

    public static class MobEffects {

        /**
         * Bleeding inflicts 1 + level damage every two seconds. Things that apply bleeding usually stack.
         */
        public static final RegistryObject<BleedingEffect> BLEEDING = R.effect("bleeding", BleedingEffect::new);

        /**
         * Flaming Detonation, when it expires, consumes all fire ticks and deals armor-piercing damage based on the duration.
         */
        public static final RegistryObject<DetonationEffect> DETONATION = R.effect("detonation", DetonationEffect::new);

        /**
         * Grievous Wounds reduces healing received by 40%/level.
         */
        public static final RegistryObject<GrievousEffect> GRIEVOUS = R.effect("grievous", GrievousEffect::new);

        /**
         * Ancient Knowledge multiplies experience dropped by mobs by level * {@link MobFxLib#knowledgeMult}.<br>
         * The multiplier is configurable.
         */
        public static final RegistryObject<KnowledgeEffect> KNOWLEDGE = R.effect("knowledge", KnowledgeEffect::new);

        /**
         * Sundering is the inverse of resistance. It increases damage taken by 20%/level.<br>
         * Each point of sundering cancels out a single point of resistance, if present.
         */
        public static final RegistryObject<SunderingEffect> SUNDERING = R.effect("sundering", SunderingEffect::new);

        /**
         * Bursting Vitality increases healing received by 20%/level.
         */
        public static final RegistryObject<VitalityEffect> VITALITY = R.effect("vitality", VitalityEffect::new);

        @ApiStatus.Internal
        public static void bootstrap() {}
    }

    public static class Particles {

        public static final RegistryObject<SimpleParticleType> APOTH_CRIT = R.particle("apoth_crit", () -> new SimpleParticleType(false));

        @ApiStatus.Internal
        public static void bootstrap() {}

    }

    public static class Sounds {

        public static final RegistryObject<SoundEvent> DODGE = R.sound("dodge");

        @ApiStatus.Internal
        public static void bootstrap() {}

    }

    public static class DamageTypes {

        /**
         * Damage type used by {@link MobEffects#BLEEDING}. Bypasses armor.
         */
        public static final ResourceKey<DamageType> BLEEDING = ResourceKey.create(Registries.DAMAGE_TYPE, AttributesLib.loc("bleeding"));

        /**
         * Damage type used by {@link MobEffects#DETONATION}. Bypasses armor, and is marked as magic damage.
         */
        public static final ResourceKey<DamageType> DETONATION = ResourceKey.create(Registries.DAMAGE_TYPE, AttributesLib.loc("detonation"));

        /**
         * Damage type used by {@link Attributes#CURRENT_HP_DAMAGE}. Same properties as generic physical damage. Has attacker context.
         */
        public static final ResourceKey<DamageType> CURRENT_HP_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, AttributesLib.loc("current_hp_damage"));

        /**
         * Damage type used by {@link Attributes#FIRE_DAMAGE}. Bypasses armor, and is marked as magic damage. Has attacker context.<br>
         * Not marked as fire damage until fire resistance is reworked to not block all fire damage.
         */
        public static final ResourceKey<DamageType> FIRE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, AttributesLib.loc("fire_damage"));

        /**
         * Damage type used by {@link Attributes#COLD_DAMAGE}. Bypasses armor, and is marked as magic damage. Has attacker context.
         */
        public static final ResourceKey<DamageType> COLD_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, AttributesLib.loc("cold_damage"));

        @ApiStatus.Internal
        public static void bootstrap() {}
    }

    @ApiStatus.Internal
    public static void bootstrap() {
        Attributes.bootstrap();
        MobEffects.bootstrap();
        Particles.bootstrap();
        Sounds.bootstrap();
        DamageTypes.bootstrap();
    }
}
