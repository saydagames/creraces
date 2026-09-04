package mc.sayda.creraces.registry;

import mc.sayda.creraces.CreRaces;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

/**
 * Enchantment lookups.
 *
 * Enchantment became a final, fully data-driven type in 1.21, so these are no longer registered
 * from code: the definitions live in data/creraces/enchantment/*.json and are loaded into a
 * datapack registry per-world. What's left here are the keys plus a resolver, since a Holder can
 * only be obtained from a live level's registry access.
 *
 * Both are marker enchantments; the behaviour is implemented in IncidentResolver (taxing) and
 * ResourceTicker (sun protection), which read the level off the equipped item.
 */
public class ModEnchantments {

    public static final ResourceKey<Enchantment> SUN_PROTECTION = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "sun_protection"));

    public static final ResourceKey<Enchantment> TAXING = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "taxing"));

    /** Resolves an enchantment key against the level's registries, or null if the datapack didn't load it. */
    public static Holder<Enchantment> get(Level level, ResourceKey<Enchantment> key) {
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key).orElse(null);
    }

    /** Convenience for the common "how many levels of X does this stack have" check. */
    public static int levelOn(Level level, ResourceKey<Enchantment> key, net.minecraft.world.item.ItemStack stack) {
        Holder<Enchantment> holder = get(level, key);
        return holder == null ? 0
                : net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
    }
}
