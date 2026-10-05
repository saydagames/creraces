package mc.sayda.creraces.worldgen;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.mixin.TreeDecoratorTypeAccessor;
import mc.sayda.creraces.world.tree.VeilDrapeDecorator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

/**
 * Registers the fairy_realm biome source and chunk generator codecs, the surface-offset jigsaw
 * structure type, and the veil drape tree decorator type, and places the fairy realm's
 * hand-built trees.
 *
 * Registration must happen before any world loads; both loaders call registerCodecs from a
 * BootstrapMixin so it runs before vanilla registries freeze.
 */
public class ModWorldgen {

    private static boolean registered = false;
    private static boolean treeWasPlaced = false;
    private static boolean seasonalTreesPlaced = false;

    public static void registerCodecs() {
        if (registered) return;
        registered = true;

        Registry.register(
                BuiltInRegistries.BIOME_SOURCE,
                new ResourceLocation(CreRaces.MODID, "fairy_realm_biomes"),
                FairyRealmBiomeSource.CODEC
        );

        Registry.register(
                BuiltInRegistries.CHUNK_GENERATOR,
                new ResourceLocation(CreRaces.MODID, "fairy_realm"),
                FairyRealmChunkGenerator.CODEC
        );

        SurfaceOffsetJigsawStructure.TYPE = Registry.register(
                BuiltInRegistries.STRUCTURE_TYPE,
                new ResourceLocation(CreRaces.MODID, "surface_jigsaw"),
                () -> SurfaceOffsetJigsawStructure.CODEC.codec()
        );

        registerVeilDrapeDecoratorType();

        CreRaces.LOGGER.info("CreRaces: Registered fairy_realm biome source + chunk generator codecs.");
    }

    private static void registerVeilDrapeDecoratorType() {
        // TreeDecoratorType's constructor is private. The accessor mixin reaches it without raw
        // reflection, which JPMS-style loaders reject with an unchecked InaccessibleObjectException.
        TreeDecoratorType<VeilDrapeDecorator> decoratorType =
                TreeDecoratorTypeAccessor.creraces$callNew(VeilDrapeDecorator.CODEC);
        Registry.register(BuiltInRegistries.TREE_DECORATOR_TYPE,
                new ResourceLocation(CreRaces.MODID, "veil_drape"), decoratorType);
        VeilDrapeDecorator.TYPE = decoratorType;
    }

    /**
     * Resets the placement flags on server stop so placement re-checks on next start.
     * Call from SERVER_STOPPING event.
     */
    public static void onServerStop() {
        treeWasPlaced = false;
        seasonalTreesPlaced = false;
    }

    /**
     * Places seasonal spawn trees at each quadrant's fairy tree position on first visit.
     * Spring cherry → (+250, -250), Summer oak → (+250, +250), Winter/Autumn spruce → (±250, ±250)
     * [X, Z only; see placeTreeAt calls below for exact Y].
     */
    public static void placeSeasonalTreesIfNeeded(ServerLevel fairyLevel) {
        if (seasonalTreesPlaced) return;
        // Each tree is 71×51×65. Origin = NW corner of footprint.
        // Center at (center.x, y, center.z): origin.x = center.x - 35, origin.z = center.z - 32 (same subtraction regardless of quadrant sign).
        placeTreeAt(fairyLevel, "fairy_tree_cherry", new BlockPos( 215, 72, -282), new BlockPos( 250, 90, -250));
        placeTreeAt(fairyLevel, "fairy_tree_oak",    new BlockPos( 215, 72,  218), new BlockPos( 250, 90,  250));
        placeTreeAt(fairyLevel, "fairy_tree_spruce", new BlockPos(-285, 72, -282), new BlockPos(-250, 90, -250));
        applySnowToStructure(fairyLevel, new BlockPos(-285, 72, -282), 71, 51, 65); // winter quadrant
        placeTreeAt(fairyLevel, "fairy_tree_spruce", new BlockPos(-285, 72,  218), new BlockPos(-250, 90,  250));
        seasonalTreesPlaced = true;
    }

    private static void applySnowToStructure(ServerLevel level, BlockPos origin, int sizeX, int sizeY, int sizeZ) {
        for (int dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                for (int dy = sizeY; dy >= 0; dy--) {
                    BlockPos pos = new BlockPos(x, origin.getY() + dy, z);
                    if (!level.getBlockState(pos).isAir()) {
                        BlockPos above = pos.above();
                        if (level.getBlockState(above).isAir()
                                && Blocks.SNOW.defaultBlockState().canSurvive(level, above)) {
                            level.setBlock(above, Blocks.SNOW.defaultBlockState(), 3);
                        }
                        break;
                    }
                }
            }
        }
    }

    private static void placeTreeAt(ServerLevel level, String name, BlockPos origin, BlockPos sentinel) {
        if (!level.getBlockState(sentinel).isAir()) return;
        placeTemplate(level, name, origin);
    }

    /**
     * Places giant_tree_paulzero.nbt on the island center if it hasn't been placed yet.
     * Call when a player first enters the fairy realm (level is guaranteed loaded).
     *
     * Structure is 132×154×114 blocks. Placement origin (-66, 56, -57) maps template
     * position (0,0,0) so the structure is centered at X=0, Z=0 with the tree base at Y=72.
     * Air blocks in the NBT are ignored so terrain integrates naturally.
     */
    public static void placeFairyTreeIfNeeded(ServerLevel fairyLevel) {
        // Set world border every time (idempotent); needed because the level
        // loads lazily so SERVER_STARTED fires before the level exists.
        WorldBorder border = fairyLevel.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(CreRacesConfig.FAIRY_REALM_BORDER_SIZE.get());
        border.setWarningBlocks(20); // start red glow 20 blocks before the edge

        if (treeWasPlaced) return;

        // Sentinel: any non-air block at (0, 90, 0) means the tree is already here
        BlockPos sentinel = new BlockPos(0, 90, 0);
        if (!fairyLevel.getBlockState(sentinel).isAir()) {
            treeWasPlaced = true;
            return;
        }

        // Template (0,0,0) lands at (-66, 56, -57): the 132×114 footprint is centred on X=0, Z=0
        // and the tree base (16 blocks up) sits at Y=72.
        if (placeTemplate(fairyLevel, "giant_tree_paulzero", new BlockPos(-66, 56, -57))) {
            treeWasPlaced = true;
        }
    }

    /** Places a structure template from this mod's namespace, skipping its air. Returns false if the template is missing. */
    private static boolean placeTemplate(ServerLevel level, String name, BlockPos origin) {
        // get() rather than getOrCreate(): the latter hands back an empty template for a missing file.
        Optional<StructureTemplate> template = level.getStructureManager().get(new ResourceLocation(CreRaces.MODID, name));
        if (template.isEmpty()) {
            CreRaces.LOGGER.error("CreRaces: structure template {} not found; skipping placement.", name);
            return false;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
        template.get().placeInWorld(level, origin, origin, settings, level.random, 3);
        CreRaces.LOGGER.info("CreRaces: Placed {} at {}.", name, origin);
        return true;
    }
}
