package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.enchantment.SunProtectionEnchantment;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(CreRaces.MODID,
            Registries.ENCHANTMENT);

    public static final RegistrySupplier<Enchantment> SUN_PROTECTION = ENCHANTMENTS.register("sun_protection",
            SunProtectionEnchantment::new);

    public static final RegistrySupplier<Enchantment> TAXING = ENCHANTMENTS.register("taxing",
            mc.sayda.creraces.enchantment.TaxingEnchantment::new);

    public static void register() {
        ENCHANTMENTS.register();
    }
}
