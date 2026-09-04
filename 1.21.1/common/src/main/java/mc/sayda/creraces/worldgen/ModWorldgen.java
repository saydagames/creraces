package mc.sayda.creraces.worldgen;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.registries.DeferredRegister;
import mc.sayda.creraces.CreRaces;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Registers the fairy_realm biome source and chunk generator codecs, the surface-offset jigsaw
 * structure type, and the veil drape tree decorator type.
 *
 * These went through Registry.register from a BootstrapMixin on 1.20.1, but the built-in
 * registries are already frozen by the time Bootstrap returns on 1.21.1, so they go through
 * DeferredRegister now and bind during normal mod registration instead.
 */
public class ModWorldgen {

    private static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(CreRaces.MODID, Registries.BIOME_SOURCE);
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(CreRaces.MODID, Registries.CHUNK_GENERATOR);
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(CreRaces.MODID, Registries.STRUCTURE_TYPE);
    private static final DeferredRegister<TreeDecoratorType<?>> TREE_DECORATOR_TYPES =
            DeferredRegister.create(CreRaces.MODID, Registries.TREE_DECORATOR_TYPE);

    static {
        BIOME_SOURCES.register("fairy_realm_biomes", () -> FairyRealmBiomeSource.CODEC);
        CHUNK_GENERATORS.register("fairy_realm", () -> FairyRealmChunkGenerator.CODEC);

        // Both of these keep a plain static TYPE field that the rest of the worldgen code reads
        // directly, so fill it in as the entry is built rather than threading a RegistrySupplier
        // through every call site.
        STRUCTURE_TYPES.register("surface_jigsaw", () -> {
            StructureType<SurfaceOffsetJigsawStructure> type = () -> SurfaceOffsetJigsawStructure.CODEC;
            SurfaceOffsetJigsawStructure.TYPE = type;
            return type;
        });
        TREE_DECORATOR_TYPES.register("veil_drape", () -> {
            TreeDecoratorType<mc.sayda.creraces.world.tree.VeilDrapeDecorator> type =
                    mc.sayda.creraces.mixin.TreeDecoratorTypeAccessor.creraces$callNew(mc.sayda.creraces.world.tree.VeilDrapeDecorator.CODEC);
            mc.sayda.creraces.world.tree.VeilDrapeDecorator.TYPE = type;
            return type;
        });
    }

    private static boolean registered = false;
    private static boolean treeWasPlaced = false;
    private static boolean seasonalTreesPlaced = false;

    public static void registerCodecs() {
        if (registered) return;
        registered = true;

        BIOME_SOURCES.register();
        CHUNK_GENERATORS.register();
        STRUCTURE_TYPES.register();
        TREE_DECORATOR_TYPES.register();

        CreRaces.LOGGER.info("CreRaces: Registered fairy_realm biome source + chunk generator codecs.");
    }

    /**
     * Resets the placement flag on server stop so placement re-checks on next start.
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
        StructureTemplateManager mgr = level.getStructureManager();
        ResourceLocation treeId = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, name);
        StructureTemplate template = mgr.getOrCreate(treeId);
        if (template == null) {
            CreRaces.LOGGER.error("CreRaces: {} not found; skipping placement.", name);
            return;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
        template.placeInWorld(level, origin, origin, settings, level.random, 3);
        CreRaces.LOGGER.info("CreRaces: Placed {} at {}.", name, origin);
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
        net.minecraft.world.level.border.WorldBorder border = fairyLevel.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(mc.sayda.creraces.config.CreRacesConfig.FAIRY_REALM_BORDER_SIZE.get());
        border.setWarningBlocks(20); // start red glow 20 blocks before the edge

        if (treeWasPlaced) return;

        // Sentinel: any non-air block at (0, 90, 0) means the tree is already here
        BlockPos sentinel = new BlockPos(0, 90, 0);
        if (!fairyLevel.getBlockState(sentinel).isAir()) {
            treeWasPlaced = true;
            return;
        }

        StructureTemplateManager mgr = fairyLevel.getStructureManager();
        ResourceLocation treeId = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "giant_tree_paulzero");
        StructureTemplate template = mgr.getOrCreate(treeId);
        if (template == null) {
            CreRaces.LOGGER.error("CreRaces: giant_tree_paulzero.nbt not found; skipping island tree placement.");
            return;
        }

        // Origin: template (0,0,0) → world (-66, 56, -57)
        // Centers the 132×114 footprint at X=0,Z=0 and puts tree base (16 blocks up) at Y=72
        BlockPos origin = new BlockPos(-66, 56, -57);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);

        template.placeInWorld(fairyLevel, origin, origin, settings, fairyLevel.random, 3);
        treeWasPlaced = true;
        CreRaces.LOGGER.info("CreRaces: Placed giant_tree_paulzero on fairy realm island.");
    }
}
