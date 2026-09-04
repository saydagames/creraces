package mc.sayda.creraces.registry;

import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public class ModAttributes {
        public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(CreRaces.MODID,
                        Registries.ATTRIBUTE);

        // Resources
        private static final RegistrySupplier<Attribute> MAX_MANA_ENTRY = ATTRIBUTES.register("max_mana",
                        () -> new RangedAttribute("attribute.creraces.max_mana",
                                        mc.sayda.creraces.config.CreRacesConfig.DEFAULT_MAX_MANA.get(), 0.0,
                                        1000.0).setSyncable(true));

        private static final RegistrySupplier<Attribute> MAX_RAGE_ENTRY = ATTRIBUTES.register("max_rage",
                        () -> new RangedAttribute("attribute.creraces.max_rage",
                                        mc.sayda.creraces.config.CreRacesConfig.DEFAULT_MAX_RAGE.get(), 0.0,
                                        1000.0)
                                        .setSyncable(true));

        private static final RegistrySupplier<Attribute> MAX_ENERGY_ENTRY = ATTRIBUTES.register("max_energy",
                        () -> new RangedAttribute("attribute.creraces.max_energy",
                                        mc.sayda.creraces.config.CreRacesConfig.DEFAULT_MAX_ENERGY.get(), 0.0,
                                        1000.0).setSyncable(true));

        private static final RegistrySupplier<Attribute> MAX_GRIT_ENTRY = ATTRIBUTES.register("max_grit",
                        () -> new RangedAttribute("attribute.creraces.max_grit",
                                        mc.sayda.creraces.config.CreRacesConfig.DEFAULT_MAX_GRIT.get(), 0.0,
                                        1000.0)
                                        .setSyncable(true));

        // RPG Stats
        private static final RegistrySupplier<Attribute> ABILITY_POWER_ENTRY = ATTRIBUTES.register("ability_power",
                        () -> new RangedAttribute("attribute.creraces.ability_power", 0.0, 0.0,
                                        1000.0) // MAX_STAT_CAP default
                                        .setSyncable(true));

        private static final RegistrySupplier<Attribute> ATTACK_DAMAGE_ENTRY = ATTRIBUTES.register("attack_damage",
                        () -> new RangedAttribute("attribute.creraces.attack_damage", 0.0, 0.0,
                                        1000.0) // MAX_STAT_CAP default
                                        .setSyncable(true));

        private static final RegistrySupplier<Attribute> CRIT_RATE_ENTRY = ATTRIBUTES.register("crit_rate",
                        () -> new RangedAttribute("attribute.creraces.crit_rate", 0.0, 0.0,
                                        10.0)
                                        .setSyncable(true)); // Percentage



        private static final RegistrySupplier<Attribute> ABILITY_HASTE_ENTRY = ATTRIBUTES.register("ability_haste",
                        () -> new RangedAttribute("attribute.creraces.ability_haste", 0.0, 0.0,
                                        10.0)
                                        .setSyncable(true));

        // Regeneration & Decay
        private static final RegistrySupplier<Attribute> MANA_REGEN_ENTRY = ATTRIBUTES.register("mana_regeneration",
                        () -> new RangedAttribute("attribute.creraces.mana_regen", 0.1, 0.0, 1000.0).setSyncable(true));

        private static final RegistrySupplier<Attribute> ENERGY_REGEN_ENTRY = ATTRIBUTES.register("energy_regeneration",
                        () -> new RangedAttribute("attribute.creraces.energy_regen", 0.25, 0.0, 1000.0)
                                        .setSyncable(true));

        private static final RegistrySupplier<Attribute> GRIT_DECAY_ENTRY = ATTRIBUTES.register("grit_decay",
                        () -> new RangedAttribute("attribute.creraces.grit_decay", 0.25, 0.0, 1000.0).setSyncable(true));

        private static final RegistrySupplier<Attribute> RAGE_DECAY_ENTRY = ATTRIBUTES.register("rage_decay",
                        () -> new RangedAttribute("attribute.creraces.rage_decay", 0.25, 0.0, 1000.0)
                                        .setSyncable(true));

        private static final RegistrySupplier<Attribute> DOUBLE_JUMP_ENTRY = ATTRIBUTES.register("double_jump",
                        () -> new RangedAttribute("attribute.creraces.double_jump", 0.0, 0.0, 10.0)
                                        .setSyncable(true));

        // LoL-Style Combat Stats (Fallback when Apothic Attributes is absent)
        // On Forge with Apothic, CombatAttributes.java will redirect these to attributeslib equivalents.

        /** Modifier for all incoming healing (1.0 = 100%). Grievous Wounds sets this to 0.6. */
        private static final RegistrySupplier<Attribute> HEALING_RECEIVED_ENTRY = ATTRIBUTES.register("healing_received",
                        () -> new RangedAttribute("attribute.creraces.healing_received", 1.0, 0.0, 100.0)
                                        .setSyncable(true));

        /** Flat armor reduction (bypasses target's armor by this amount). */
        private static final RegistrySupplier<Attribute> ARMOR_PIERCE_ENTRY = ATTRIBUTES.register("armor_pierce",
                        () -> new RangedAttribute("attribute.creraces.armor_pierce", 0.0, 0.0, 1000.0)
                                        .setSyncable(true));

        /** Percentage armor reduction (0.3 = shreds 30% of target's armor before reduction). */
        private static final RegistrySupplier<Attribute> ARMOR_SHRED_ENTRY = ATTRIBUTES.register("armor_shred",
                        () -> new RangedAttribute("attribute.creraces.armor_shred", 0.0, 0.0, 1.0)
                                        .setSyncable(true));

        /** Flat magic resistance; reduces incoming magical damage via LoL formula. */
        private static final RegistrySupplier<Attribute> MAGIC_RESIST_ENTRY = ATTRIBUTES.register("magic_resist",
                        () -> new RangedAttribute("attribute.creraces.magic_resist", 0.0, 0.0, 1000.0)
                                        .setSyncable(true));

        /** Flat magic penetration; reduces target's MR by this amount. */
        private static final RegistrySupplier<Attribute> MAGIC_PIERCE_ENTRY = ATTRIBUTES.register("magic_pierce",
                        () -> new RangedAttribute("attribute.creraces.magic_pierce", 0.0, 0.0, 1000.0)
                                        .setSyncable(true));

        /** Percentage magic penetration (0.3 = ignores 30% of target's MR). */
        private static final RegistrySupplier<Attribute> MAGIC_SHRED_ENTRY = ATTRIBUTES.register("magic_shred",
                        () -> new RangedAttribute("attribute.creraces.magic_shred", 0.0, 0.0, 1.0)
                                        .setSyncable(true));

        private static boolean initialized = false;

        public static final String APOTHIC_ID = "attributeslib";

        /**
         * Looks up a Holder for a namespaced attribute id, or null if it isn't registered.
         */
        private static Holder<Attribute> holderFor(String namespace, String path) {
                return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.fromNamespaceAndPath(namespace, path))
                                .map(h -> (Holder<Attribute>) h).orElse(null);
        }

        /**
         * Resolves a CreRaces attribute to its Apothic Attributes equivalent if the mod is present.
         * This ensures that all components (Traits, Scaling, Combat) point to the same global attribute.
         */
        public static Holder<Attribute> resolve(RegistrySupplier<Attribute> supplier) {
                if (supplier == null)
                        return null;
                if (!Platform.isModLoaded(APOTHIC_ID))
                        return supplier; // RegistrySupplier<Attribute> implements Holder<Attribute>.

                String internalName = supplier.getId().getPath();
                String apothicName = switch (internalName) {
                        case "crit_rate" -> "crit_chance";
                        case "ability_haste" -> "cooldown_reduction";

                        case "attack_damage" -> "attack_damage";
                        case "magic_resist" -> "magic_resistance";
                        case "healing_received" -> "healing_received";
                        default -> internalName;
                };

                Holder<Attribute> attr = holderFor(APOTHIC_ID, apothicName);
                return attr != null ? attr : supplier;
        }

        /**
         * Resolves a raw Attribute Holder to its Apothic version by checking its registry key.
         */
        public static Holder<Attribute> resolve(Holder<Attribute> attr) {
                if (attr == null || !Platform.isModLoaded(APOTHIC_ID))
                        return attr;

                ResourceLocation id = attr.unwrapKey().map(ResourceKey::location).orElse(null);
                if (id == null || !id.getNamespace().equals(CreRaces.MODID))
                        return attr;

                String path = id.getPath();
                String apothicName = switch (path) {
                        case "crit_rate" -> "crit_chance";
                        case "ability_haste" -> "cooldown_reduction";

                        case "magic_resist" -> "magic_resistance";
                        case "healing_received" -> "healing_received";
                        default -> path;
                };

                Holder<Attribute> apothicAttr = holderFor(APOTHIC_ID, apothicName);
                return apothicAttr != null ? apothicAttr : attr;
        }

        /**
         * Checks if the given resolved Attribute is a percentage-based attribute in AttributesLib
         * that uses 0.0-1.0 range instead of 0-100.
         */
        public static boolean isPercentAttribute(Holder<Attribute> attr) {
                if (attr == null)
                        return false;
                ResourceLocation id = attr.unwrapKey().map(ResourceKey::location).orElse(null);
                if (id == null) return false;

                String path = id.getPath();
                return switch (path) {
                        case "crit_chance", "cooldown_reduction", "armor_shred", "life_steal", "overheal", "current_hp_damage", "dodge_chance" -> true;
                        default -> false;
                };
        }

        /**
         * Resolves a ResourceLocation (often from JSON) to an Attribute, supporting aliases.
         * Aliases: hp, ad, ap, speed, armor, luck, grit, rage, mana, energy, crit.
         */
        public static Holder<Attribute> getAttribute(ResourceLocation id) {
                if (id == null)
                        return null;
                String path = id.getPath().toLowerCase();

                // 1. Resolve Aliases (Shortnames)
                Holder<Attribute> aliased = switch (path) {
                        case "max_health", "hp" -> net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH;
                        case "attack_damage", "ad" -> net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE;
                        case "movement_speed", "speed" -> net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED;
                        case "armor" -> net.minecraft.world.entity.ai.attributes.Attributes.ARMOR;
                        case "luck" -> net.minecraft.world.entity.ai.attributes.Attributes.LUCK;
                        case "ap", "ability_power" -> ABILITY_POWER;
                        case "mana", "max_mana" -> MAX_MANA;
                        case "energy", "max_energy" -> MAX_ENERGY;
                        case "rage", "max_rage" -> MAX_RAGE;
                        case "grit", "max_grit" -> MAX_GRIT;
                        case "crit", "crit_rate" -> CRIT_RATE;
                        default -> {
                                if (path.contains("life_steal") || path.contains("lifesteal")) {
                                        Holder<Attribute> ls = holderFor(APOTHIC_ID, "life_steal");
                                        if (ls == null)
                                                ls = holderFor(APOTHIC_ID, "lifesteal");
                                        yield ls;
                                }
                                yield null;
                        }
                };

                if (aliased != null)
                        return resolve(aliased);

                // 2. Direct registry lookup + Apothic resolution
                Holder<Attribute> direct = holderFor(id.getNamespace(), id.getPath());
                return resolve(direct);
        }


        // Architectury's RegistrySupplier implements Holder, but it is not the registry's own
        // reference holder. Attribute values are synced to the client by writing that holder
        // through the registry, which only accepts a registered one, so bind the real holders
        // here and use them everywhere (AttributeSupplier keys off holder identity too).
        public static Holder<Attribute> MAX_MANA;
        public static Holder<Attribute> MAX_RAGE;
        public static Holder<Attribute> MAX_ENERGY;
        public static Holder<Attribute> MAX_GRIT;
        public static Holder<Attribute> ABILITY_POWER;
        public static Holder<Attribute> ATTACK_DAMAGE;
        public static Holder<Attribute> CRIT_RATE;
        public static Holder<Attribute> ABILITY_HASTE;
        public static Holder<Attribute> MANA_REGEN;
        public static Holder<Attribute> ENERGY_REGEN;
        public static Holder<Attribute> GRIT_DECAY;
        public static Holder<Attribute> RAGE_DECAY;
        public static Holder<Attribute> DOUBLE_JUMP;
        public static Holder<Attribute> HEALING_RECEIVED;
        public static Holder<Attribute> ARMOR_PIERCE;
        public static Holder<Attribute> ARMOR_SHRED;
        public static Holder<Attribute> MAGIC_RESIST;
        public static Holder<Attribute> MAGIC_PIERCE;
        public static Holder<Attribute> MAGIC_SHRED;

        public static void init() {
                if (initialized)
                        return;
                ATTRIBUTES.register();
                MAX_MANA_ENTRY.listen(v -> MAX_MANA = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                MAX_RAGE_ENTRY.listen(v -> MAX_RAGE = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                MAX_ENERGY_ENTRY.listen(v -> MAX_ENERGY = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                MAX_GRIT_ENTRY.listen(v -> MAX_GRIT = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                ABILITY_POWER_ENTRY.listen(v -> ABILITY_POWER = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                ATTACK_DAMAGE_ENTRY.listen(v -> ATTACK_DAMAGE = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                CRIT_RATE_ENTRY.listen(v -> CRIT_RATE = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                ABILITY_HASTE_ENTRY.listen(v -> ABILITY_HASTE = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                MANA_REGEN_ENTRY.listen(v -> MANA_REGEN = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                ENERGY_REGEN_ENTRY.listen(v -> ENERGY_REGEN = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                GRIT_DECAY_ENTRY.listen(v -> GRIT_DECAY = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                RAGE_DECAY_ENTRY.listen(v -> RAGE_DECAY = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                DOUBLE_JUMP_ENTRY.listen(v -> DOUBLE_JUMP = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                HEALING_RECEIVED_ENTRY.listen(v -> HEALING_RECEIVED = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                ARMOR_PIERCE_ENTRY.listen(v -> ARMOR_PIERCE = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                ARMOR_SHRED_ENTRY.listen(v -> ARMOR_SHRED = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                MAGIC_RESIST_ENTRY.listen(v -> MAGIC_RESIST = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                MAGIC_PIERCE_ENTRY.listen(v -> MAGIC_PIERCE = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                MAGIC_SHRED_ENTRY.listen(v -> MAGIC_SHRED = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(v));
                initialized = true;
        }
}
