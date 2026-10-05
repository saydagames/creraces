package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.enchantment.SunProtectionEnchantment;
import mc.sayda.creraces.enchantment.TaxingEnchantment;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Both are marker enchantments; the behaviour is implemented in IncidentResolver (taxing) and
 * ResourceTicker (sun protection), which read the level off the equipped item.
 */
public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(CreRaces.MODID,
            Registries.ENCHANTMENT);

    public static final RegistrySupplier<Enchantment> SUN_PROTECTION = ENCHANTMENTS.register("sun_protection",
            SunProtectionEnchantment::new);

    public static final RegistrySupplier<Enchantment> TAXING = ENCHANTMENTS.register("taxing",
            TaxingEnchantment::new);

    public static void register() {
        ENCHANTMENTS.register();
    }
}
