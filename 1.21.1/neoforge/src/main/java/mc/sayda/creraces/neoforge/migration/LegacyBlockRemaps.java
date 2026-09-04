package mc.sayda.creraces.neoforge.migration;

import mc.sayda.creraces.CreRaces;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Redirects CreRaces Classic block/item/fluid/block-entity IDs that don't literally match a
 * current registry name onto their rewrite equivalent, so legacy world chunks and player
 * inventories resolve to the right thing instead of dropping the reference. (Blocks whose
 * registry name was renamed outright to match Classic, naiad_statue, aurai_sculpture,
 * oread_idol, need no entry here; they already match by construction.)
 *
 * 1.20.1 did this with Forge's MissingMappingsEvent, resolving each miss as the world loaded.
 * NeoForge dropped that event; the equivalent is a registry alias declared up front, which the
 * registry then applies whenever the old name is looked up. Aliases are pure name-to-name, so
 * unlike the old event handler nothing here has to wait for the suppliers to bind.
 */
public final class LegacyBlockRemaps {
    private static final Map<String, String> BLOCK_REMAPS = Map.of(
            "tori_bell", "torii_bell",
            "weathered_tori_bell", "weathered_torii_bell",
            "blue_mushroom", "veil_mushroom",
            // Both intentionally diverge from Classic's original registry names, see their
            // ModBlocks.java declarations for why.
            "blessed_water", "eterveil",
            "dryad_totem", "forest_totem"
    );

    private static final Map<String, String> BLOCK_ENTITY_REMAPS = Map.of(
            "tori_bell", "torii_bell"
    );

    private static final Map<String, String> ITEM_REMAPS = Map.of(
            "blessed_water_bucket", "eterveil_bucket"
    );

    private static final Map<String, String> FLUID_REMAPS = Map.of(
            "blessed_water", "eterveil",
            "blessed_water_flowing", "eterveil_flowing"
    );

    private LegacyBlockRemaps() {}

    public static void init() {
        addAliases(BuiltInRegistries.BLOCK, BLOCK_REMAPS, "block");
        addAliases(BuiltInRegistries.BLOCK_ENTITY_TYPE, BLOCK_ENTITY_REMAPS, "block entity");
        addAliases(BuiltInRegistries.ITEM, ITEM_REMAPS, "item");
        addAliases(BuiltInRegistries.FLUID, FLUID_REMAPS, "fluid");
    }

    private static void addAliases(Registry<?> registry, Map<String, String> remaps, String kind) {
        remaps.forEach((legacyPath, currentPath) -> {
            ResourceLocation from = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, legacyPath);
            ResourceLocation to = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, currentPath);
            registry.addAlias(from, to);
            CreRaces.LOGGER.debug("Aliased legacy {} {} -> {}", kind, from, to);
        });
    }
}
