package mc.sayda.creraces.registry;

import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(CreRaces.MODID,
            Registries.ATTRIBUTE);

    // Resources
    public static final RegistrySupplier<Attribute> MAX_MANA = ATTRIBUTES.register("max_mana",
            () -> new RangedAttribute("attribute.creraces.max_mana",
                    CreRacesConfig.DEFAULT_MAX_MANA.get(), 0.0, 1000.0).setSyncable(true));

    public static final RegistrySupplier<Attribute> MAX_RAGE = ATTRIBUTES.register("max_rage",
            () -> new RangedAttribute("attribute.creraces.max_rage",
                    CreRacesConfig.DEFAULT_MAX_RAGE.get(), 0.0, 1000.0).setSyncable(true));

    public static final RegistrySupplier<Attribute> MAX_ENERGY = ATTRIBUTES.register("max_energy",
            () -> new RangedAttribute("attribute.creraces.max_energy",
                    CreRacesConfig.DEFAULT_MAX_ENERGY.get(), 0.0, 1000.0).setSyncable(true));

    public static final RegistrySupplier<Attribute> MAX_GRIT = ATTRIBUTES.register("max_grit",
            () -> new RangedAttribute("attribute.creraces.max_grit",
                    CreRacesConfig.DEFAULT_MAX_GRIT.get(), 0.0, 1000.0).setSyncable(true));

    // RPG stats
    public static final RegistrySupplier<Attribute> ABILITY_POWER = ATTRIBUTES.register("ability_power",
            () -> new RangedAttribute("attribute.creraces.ability_power", 0.0, 0.0, 1000.0).setSyncable(true));

    public static final RegistrySupplier<Attribute> ATTACK_DAMAGE = ATTRIBUTES.register("attack_damage",
            () -> new RangedAttribute("attribute.creraces.attack_damage", 0.0, 0.0, 1000.0).setSyncable(true));

    public static final RegistrySupplier<Attribute> CRIT_RATE = ATTRIBUTES.register("crit_rate",
            () -> new RangedAttribute("attribute.creraces.crit_rate", 0.0, 0.0, 10.0).setSyncable(true));

    /** Percent cooldown reduction. ABILITY_HASTE_CAP is the real cap, so this bound only has to stay above it. */
    public static final RegistrySupplier<Attribute> ABILITY_HASTE = ATTRIBUTES.register("ability_haste",
            () -> new RangedAttribute("attribute.creraces.ability_haste", 0.0, 0.0, 100.0).setSyncable(true));

    // Regeneration and decay
    public static final RegistrySupplier<Attribute> MANA_REGEN = ATTRIBUTES.register("mana_regeneration",
            () -> new RangedAttribute("attribute.creraces.mana_regen", 0.1, 0.0, 1000.0).setSyncable(true));

    public static final RegistrySupplier<Attribute> ENERGY_REGEN = ATTRIBUTES.register("energy_regeneration",
            () -> new RangedAttribute("attribute.creraces.energy_regen", 0.25, 0.0, 1000.0).setSyncable(true));

    public static final RegistrySupplier<Attribute> GRIT_DECAY = ATTRIBUTES.register("grit_decay",
            () -> new RangedAttribute("attribute.creraces.grit_decay", 0.25, 0.0, 1000.0).setSyncable(true));

    public static final RegistrySupplier<Attribute> RAGE_DECAY = ATTRIBUTES.register("rage_decay",
            () -> new RangedAttribute("attribute.creraces.rage_decay", 0.25, 0.0, 1000.0).setSyncable(true));

    public static final RegistrySupplier<Attribute> DOUBLE_JUMP = ATTRIBUTES.register("double_jump",
            () -> new RangedAttribute("attribute.creraces.double_jump", 0.0, 0.0, 10.0).setSyncable(true));

    // LoL-style combat stats. When Apothic Attributes is installed, resolve() swaps in its equivalents.

    /** Multiplier on all incoming healing (1.0 = 100%). */
    public static final RegistrySupplier<Attribute> HEALING_RECEIVED = ATTRIBUTES.register("healing_received",
            () -> new RangedAttribute("attribute.creraces.healing_received", 1.0, 0.0, 100.0).setSyncable(true));

    /** Flat armor penetration: ignores this much of the target's armor. */
    public static final RegistrySupplier<Attribute> ARMOR_PIERCE = ATTRIBUTES.register("armor_pierce",
            () -> new RangedAttribute("attribute.creraces.armor_pierce", 0.0, 0.0, 1000.0).setSyncable(true));

    /** Percentage armor reduction (0.3 shreds 30% of the target's armor). */
    public static final RegistrySupplier<Attribute> ARMOR_SHRED = ATTRIBUTES.register("armor_shred",
            () -> new RangedAttribute("attribute.creraces.armor_shred", 0.0, 0.0, 1.0).setSyncable(true));

    /** Flat magic resistance; magic damage is multiplied by 100 / (100 + MR). */
    public static final RegistrySupplier<Attribute> MAGIC_RESIST = ATTRIBUTES.register("magic_resist",
            () -> new RangedAttribute("attribute.creraces.magic_resist", 0.0, 0.0, 1000.0).setSyncable(true));

    /** Flat magic penetration: ignores this much of the target's magic resistance. */
    public static final RegistrySupplier<Attribute> MAGIC_PIERCE = ATTRIBUTES.register("magic_pierce",
            () -> new RangedAttribute("attribute.creraces.magic_pierce", 0.0, 0.0, 1000.0).setSyncable(true));

    /** Percentage magic penetration (0.3 ignores 30% of the target's magic resistance). */
    public static final RegistrySupplier<Attribute> MAGIC_SHRED = ATTRIBUTES.register("magic_shred",
            () -> new RangedAttribute("attribute.creraces.magic_shred", 0.0, 0.0, 1.0).setSyncable(true));

    private static boolean initialized = false;

    public static final String APOTHIC_ID = "attributeslib";

    /**
     * Returns the Apothic Attributes equivalent of a CreRaces attribute when that mod is installed, so
     * traits, scaling and combat all read the same attribute. Falls back to our own otherwise.
     */
    public static Attribute resolve(RegistrySupplier<Attribute> supplier) {
        if (supplier == null)
            return null;
        if (!Platform.isModLoaded(APOTHIC_ID))
            return supplier.get();

        Attribute apothic = BuiltInRegistries.ATTRIBUTE.get(
                new ResourceLocation(APOTHIC_ID, apothicName(supplier.getId().getPath())));
        return apothic != null ? apothic : supplier.get();
    }

    /** Same as {@link #resolve(RegistrySupplier)}, for an attribute already looked up. */
    public static Attribute resolve(Attribute attr) {
        if (attr == null || !Platform.isModLoaded(APOTHIC_ID))
            return attr;

        ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(attr);
        if (id == null || !id.getNamespace().equals(CreRaces.MODID))
            return attr;

        Attribute apothic = BuiltInRegistries.ATTRIBUTE.get(new ResourceLocation(APOTHIC_ID, apothicName(id.getPath())));
        return apothic != null ? apothic : attr;
    }

    /** Apothic's name for one of our attributes; most share the same path. */
    private static String apothicName(String path) {
        return switch (path) {
            case "crit_rate" -> "crit_chance";
            case "ability_haste" -> "cooldown_reduction";
            case "magic_resist" -> "magic_resistance";
            default -> path;
        };
    }

    /** Whether a resolved attribute is one of Apothic's percentages, stored as 0.0-1.0 rather than 0-100. */
    public static boolean isPercentAttribute(Attribute attr) {
        if (attr == null)
            return false;
        ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(attr);
        if (id == null)
            return false;

        return switch (id.getPath()) {
            case "crit_chance", "cooldown_reduction", "armor_shred", "life_steal", "overheal",
                    "current_hp_damage", "dodge_chance" -> true;
            default -> false;
        };
    }

    /**
     * Resolves an attribute id from JSON, accepting the short aliases hp, ad, ap, speed, armor, luck,
     * mana, energy, rage, grit and crit, then applies {@link #resolve(Attribute)}.
     */
    public static Attribute getAttribute(ResourceLocation id) {
        if (id == null)
            return null;
        String path = id.getPath().toLowerCase();

        Attribute aliased = switch (path) {
            case "max_health", "hp" -> Attributes.MAX_HEALTH;
            case "attack_damage", "ad" -> Attributes.ATTACK_DAMAGE;
            case "movement_speed", "speed" -> Attributes.MOVEMENT_SPEED;
            case "armor" -> Attributes.ARMOR;
            case "luck" -> Attributes.LUCK;
            case "ap", "ability_power" -> ABILITY_POWER.get();
            case "mana", "max_mana" -> MAX_MANA.get();
            case "energy", "max_energy" -> MAX_ENERGY.get();
            case "rage", "max_rage" -> MAX_RAGE.get();
            case "grit", "max_grit" -> MAX_GRIT.get();
            case "crit", "crit_rate" -> CRIT_RATE.get();
            default -> {
                if (path.contains("life_steal") || path.contains("lifesteal")) {
                    Attribute lifeSteal = BuiltInRegistries.ATTRIBUTE.get(new ResourceLocation(APOTHIC_ID, "life_steal"));
                    if (lifeSteal == null)
                        lifeSteal = BuiltInRegistries.ATTRIBUTE.get(new ResourceLocation(APOTHIC_ID, "lifesteal"));
                    yield lifeSteal;
                }
                yield null;
            }
        };

        if (aliased != null)
            return resolve(aliased);

        return resolve(BuiltInRegistries.ATTRIBUTE.getOptional(id).orElse(null));
    }

    public static void init() {
        if (initialized)
            return;
        ATTRIBUTES.register();
        initialized = true;
    }
}
