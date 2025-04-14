package dev.shadowsoffire.apothic_attributes.api;

import static dev.shadowsoffire.apothic_attributes.ApothicAttributes.R;

import java.util.function.Supplier;

import org.jetbrains.annotations.ApiStatus;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.mob_effect.BleedingEffect;
import dev.shadowsoffire.apothic_attributes.mob_effect.DetonationEffect;
import dev.shadowsoffire.apothic_attributes.mob_effect.FlyingEffect;
import dev.shadowsoffire.apothic_attributes.mob_effect.GrievousEffect;
import dev.shadowsoffire.apothic_attributes.mob_effect.KnowledgeEffect;
import dev.shadowsoffire.apothic_attributes.mob_effect.SunderingEffect;
import dev.shadowsoffire.apothic_attributes.mob_effect.VitalityEffect;
import dev.shadowsoffire.apothic_attributes.modifiers.EntityEquipmentSlot;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import dev.shadowsoffire.apothic_attributes.modifiers.StackAttributeModifiers;
import dev.shadowsoffire.apothic_attributes.modifiers.VanillaEquipmentSlot;
import dev.shadowsoffire.apothic_attributes.util.AuxDmgTracker;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.BooleanAttribute;
import net.neoforged.neoforge.common.PercentageAttribute;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.registries.holdersets.AnyHolderSet;

public class ALObjects {

    public static class BuiltInRegs {

        public static final Registry<EntityEquipmentSlot> ENTITY_EQUIPMENT_SLOT = R.registry("entity_equipment_slot", c -> c.sync(true));
        public static final Registry<EntitySlotGroup> ENTITY_SLOT_GROUP = R.registry("entity_slot_group", c -> c.sync(true));

        private static void bootstrap() {}
    }

    public static class Attributes {

