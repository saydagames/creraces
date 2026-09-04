package mc.sayda.creraces.forge.migration;

import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.registry.ModBlocks;
import mc.sayda.creraces.registry.ModFluids;
import mc.sayda.creraces.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.MissingMappingsEvent;

import java.util.Map;

/**
 * Redirects CreRaces Classic block/item/fluid/block-entity IDs that don't literally match a
 * current registry name onto their rewrite equivalent, so legacy world chunks and player
 * inventories resolve to the right thing instead of dropping the reference. (Blocks whose
 * registry name was renamed outright to match Classic, naiad_statue, aurai_sculpture,
 * oread_idol, need no entry here; they already match by construction.)
 */
public final class LegacyBlockRemaps {
    private static final Map<String, RegistrySupplier<Block>> BLOCK_REMAPS = Map.of(
            "tori_bell", ModBlocks.TORII_BELL,
            "weathered_tori_bell", ModBlocks.WEATHERED_TORII_BELL,
            "blue_mushroom", ModBlocks.VEIL_MUSHROOM,
            // Both intentionally diverge from Classic's original registry names, see their
            // ModBlocks.java declarations for why.
            "blessed_water", ModBlocks.ETERVEIL_BLOCK,
            "dryad_totem", ModBlocks.DRYAD_TOTEM
    );

    private static final Map<String, RegistrySupplier<Item>> ITEM_REMAPS = Map.of(
            "blessed_water_bucket", ModItems.ETERVEIL_BUCKET
    );

    private LegacyBlockRemaps() {}

    /**
     * Deliberately not a static field: ModBlocks.TORII_BELL_ENTITY is assigned inside
     * ModBlocks.register() (called from CreRaces.init()), not at field-declaration time like the
     * plain Block suppliers above, LegacyBlockRemaps.init() runs before that, so evaluating this
     * eagerly at class-load time would capture null. MissingMappingsEvent only ever fires much
     * later during actual world loading, well after ModBlocks.register() has run, so a lazy
     * lookup here is always safe.
     */
    private static Map<String, RegistrySupplier<? extends BlockEntityType<?>>> blockEntityRemaps() {
        return Map.of("tori_bell", ModBlocks.TORII_BELL_ENTITY);
    }

    /** Same lazy-evaluation reasoning as blockEntityRemaps(), ModFluids fields ARE eager, but kept consistent for clarity. */
    private static Map<String, RegistrySupplier<? extends Fluid>> fluidRemaps() {
        return Map.of(
                "blessed_water", ModFluids.ETERVEIL,
                "blessed_water_flowing", ModFluids.ETERVEIL_FLOWING
        );
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(LegacyBlockRemaps::onMissingMappings);
    }

    private static void onMissingMappings(MissingMappingsEvent event) {
        for (MissingMappingsEvent.Mapping<Block> mapping : event.<Block>getAllMappings(Registries.BLOCK)) {
            ResourceLocation key = mapping.getKey();
            if (!CreRaces.MODID.equals(key.getNamespace())) continue;

            var replacement = BLOCK_REMAPS.get(key.getPath());
            if (replacement != null) {
                mapping.remap(replacement.get());
                CreRaces.LOGGER.info("Remapped legacy block {} -> {}", key, replacement.get());
            }
        }

        for (MissingMappingsEvent.Mapping<BlockEntityType<?>> mapping : event.<BlockEntityType<?>>getAllMappings(Registries.BLOCK_ENTITY_TYPE)) {
            ResourceLocation key = mapping.getKey();
            if (!CreRaces.MODID.equals(key.getNamespace())) continue;

            var replacement = blockEntityRemaps().get(key.getPath());
            if (replacement != null) {
                BlockEntityType<?> target = replacement.get();
                mapping.remap(target);
                CreRaces.LOGGER.info("Remapped legacy block entity {} -> {}", key, target);
            }
        }

        for (MissingMappingsEvent.Mapping<Item> mapping : event.<Item>getAllMappings(Registries.ITEM)) {
            ResourceLocation key = mapping.getKey();
            if (!CreRaces.MODID.equals(key.getNamespace())) continue;

            var replacement = ITEM_REMAPS.get(key.getPath());
            if (replacement != null) {
                mapping.remap(replacement.get());
                CreRaces.LOGGER.info("Remapped legacy item {} -> {}", key, replacement.get());
            }
        }

        for (MissingMappingsEvent.Mapping<Fluid> mapping : event.<Fluid>getAllMappings(Registries.FLUID)) {
            ResourceLocation key = mapping.getKey();
            if (!CreRaces.MODID.equals(key.getNamespace())) continue;

            var replacement = fluidRemaps().get(key.getPath());
            if (replacement != null) {
                Fluid target = (Fluid) replacement.get();
                mapping.remap(target);
                CreRaces.LOGGER.info("Remapped legacy fluid {} -> {}", key, target);
            }
        }
    }
}
