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

    /**
     * Critical strike chance (0.25 = 25%), 5% by default as in Apothic Attributes. Above 1.0 a hit can
     * crit more than once. See CriticalStrikes.
     */
    public static final RegistrySupplier<Attribute> CRIT_RATE = ATTRIBUTES.register("crit_rate",
            () -> new RangedAttribute("attribute.creraces.crit_rate", 0.05, 0.0, 10.0).setSyncable(true));

    /** Damage multiplier of a critical strike (1.5 = 150%). */
    public static final RegistrySupplier<Attribute> CRIT_DAMAGE = ATTRIBUTES.register("crit_damage",
            () -> new RangedAttribute("attribute.creraces.crit_damage", 1.5, 1.0, 100.0).setSyncable(true));

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

    public static boolean isApothicLoaded() {
        return Platform.isModLoaded(APOTHIC_ID);
    }

    /**
     * Returns the Apothic Attributes equivalent of a CreRaces attribute when that mod is installed, so
     * traits, scaling and combat all read the same attribute. Falls back to our own otherwise.
     */
    public static Attribute resolve(RegistrySupplier<Attribute> supplier) {
        if (supplier == null)
            return null;
        if (!isApothicLoaded())
            return supplier.get();

        Attribute apothic = BuiltInRegistries.ATTRIBUTE.get(
                new ResourceLocation(APOTHIC_ID, apothicName(supplier.getId().getPath())));
        return apothic != null ? apothic : supplier.get();
    }

    /** Same as {@link #resolve(RegistrySupplier)}, for an attribute already looked up. */
    public static Attribute resolve(Attribute attr) {
        if (attr == null || !isApothicLoaded())
            return attr;

        ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(attr);
        if (id == null || !id.getNamespace().equals(CreRaces.MODID))
            return attr;

        Attribute apothic = BuiltInRegistries.ATTRIBUTE.get(new ResourceLocation(APOTHIC_ID, apothicName(id.getPath())));
        return apothic != null ? apothic : attr;
    }

    /** Apothic's name for one of our attributes; most share the same path. */
    private static String apothicName(String path) {
        return path.equals("crit_rate") ? "crit_chance" : path;
    }

    /** Whether a resolved attribute is a percentage stored as 0.0-1.0 rather than 0-100 (crit rate and Apothic's). */
    public static boolean isPercentAttribute(Attribute attr) {
        if (attr == null)
            return false;
        ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(attr);
        if (id == null)
            return false;

        return switch (id.getPath()) {
            case "crit_rate", "crit_chance", "armor_shred", "prot_shred", "life_steal", "overheal",
                    "current_hp_damage", "dodge_chance" -> true;
            default -> false;
        };
    }

    /** Looks up an attribute by its exact id, swapped for its Apothic equivalent when that mod is loaded. */
    public static Attribute getAttribute(ResourceLocation id) {
        return id == null ? null : resolve(BuiltInRegistries.ATTRIBUTE.getOptional(id).orElse(null));
    }

    public static void init() {
        if (initialized)
            return;
        ATTRIBUTES.register();
        initialized = true;
    }
}
