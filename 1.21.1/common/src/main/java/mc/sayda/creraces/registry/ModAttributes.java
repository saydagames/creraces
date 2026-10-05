package mc.sayda.creraces.registry;

import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(CreRaces.MODID,
            Registries.ATTRIBUTE);

    // Resources
    private static final RegistrySupplier<Attribute> MAX_MANA_ENTRY = ATTRIBUTES.register("max_mana",
            () -> new RangedAttribute("attribute.creraces.max_mana",
                    CreRacesConfig.DEFAULT_MAX_MANA.get(), 0.0, 1000.0).setSyncable(true));

    private static final RegistrySupplier<Attribute> MAX_RAGE_ENTRY = ATTRIBUTES.register("max_rage",
            () -> new RangedAttribute("attribute.creraces.max_rage",
                    CreRacesConfig.DEFAULT_MAX_RAGE.get(), 0.0, 1000.0).setSyncable(true));

    private static final RegistrySupplier<Attribute> MAX_ENERGY_ENTRY = ATTRIBUTES.register("max_energy",
            () -> new RangedAttribute("attribute.creraces.max_energy",
                    CreRacesConfig.DEFAULT_MAX_ENERGY.get(), 0.0, 1000.0).setSyncable(true));

    private static final RegistrySupplier<Attribute> MAX_GRIT_ENTRY = ATTRIBUTES.register("max_grit",
            () -> new RangedAttribute("attribute.creraces.max_grit",
                    CreRacesConfig.DEFAULT_MAX_GRIT.get(), 0.0, 1000.0).setSyncable(true));

    // RPG stats
    private static final RegistrySupplier<Attribute> ABILITY_POWER_ENTRY = ATTRIBUTES.register("ability_power",
            () -> new RangedAttribute("attribute.creraces.ability_power", 0.0, 0.0, 1000.0).setSyncable(true));

    private static final RegistrySupplier<Attribute> ATTACK_DAMAGE_ENTRY = ATTRIBUTES.register("attack_damage",
            () -> new RangedAttribute("attribute.creraces.attack_damage", 0.0, 0.0, 1000.0).setSyncable(true));

    private static final RegistrySupplier<Attribute> CRIT_RATE_ENTRY = ATTRIBUTES.register("crit_rate",
            () -> new RangedAttribute("attribute.creraces.crit_rate", 0.0, 0.0, 10.0).setSyncable(true));

    /** Percent cooldown reduction. ABILITY_HASTE_CAP is the real cap, so this bound only has to stay above it. */
    private static final RegistrySupplier<Attribute> ABILITY_HASTE_ENTRY = ATTRIBUTES.register("ability_haste",
            () -> new RangedAttribute("attribute.creraces.ability_haste", 0.0, 0.0, 100.0).setSyncable(true));

    // Regeneration and decay
    private static final RegistrySupplier<Attribute> MANA_REGEN_ENTRY = ATTRIBUTES.register("mana_regeneration",
            () -> new RangedAttribute("attribute.creraces.mana_regen", 0.1, 0.0, 1000.0).setSyncable(true));

    private static final RegistrySupplier<Attribute> ENERGY_REGEN_ENTRY = ATTRIBUTES.register("energy_regeneration",
            () -> new RangedAttribute("attribute.creraces.energy_regen", 0.25, 0.0, 1000.0).setSyncable(true));

    private static final RegistrySupplier<Attribute> GRIT_DECAY_ENTRY = ATTRIBUTES.register("grit_decay",
            () -> new RangedAttribute("attribute.creraces.grit_decay", 0.25, 0.0, 1000.0).setSyncable(true));

    private static final RegistrySupplier<Attribute> RAGE_DECAY_ENTRY = ATTRIBUTES.register("rage_decay",
            () -> new RangedAttribute("attribute.creraces.rage_decay", 0.25, 0.0, 1000.0).setSyncable(true));

    private static final RegistrySupplier<Attribute> DOUBLE_JUMP_ENTRY = ATTRIBUTES.register("double_jump",
            () -> new RangedAttribute("attribute.creraces.double_jump", 0.0, 0.0, 10.0).setSyncable(true));

    // LoL-style combat stats. When Apothic Attributes is installed, resolve() swaps in its equivalents.

    /** Multiplier on all incoming healing (1.0 = 100%). */
    private static final RegistrySupplier<Attribute> HEALING_RECEIVED_ENTRY = ATTRIBUTES.register("healing_received",
            () -> new RangedAttribute("attribute.creraces.healing_received", 1.0, 0.0, 100.0).setSyncable(true));

    /** Flat armor penetration: ignores this much of the target's armor. */
    private static final RegistrySupplier<Attribute> ARMOR_PIERCE_ENTRY = ATTRIBUTES.register("armor_pierce",
            () -> new RangedAttribute("attribute.creraces.armor_pierce", 0.0, 0.0, 1000.0).setSyncable(true));

    /** Percentage armor reduction (0.3 shreds 30% of the target's armor). */
    private static final RegistrySupplier<Attribute> ARMOR_SHRED_ENTRY = ATTRIBUTES.register("armor_shred",
            () -> new RangedAttribute("attribute.creraces.armor_shred", 0.0, 0.0, 1.0).setSyncable(true));

    /** Flat magic resistance; magic damage is multiplied by 100 / (100 + MR). */
    private static final RegistrySupplier<Attribute> MAGIC_RESIST_ENTRY = ATTRIBUTES.register("magic_resist",
            () -> new RangedAttribute("attribute.creraces.magic_resist", 0.0, 0.0, 1000.0).setSyncable(true));

    /** Flat magic penetration: ignores this much of the target's magic resistance. */
    private static final RegistrySupplier<Attribute> MAGIC_PIERCE_ENTRY = ATTRIBUTES.register("magic_pierce",
            () -> new RangedAttribute("attribute.creraces.magic_pierce", 0.0, 0.0, 1000.0).setSyncable(true));

    /** Percentage magic penetration (0.3 ignores 30% of the target's magic resistance). */
    private static final RegistrySupplier<Attribute> MAGIC_SHRED_ENTRY = ATTRIBUTES.register("magic_shred",
            () -> new RangedAttribute("attribute.creraces.magic_shred", 0.0, 0.0, 1.0).setSyncable(true));

    // RegistrySupplier implements Holder but is not the registry's own Holder.Reference. Attribute
    // syncing writes the holder through the registry, which rejects anything unregistered, and
    // AttributeSupplier keys on holder identity, so the real holders are bound here once each entry
    // registers and are the ones to use everywhere.
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

    private static boolean initialized = false;

    public static final String APOTHIC_ID = "attributeslib";

    /** The registered holder for an attribute id, or null if it isn't registered. */
    private static Holder<Attribute> holderFor(String namespace, String path) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.fromNamespaceAndPath(namespace, path))
                .map(h -> (Holder<Attribute>) h).orElse(null);
    }

    /**
     * Returns the Apothic Attributes equivalent of a CreRaces attribute when that mod is installed, so
     * traits, scaling and combat all read the same attribute. Falls back to our own otherwise.
     */
    public static Holder<Attribute> resolve(Holder<Attribute> attr) {
        if (attr == null || !Platform.isModLoaded(APOTHIC_ID))
            return attr;

        ResourceLocation id = attr.unwrapKey().map(ResourceKey::location).orElse(null);
        if (id == null || !id.getNamespace().equals(CreRaces.MODID))
            return attr;

        Holder<Attribute> apothic = holderFor(APOTHIC_ID, apothicName(id.getPath()));
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
    public static boolean isPercentAttribute(Holder<Attribute> attr) {
        if (attr == null)
            return false;
        ResourceLocation id = attr.unwrapKey().map(ResourceKey::location).orElse(null);
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
     * mana, energy, rage, grit and crit, then applies {@link #resolve(Holder)}.
     */
    public static Holder<Attribute> getAttribute(ResourceLocation id) {
        if (id == null)
            return null;
        String path = id.getPath().toLowerCase();

        Holder<Attribute> aliased = switch (path) {
            case "max_health", "hp" -> Attributes.MAX_HEALTH;
            case "attack_damage", "ad" -> Attributes.ATTACK_DAMAGE;
            case "movement_speed", "speed" -> Attributes.MOVEMENT_SPEED;
            case "armor" -> Attributes.ARMOR;
            case "luck" -> Attributes.LUCK;
            case "ap", "ability_power" -> ABILITY_POWER;
            case "mana", "max_mana" -> MAX_MANA;
            case "energy", "max_energy" -> MAX_ENERGY;
            case "rage", "max_rage" -> MAX_RAGE;
            case "grit", "max_grit" -> MAX_GRIT;
            case "crit", "crit_rate" -> CRIT_RATE;
            default -> {
                if (path.contains("life_steal") || path.contains("lifesteal")) {
                    Holder<Attribute> lifeSteal = holderFor(APOTHIC_ID, "life_steal");
                    if (lifeSteal == null)
                        lifeSteal = holderFor(APOTHIC_ID, "lifesteal");
                    yield lifeSteal;
                }
                yield null;
            }
        };

        if (aliased != null)
            return resolve(aliased);

        return resolve(holderFor(id.getNamespace(), id.getPath()));
    }

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