        /**
         * Flat armor penetration. Base value = (0.0) = 0 armor reduced during damage calculations.
         */
        public static final Holder<Attribute> ARMOR_PIERCE = R.attribute("armor_pierce", () -> new RangedAttribute("apothic_attributes:armor_pierce", 0.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Percentage armor reduction. Base value = (0.0) = 0% of armor reduced during damage calculations.
         */
        public static final Holder<Attribute> ARMOR_SHRED = R.attribute("armor_shred", () -> new PercentageAttribute("apothic_attributes:armor_shred", 0.0D, 0.0D, 2.0D).setSyncable(true));

        /**
         * Arrow Damage. Base value = (1.0) = 100% default arrow damage
         */
        public static final Holder<Attribute> ARROW_DAMAGE = R.attribute("arrow_damage", () -> new PercentageAttribute("apothic_attributes:arrow_damage", 1.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Arrow Velocity. Base value = (1.0) = 100% default arrow velocity
         * <p>
         * Arrow damage scales with the velocity as well as {@link #ARROW_DAMAGE} and the base damage of the arrow entity.
         */
        public static final Holder<Attribute> ARROW_VELOCITY = R.attribute("arrow_velocity", () -> new PercentageAttribute("apothic_attributes:arrow_velocity", 1.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Bonus magic damage that slows enemies hit. Base value = (0.0) = 0 damage
         */
        public static final Holder<Attribute> COLD_DAMAGE = R.attribute("cold_damage", () -> new RangedAttribute("apothic_attributes:cold_damage", 0.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Chance that any attack will critically strike. Base value = (0.05) = 5% chance to critically strike.
         * <p>
         * Not related to vanilla (jump) critical strikes.
         */
        public static final Holder<Attribute> CRIT_CHANCE = R.attribute("crit_chance", () -> new PercentageAttribute("apothic_attributes:crit_chance", 0.05D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Amount of damage caused by critical strikes. Base value = (1.5) = 150% normal damage dealt.
         * <p>
         * Also impacts vanilla (jump) critical strikes.
         */
        public static final Holder<Attribute> CRIT_DAMAGE = R.attribute("crit_damage", () -> new PercentageAttribute("apothic_attributes:crit_damage", 1.5D, 1.0D, 100.0D).setSyncable(true));

        /**
         * Bonus physical damage dealt equal to enemy's current health. Base value = (0.0) = 0%
         */
        public static final Holder<Attribute> CURRENT_HP_DAMAGE = R.attribute("current_hp_damage", () -> new PercentageAttribute("apothic_attributes:current_hp_damage", 0.0D, 0.0D, 1.0D).setSyncable(true));

        /**
         * Chance to dodge incoming melee damage. Base value = (0.0) = 0% chance to dodge.
         * "Melee" damage is considered as damage from another entity within the player's attack range.
         * <p>
         * This includes projectile attacks, as long as the projectile actually impacts the player.
         */
        public static final Holder<Attribute> DODGE_CHANCE = R.attribute("dodge_chance", () -> new PercentageAttribute("apothic_attributes:dodge_chance", 0.0D, 0.0D, 1.0D).setSyncable(true));

        /**
         * How fast a ranged weapon is charged. Base Value = (1.0) = 100% default draw speed.
         */
        public static final Holder<Attribute> DRAW_SPEED = R.attribute("draw_speed", () -> new PercentageAttribute("apothic_attributes:draw_speed", 1.0D, 0.0D, 4.0D).setSyncable(true));

        /**
         * Experience mulitplier, from killing mobs or breaking ores. Base value = (1.0) = 100% xp gained.
         */
        public static final Holder<Attribute> EXPERIENCE_GAINED = R.attribute("experience_gained", () -> new PercentageAttribute("apothic_attributes:experience_gained", 1.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Bonus magic damage that burns enemies hit. Base value = (0.0) = 0 damage
         */
        public static final Holder<Attribute> FIRE_DAMAGE = R.attribute("fire_damage", () -> new RangedAttribute("apothic_attributes:fire_damage", 0.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Extra health that regenerates when not taking damage. Base value = (0.0) = 0 damage
         */
        public static final Holder<Attribute> GHOST_HEALTH = R.attribute("ghost_health", () -> new RangedAttribute("apothic_attributes:ghost_health", 0.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Adjusts all healing received. Base value = (1.0) = 100% incoming healing.
         */
        public static final Holder<Attribute> HEALING_RECEIVED = R.attribute("healing_received", () -> new PercentageAttribute("apothic_attributes:healing_received", 1.0D, 0.0D, 1000.0D).setSyncable(true));

        /**
         * Percent of physical damage dealt converted to health. Base value = (0.0) = 0%
         */
        public static final Holder<Attribute> LIFE_STEAL = R.attribute("life_steal", () -> new PercentageAttribute("apothic_attributes:life_steal", 0.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Mining Speed. Base value = (1.0) = 100% default break speed
         */
        public static final Holder<Attribute> MINING_SPEED = R.attribute("mining_speed", () -> new PercentageAttribute("apothic_attributes:mining_speed", 1.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Percent of physical damage converted to absorption hearts. Base value = (0.0) = 0%
         */
        public static final Holder<Attribute> OVERHEAL = R.attribute("overheal", () -> new PercentageAttribute("apothic_attributes:overheal", 0.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Generic Projectile Damage. Base value = (1.0) = 100% default projectile damage.
         * <p>
         * Applies to any incoming damage where the direct attacking entity is a {@link Projectile}.
         */
        public static final Holder<Attribute> PROJECTILE_DAMAGE = R.attribute("projectile_damage", () -> new PercentageAttribute("apothic_attributes:projectile_damage", 1.0D, 0.0D, 10.0D).setSyncable(true));

        /**
         * Flat protection penetration. Base value = (0.0) = 0 protection points bypassed during damage calculations.
         */
        public static final Holder<Attribute> PROT_PIERCE = R.attribute("prot_pierce", () -> new RangedAttribute("apothic_attributes:prot_pierce", 0.0D, 0.0D, 34.0D).setSyncable(true));

        /**
         * Percentage protection reduction. Base value = (0.0) = 0% of protection points bypassed during damage calculations.
         */
        public static final Holder<Attribute> PROT_SHRED = R.attribute("prot_shred", () -> new PercentageAttribute("apothic_attributes:prot_shred", 0.0D, 0.0D, 1.0D).setSyncable(true));

        /**
         * Boolean attribute for if elytra flight is enabled. Default value = false.
         */
        public static final Holder<Attribute> ELYTRA_FLIGHT = R.attribute("elytra_flight", () -> new BooleanAttribute("apothic_attributes:elytra_flight", false).setSyncable(true));

        private static void bootstrap() {}
    }

    public static class MobEffects extends net.minecraft.world.effect.MobEffects { // Hack to bring vanilla things in-scope

        /**
         * Bleeding inflicts 1 + level damage every two seconds. Things that apply bleeding usually stack.
         */
        public static final Holder<MobEffect> BLEEDING = R.effect("bleeding", BleedingEffect::new);

        /**
         * Flaming Detonation, when it expires, consumes all fire ticks and deals armor-piercing damage based on the duration.
         */
        public static final Holder<MobEffect> DETONATION = R.effect("detonation", DetonationEffect::new);

        /**
         * Grievous Wounds reduces healing received by 40%/level.
         */
        public static final Holder<MobEffect> GRIEVOUS = R.effect("grievous", GrievousEffect::new);

        /**
         * Ancient Knowledge multiplies experience dropped by mobs by level * {@link MobFxLib#knowledgeMult}.<br>
         * The multiplier is configurable.
         */
        public static final Holder<MobEffect> KNOWLEDGE = R.effect("knowledge", KnowledgeEffect::new);

        /**
         * Sundering is the inverse of resistance. It increases damage taken by 20%/level.<br>
         * Each point of sundering cancels out a single point of resistance, if present.
         */
        public static final Holder<MobEffect> SUNDERING = R.effect("sundering", SunderingEffect::new);

        /**
         * Bursting Vitality increases healing received by 20%/level.
         */
        public static final Holder<MobEffect> VITALITY = R.effect("vitality", VitalityEffect::new);

        /**
         * Grants Creative Flight
         */
        public static final Holder<MobEffect> FLYING = R.effect("flying", FlyingEffect::new);

        private static void bootstrap() {}
    }

    public static class Particles {

        public static final Supplier<SimpleParticleType> APOTH_CRIT = R.particle("apoth_crit", () -> new SimpleParticleType(false));

        private static void bootstrap() {}

    }

    public static class Sounds {

        public static final Holder<SoundEvent> DODGE = R.sound("dodge");

        private static void bootstrap() {}

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

        private static void bootstrap() {}
    }

    public static final class Potions {
        public static final Holder<Potion> RESISTANCE = R.singlePotion("resistance", () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 3600));
        public static final Holder<Potion> LONG_RESISTANCE = R.singlePotion("long_resistance", () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 9600));
        public static final Holder<Potion> STRONG_RESISTANCE = R.singlePotion("strong_resistance", () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1800, 1));

        public static final Holder<Potion> ABSORPTION = R.singlePotion("absorption", () -> new MobEffectInstance(MobEffects.ABSORPTION, 1200, 1));
        public static final Holder<Potion> LONG_ABSORPTION = R.singlePotion("long_absorption", () -> new MobEffectInstance(MobEffects.ABSORPTION, 3600, 1));
        public static final Holder<Potion> STRONG_ABSORPTION = R.singlePotion("strong_absorption", () -> new MobEffectInstance(MobEffects.ABSORPTION, 600, 3));

        public static final Holder<Potion> HASTE = R.singlePotion("haste", () -> new MobEffectInstance(MobEffects.DIG_SPEED, 3600));
        public static final Holder<Potion> LONG_HASTE = R.singlePotion("long_haste", () -> new MobEffectInstance(MobEffects.DIG_SPEED, 9600));
        public static final Holder<Potion> STRONG_HASTE = R.singlePotion("strong_haste", () -> new MobEffectInstance(MobEffects.DIG_SPEED, 1800, 1));

        public static final Holder<Potion> FATIGUE = R.singlePotion("fatigue", () -> new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 3600));
        public static final Holder<Potion> LONG_FATIGUE = R.singlePotion("long_fatigue", () -> new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 9600));
        public static final Holder<Potion> STRONG_FATIGUE = R.singlePotion("strong_fatigue", () -> new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 1800, 1));

        public static final Holder<Potion> WITHER = R.singlePotion("wither", () -> new MobEffectInstance(MobEffects.WITHER, 3600));
        public static final Holder<Potion> LONG_WITHER = R.singlePotion("long_wither", () -> new MobEffectInstance(MobEffects.WITHER, 9600));
        public static final Holder<Potion> STRONG_WITHER = R.singlePotion("strong_wither", () -> new MobEffectInstance(MobEffects.WITHER, 1800, 1));

        public static final Holder<Potion> SUNDERING = R.singlePotion("sundering", () -> new MobEffectInstance(MobEffects.SUNDERING, 3600));
        public static final Holder<Potion> LONG_SUNDERING = R.singlePotion("long_sundering", () -> new MobEffectInstance(MobEffects.SUNDERING, 9600));
        public static final Holder<Potion> STRONG_SUNDERING = R.singlePotion("strong_sundering", () -> new MobEffectInstance(MobEffects.SUNDERING, 1800, 1));

        public static final Holder<Potion> KNOWLEDGE = R.singlePotion("knowledge", () -> new MobEffectInstance(MobEffects.KNOWLEDGE, 2400));
        public static final Holder<Potion> LONG_KNOWLEDGE = R.singlePotion("long_knowledge", () -> new MobEffectInstance(ALObjects.MobEffects.KNOWLEDGE, 4800));
        public static final Holder<Potion> STRONG_KNOWLEDGE = R.singlePotion("strong_knowledge", () -> new MobEffectInstance(ALObjects.MobEffects.KNOWLEDGE, 1200, 3));

        public static final Holder<Potion> VITALITY = R.singlePotion("vitality", () -> new MobEffectInstance(ALObjects.MobEffects.VITALITY, 4800));
        public static final Holder<Potion> LONG_VITALITY = R.singlePotion("long_vitality", () -> new MobEffectInstance(ALObjects.MobEffects.VITALITY, 14400));
        public static final Holder<Potion> STRONG_VITALITY = R.singlePotion("strong_vitality", () -> new MobEffectInstance(ALObjects.MobEffects.VITALITY, 3600, 1));

        public static final Holder<Potion> GRIEVOUS = R.singlePotion("grievous", () -> new MobEffectInstance(MobEffects.GRIEVOUS, 4800));
        public static final Holder<Potion> LONG_GRIEVOUS = R.singlePotion("long_grievous", () -> new MobEffectInstance(MobEffects.GRIEVOUS, 14400));
        public static final Holder<Potion> STRONG_GRIEVOUS = R.singlePotion("strong_grievous", () -> new MobEffectInstance(MobEffects.GRIEVOUS, 3600, 1));

        public static final Holder<Potion> LEVITATION = R.singlePotion("levitation", () -> new MobEffectInstance(MobEffects.LEVITATION, 2400));

        public static final Holder<Potion> FLYING = R.singlePotion("flying", () -> new MobEffectInstance(MobEffects.FLYING, 9600));
        public static final Holder<Potion> LONG_FLYING = R.singlePotion("long_flying", () -> new MobEffectInstance(MobEffects.FLYING, 18000));
        public static final Holder<Potion> EXTRA_LONG_FLYING = R.singlePotion("extra_long_flying", () -> new MobEffectInstance(MobEffects.FLYING, 36000));

        private static void bootstrap() {}
    }

    public static class Components {

        /**
         * @deprecated Prefer {@link #BONUS_STACK_ATTRIBUTE_MODIFIERS}.
         */
        @Deprecated(forRemoval = true)
        public static final DataComponentType<ItemAttributeModifiers> BONUS_ATTRIBUTE_MODIFIERS = R.component("bonus_attribute_modifiers",
            builder -> builder.persistent(ItemAttributeModifiers.CODEC).networkSynchronized(ItemAttributeModifiers.STREAM_CODEC).cacheEncoding());

        public static final DataComponentType<StackAttributeModifiers> BONUS_STACK_ATTRIBUTE_MODIFIERS = R.component("bonus_stack_attribute_modifiers",
            builder -> builder.persistent(StackAttributeModifiers.CODEC).networkSynchronized(StackAttributeModifiers.STREAM_CODEC).cacheEncoding());

        private static void bootstrap() {}
    }

    public static class Attachments {

        /**
         * Records the amount of health an entity had during the {@link LivingDamageEvent.Pre} for use in {@link LivingDamageEvent.Post}.
         * <p>
         * This allows effects to be computed based on the actual amount of damage taken.
         */
        public static final AttachmentType<Float> PRE_DAMAGE_HEALTH = R.attachment("pre_damage_health", (holder) -> 0F, b -> b);

        /**
         * Records invulnerability times and last hurt amounts for the auxiliary damage types.
         * <p>
         * This allows us to keep track of these damages on their own, without merging them with other damage types.
         */
        public static final AttachmentType<AuxDmgTracker> AUX_DMG_TRACKER = R.attachment("aux_dmg_tracker", () -> new AuxDmgTracker(), b -> b
            .serialize(AuxDmgTracker.CODEC));

        private static void bootstrap() {}
    }

    public static class Tags {

        /**
         * An attribute with a dynamic base cannot have its value computed out of context, and is instead treated as a list of modifiers
         * that will be applied when the event occurs. The applied modifiers will use the normal rules of {@link Operation} but on the dynamic base.
         */
        public static final TagKey<Attribute> DYNAMIC_BASE_ATTRIBUTES = TagKey.create(Registries.ATTRIBUTE, ApothicAttributes.loc("dynamic_base"));

        /**
         * This tag is a joined tag of all non-physical damage type tags (magic, fire, lightning, explosions, etc).
         * <p>
         * It is used to determine if a damage type is physical or not.
         */
        public static final TagKey<DamageType> IS_NON_PHYSICAL = TagKey.create(Registries.DAMAGE_TYPE, ApothicAttributes.loc("is_non_physical"));

        /**
         * Damage Types with this tag are immune from any processing by {@link Attributes#CRIT_CHANCE} and {@link Attributes#CRIT_DAMAGE}.
         */
        public static final TagKey<DamageType> CANNOT_CRITICALLY_STRIKE = TagKey.create(Registries.DAMAGE_TYPE, ApothicAttributes.loc("cannot_critically_strike"));
    }

    @ApiStatus.Experimental
    public static class EquipmentSlots {
        public static final Holder<EntityEquipmentSlot> MAINHAND = slot(EquipmentSlot.MAINHAND);
        public static final Holder<EntityEquipmentSlot> OFFHAND = slot(EquipmentSlot.OFFHAND);
        public static final Holder<EntityEquipmentSlot> HEAD = slot(EquipmentSlot.HEAD);
        public static final Holder<EntityEquipmentSlot> CHEST = slot(EquipmentSlot.CHEST);
        public static final Holder<EntityEquipmentSlot> LEGS = slot(EquipmentSlot.LEGS);
        public static final Holder<EntityEquipmentSlot> FEET = slot(EquipmentSlot.FEET);
        public static final Holder<EntityEquipmentSlot> BODY = slot(EquipmentSlot.BODY);

        private static Holder<EntityEquipmentSlot> slot(EquipmentSlot slot) {
            return R.<EntityEquipmentSlot, EntityEquipmentSlot>custom(slot.getSerializedName(), BuiltInRegs.ENTITY_EQUIPMENT_SLOT.key(), () -> new VanillaEquipmentSlot(slot));
        }

        private static void bootstrap() {}
    }

    @ApiStatus.Experimental
    public static class EquipmentSlotGroups {
        /**
         * True "any" slot group. Matches any registered {@link EntityEquipmentSlot}.
         */
        public static final EntitySlotGroup ANY = group("any", new AnyHolderSet<>(BuiltInRegs.ENTITY_EQUIPMENT_SLOT.asLookup()));

        /**
         * Vanilla "any" slot group, corresponding to {@link EquipmentSlotGroup#ANY}.
         */
        public static final EntitySlotGroup ANY_VANILLA = group("any_vanilla", HolderSet.direct(
            EquipmentSlots.MAINHAND, EquipmentSlots.OFFHAND, EquipmentSlots.HEAD,
            EquipmentSlots.CHEST, EquipmentSlots.LEGS, EquipmentSlots.FEET, EquipmentSlots.BODY));

        public static final EntitySlotGroup MAINHAND = group("mainhand", HolderSet.direct(EquipmentSlots.MAINHAND));
        public static final EntitySlotGroup OFFHAND = group("offhand", HolderSet.direct(EquipmentSlots.OFFHAND));
        public static final EntitySlotGroup HAND = group("hand", HolderSet.direct(EquipmentSlots.MAINHAND, EquipmentSlots.OFFHAND)); // TODO: Tag key for this?
        public static final EntitySlotGroup HEAD = group("head", HolderSet.direct(EquipmentSlots.HEAD));
        public static final EntitySlotGroup CHEST = group("chest", HolderSet.direct(EquipmentSlots.CHEST));
        public static final EntitySlotGroup LEGS = group("legs", HolderSet.direct(EquipmentSlots.LEGS));
        public static final EntitySlotGroup FEET = group("feet", HolderSet.direct(EquipmentSlots.FEET));
        public static final EntitySlotGroup ARMOR = group("armor", HolderSet.direct(EquipmentSlots.HEAD, EquipmentSlots.CHEST, EquipmentSlots.LEGS, EquipmentSlots.FEET));
        public static final EntitySlotGroup BODY = group("body", HolderSet.direct(EquipmentSlots.BODY));

        private static ResourceLocation id(String path) {
            return ApothicAttributes.loc(path);
        }

        private static EntitySlotGroup group(String path, HolderSet<EntityEquipmentSlot> slots) {
            return R.custom(path, BuiltInRegs.ENTITY_SLOT_GROUP.key(), new EntitySlotGroup(id(path), slots));
        }

        private static void bootstrap() {}
    }

    @ApiStatus.Internal
    public static void bootstrap(IEventBus bus) {
        BuiltInRegs.bootstrap();
        Attributes.bootstrap();
        MobEffects.bootstrap();
        Particles.bootstrap();
        Sounds.bootstrap();
        DamageTypes.bootstrap();
        Potions.bootstrap();
        Components.bootstrap();
        Attachments.bootstrap();
        EquipmentSlots.bootstrap();
        EquipmentSlotGroups.bootstrap();
        bus.register(R);
    }
}
