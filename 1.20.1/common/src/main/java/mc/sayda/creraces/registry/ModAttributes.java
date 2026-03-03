package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import java.util.function.Supplier;

public class ModAttributes {
        public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(CreRaces.MODID,
                        Registries.ATTRIBUTE);

        // Resources
        public static final RegistrySupplier<Attribute> MAX_MANA = ATTRIBUTES.register("max_mana",
                        () -> new RangedAttribute("attribute.creraces.max_mana", 0.0, 0.0, 10000.0).setSyncable(true));

        public static final RegistrySupplier<Attribute> MAX_RAGE = ATTRIBUTES.register("max_rage",
                        () -> new RangedAttribute("attribute.creraces.max_rage", 0.0, 0.0, 100.0).setSyncable(true));

        public static final RegistrySupplier<Attribute> MAX_ENERGY = ATTRIBUTES.register("max_energy",
                        () -> new RangedAttribute("attribute.creraces.max_energy", 0.0, 0.0, 1000.0).setSyncable(true));

        public static final RegistrySupplier<Attribute> MAX_GRIT = ATTRIBUTES.register("max_grit",
                        () -> new RangedAttribute("attribute.creraces.max_grit", 0.0, 0.0, 100.0).setSyncable(true));

        // RPG Stats
        public static final RegistrySupplier<Attribute> ABILITY_POWER = ATTRIBUTES.register("ability_power",
                        () -> new RangedAttribute("attribute.creraces.ability_power", 0.0, 0.0, 10000.0)
                                        .setSyncable(true));

        public static final RegistrySupplier<Attribute> ATTACK_DAMAGE = ATTRIBUTES.register("attack_damage",
                        () -> new RangedAttribute("attribute.creraces.attack_damage", 0.0, 0.0, 10000.0)
                                        .setSyncable(true));

        public static final RegistrySupplier<Attribute> CRIT_RATE = ATTRIBUTES.register("crit_rate",
                        () -> new RangedAttribute("attribute.creraces.crit_rate", 0.0, 0.0, 100.0).setSyncable(true)); // Percentage

        public static final RegistrySupplier<Attribute> ARMOR_PENETRATION = ATTRIBUTES.register("armor_penetration",
                        () -> new RangedAttribute("attribute.creraces.armor_penetration", 0.0, 0.0, 100.0)
                                        .setSyncable(true)); // Percentage

        public static final RegistrySupplier<Attribute> ABILITY_HASTE = ATTRIBUTES.register("ability_haste",
                        () -> new RangedAttribute("attribute.creraces.ability_haste", 0.0, 0.0, 100.0)
                                        .setSyncable(true));

        // Regeneration & Decay
        public static final RegistrySupplier<Attribute> MANA_REGEN = ATTRIBUTES.register("mana_regeneration",
                        () -> new RangedAttribute("attribute.creraces.mana_regen", 0.1, 0.0, 1000.0).setSyncable(true));

        public static final RegistrySupplier<Attribute> ENERGY_REGEN = ATTRIBUTES.register("energy_regeneration",
                        () -> new RangedAttribute("attribute.creraces.energy_regen", 0.25, 0.0, 1000.0)
                                        .setSyncable(true));

        public static final RegistrySupplier<Attribute> GRIT_DECAY = ATTRIBUTES.register("grit_decay",
                        () -> new RangedAttribute("attribute.creraces.grit_decay", 0.1, 0.0, 1000.0).setSyncable(true));

        public static final RegistrySupplier<Attribute> RAGE_DECAY = ATTRIBUTES.register("rage_decay",
                        () -> new RangedAttribute("attribute.creraces.rage_decay", 0.25, 0.0, 1000.0)
                                        .setSyncable(true));

        private static boolean initialized = false;

        public static void init() {
                if (initialized)
                        return;
                ATTRIBUTES.register();
                initialized = true;
        }
}
