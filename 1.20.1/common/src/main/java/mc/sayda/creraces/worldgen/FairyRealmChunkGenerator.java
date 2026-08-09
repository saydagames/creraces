package mc.sayda.creraces.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Fairy realm chunk generator: hash-based value noise, no sine functions,
 * so no periodic grid or wave patterns in terrain or river banks.
 *
 * Terrain: domain-warped 6-octave fBm, always >= RIVER_LEVEL+2 (no ocean areas).
 *
 * Rivers: meander and bank width driven by multi-octave value noise (natural
 * bends). Floor is a two-frequency noise blend of sand/gravel/clay, depth
 * varies ±2 blocks. Seagrass (short+tall) and lily pads are placed directly.
 * Sandy banks use a two-frequency noise-driven boundary.
 *
 * Caves: two independent 3D value noises; always air (no vanilla aquifer fill).
 * Start >= 7 blocks below surface to prevent shallow craters.
 *
 * Island: forced flat plateau at y=69, smooth slope over the outer 12-block ring.
 *
 * applyCarvers: intentionally empty; caves are generated in fillFromNoise so
 * the vanilla aquifer never runs and floods caves with water.
 */
public class FairyRealmChunkGenerator extends ChunkGenerator {

    // ─── Geographic constants ────────────────────────────────────────────────────

    static final int RIVER_LEVEL       = 63;
    static final int RIVER_HALF        = 12;
    static final int ISLAND_RADIUS     = 56;
    static final int ISLAND_MOAT_OUTER = 90;  // wider moat for smoother junction
    private static final int MOAT_INNER_BANK_W = 10; // blocks of slope on the island-facing moat wall
    private static final int BLEND_RADIUS  = 28;
    private static final int SURFACE_BASE  = 70;
    private static final int ISLAND_FLAT_Y = 69;
    private static final int RIVER_FLOOR   = RIVER_LEVEL - 5; // y=58

    private static final long ISLAND_RADIUS_SQ     = (long) ISLAND_RADIUS     * ISLAND_RADIUS;
    private static final long ISLAND_MOAT_OUTER_SQ = (long) ISLAND_MOAT_OUTER * ISLAND_MOAT_OUTER;

    // ─── Codec ───────────────────────────────────────────────────────────────────

    public static final Codec<FairyRealmChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source")
                            .forGetter(ChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings")
                            .forGetter(g -> g.settings)
            ).apply(instance, FairyRealmChunkGenerator::new)
    );

    private final NoiseBasedChunkGenerator delegate;
    private final Holder<NoiseGeneratorSettings> settings;

    public FairyRealmChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource);
        this.settings = settings;
        this.delegate = new NoiseBasedChunkGenerator(biomeSource, settings);
    }

    @Override protected Codec<? extends ChunkGenerator> codec() { return CODEC; }

    // ─── Noise primitives ────────────────────────────────────────────────────────

    private static double lerp(double a, double b, double t) { return a + t * (b - a); }
    private static double smooth(double t) { return t * t * (3.0 - 2.0 * t); }

    /** Long-mixed hash; no directional artifacts. Returns [-1, 1]. */
    private static double h2(int x, int z) {
        long h = (long) x * 374761393L + (long) z * 668265263L;
        h = (h ^ (h >>> 16)) * 2246822519L;
        h ^= h >>> 13;
        return (h & 0x7FFFFFFFL) / (double) 0x7FFFFFFFL * 2.0 - 1.0;
    }

    private static double h3(int x, int y, int z) {
        long h = (long) x * 374761393L ^ (long) y * 2246822519L ^ (long) z * 668265263L;
        h = (h ^ (h >>> 16)) * 2246822519L;
        h ^= h >>> 13;
        return (h & 0x7FFFFFFFL) / (double) 0x7FFFFFFFL * 2.0 - 1.0;
    }

    private static double n2(double x, double z) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        double fx = smooth(x - ix), fz = smooth(z - iz);
        return lerp(lerp(h2(ix, iz), h2(ix+1, iz), fx), lerp(h2(ix, iz+1), h2(ix+1, iz+1), fx), fz);
    }

    private static double n3(double x, double y, double z) {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y), iz = (int) Math.floor(z);
        double fx = smooth(x - ix), fy = smooth(y - iy), fz = smooth(z - iz);
        double y0 = lerp(lerp(h3(ix,iy,iz), h3(ix+1,iy,iz), fx), lerp(h3(ix,iy+1,iz), h3(ix+1,iy+1,iz), fx), fy);
        double y1 = lerp(lerp(h3(ix,iy,iz+1), h3(ix+1,iy,iz+1), fx), lerp(h3(ix,iy+1,iz+1), h3(ix+1,iy+1,iz+1), fx), fy);
        return lerp(y0, y1, fz);
    }

    // ─── Terrain noise ───────────────────────────────────────────────────────────

    /**
     * Domain-warped 6-octave fBm. The warp displaces sampling coords by up to
     * ±30 blocks using low-frequency noise, breaking up grid regularity.
     */
    static int surfaceHeight(int x, int z) {
        double wFreq = 1.0 / 220.0;
        double wx = 30.0 * n2(x * wFreq + 31.7, z * wFreq + 83.9);
        double wz = 30.0 * n2(x * wFreq + 71.3, z * wFreq + 17.5);
        double h = SURFACE_BASE;
        double freq = 1.0 / 200.0, amp = 7.0;
        for (int i = 0; i < 6; i++) {
            h += amp * n2((x + wx) * freq, (z + wz) * freq);
            freq *= 2.07; amp *= 0.52;
        }
        return (int) h;
    }

    /** Multi-octave value noise meander: natural bends, not a sine wave. */
    static double meander(int coord) {
        return 18.0 * n2(coord * 0.0026, 17.3)
             +  6.0 * n2(coord * 0.0079, 83.1)
             +  2.5 * n2(coord * 0.019,  41.7);
    }

    // ─── Geometry helpers (package-private for FairyRealmBiomeSource) ───────────

    static boolean isIsland(int x, int z) {
        return (long) x * x + (long) z * z < ISLAND_RADIUS_SQ;
    }

    static boolean isIslandMoat(int x, int z) {
        long d = (long) x * x + (long) z * z;
        return d >= ISLAND_RADIUS_SQ && d < ISLAND_MOAT_OUTER_SQ;
    }

    /**
     * Signed distance to nearest river-corridor wall (negative = inside water zone).
     * Bank width varies with value noise, making edges jagged and irregular.
     */
    static double riverEdgeDist(int x, int z) {
        double bankVar = 5.0 * n2(x * 0.027 + z * 0.019, z * 0.023 - x * 0.013);
        double half = RIVER_HALF + bankVar;
        return Math.min(
                Math.abs(z - meander(x)) - half,
                Math.abs(x - meander(z)) - half);
    }

    static boolean isRiverCorridor(int x, int z) { return riverEdgeDist(x, z) < 0; }

    static boolean isWater(int x, int z) {
        if (isIsland(x, z)) return false;
        return isIslandMoat(x, z) || isRiverCorridor(x, z);
    }

    // ─── Cave & block helpers ─────────────────────────────────────────────────────

    /**
     * Two independent 3D value noises; tunnel forms where both approach zero.
     * Starts >= 12 blocks below surface to prevent craters and side-exposure on slopes.
     */
    private static boolean isCaveAt(int x, int y, int z, int surfaceY, int minY) {
        if (y >= surfaceY - 12 || y <= minY + 5) return false;
        double sc = (y < 0) ? 9.0 : 11.0;
        double a = n3(x / sc,       y / sc,       z / sc);
        double b = n3((x+41) / sc, (y+37) / sc, (z+71) / sc);
        return a * a + b * b < ((y < 0) ? 0.06 : 0.04);
    }

    private static BlockState stoneAt(int y, int x, int z) {
        if (y < 0) return Blocks.DEEPSLATE.defaultBlockState();
        long h = (long) x * 374761393L ^ (long) z * 668265263L ^ (long) y * 2246822519L;
        h = (h ^ (h >>> 16)) * 2246822519L; h ^= h >>> 13;
        int v = (int)(h & 0xFF);
        if (v < 14) return Blocks.ANDESITE.defaultBlockState();
        if (v < 24) return Blocks.DIORITE.defaultBlockState();
        if (v < 34) return Blocks.GRANITE.defaultBlockState();
        if (v < 37) return Blocks.GRAVEL.defaultBlockState();
        return Blocks.STONE.defaultBlockState();
    }

    // ─── Core generation ─────────────────────────────────────────────────────────

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
            RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        int minY   = chunk.getMinBuildHeight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int lx = 0; lx < 16; lx++) {
            int wx = startX + lx;
            for (int lz = 0; lz < 16; lz++) {
                int wz = startZ + lz;

                for (int y = minY; y <= minY + 3; y++) {
                    chunk.setBlockState(pos.set(wx, y, wz), Blocks.BEDROCK.defaultBlockState(), false);
                }

                boolean inIsland = isIsland(wx, wz);
                boolean inMoat   = isIslandMoat(wx, wz);

                int rawHeight = Math.max(surfaceHeight(wx, wz), RIVER_LEVEL + 2);
                int solidTop;
                boolean canHaveCaves;

                if (inIsland) {
                    solidTop = rawHeight;
                    canHaveCaves = false; // flattenIsland carves island down, exposing caves
                } else if (inMoat) {
                    // Inner bank: slope from RIVER_LEVEL down to RIVER_FLOOR over MOAT_INNER_BANK_W.
                    // This mirrors the outer bank blend so both sides of the moat look symmetric.
                    double rr = Math.sqrt((long) wx * wx + (long) wz * wz);
                    double distFromInner = rr - ISLAND_RADIUS;
                    if (distFromInner < MOAT_INNER_BANK_W) {
                        double ti = distFromInner / MOAT_INNER_BANK_W;
                        ti = ti * ti * (3.0 - 2.0 * ti);
                        solidTop = (int)(RIVER_LEVEL - ti * (RIVER_LEVEL - RIVER_FLOOR));
                    } else {
                        solidTop = RIVER_FLOOR;
                    }
                    canHaveCaves = false;
                } else {
                    double rEdge = riverEdgeDist(wx, wz);
                    double moatOuter = Math.sqrt((long) wx * wx + (long) wz * wz) - ISLAND_MOAT_OUTER;
                    double distToWater = Math.min(rEdge, moatOuter);
                    if (distToWater < 0) {
                        solidTop = RIVER_FLOOR;
                        canHaveCaves = false;
                    } else if (distToWater < BLEND_RADIUS) {
                        double t = distToWater / BLEND_RADIUS;
                        t = t * t * (3.0 - 2.0 * t);
                        solidTop = (int)(RIVER_FLOOR + t * (rawHeight - RIVER_FLOOR));
                        canHaveCaves = false;
                    } else {
                        solidTop = rawHeight;
                        canHaveCaves = true;
                    }
                }
                for (int y = minY + 4; y <= solidTop; y++) {
                    if (canHaveCaves && isCaveAt(wx, y, wz, solidTop, minY)) continue;
                    BlockState state;
                    if (y < solidTop - 3) {
                        state = stoneAt(y, wx, wz);
                    } else if (y < solidTop) {
                        state = Blocks.DIRT.defaultBlockState();
                    } else {
                        state = Blocks.GRASS_BLOCK.defaultBlockState();
                    }
                    chunk.setBlockState(pos.set(wx, y, wz), state, false);
                }

                // Fill water for water zones and submerged bank cells
                if (solidTop < RIVER_LEVEL) {
                    for (int y = solidTop + 1; y <= RIVER_LEVEL; y++) {
                        chunk.setBlockState(pos.set(wx, y, wz), Blocks.WATER.defaultBlockState(), false);
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager,
            RandomState randomState, ChunkAccess chunk) {
        postProcessGeography(chunk);
        placeIslandBushes(region, chunk);
        placeIslandDecoration(region, chunk);
        // Snow and ice are handled by minecraft:snowy_taiga's FREEZE_TOP_LAYER biome
        // feature, which runs in applyBiomeDecoration AFTER tree placement. Placing
        // snow here (before applyBiomeDecoration) would block spruce tree growth.
    }

    /**
     * Dense, organically-shaped oak undergrowth across the island surface.
     *
     * All centres kept at [2..13] so the widest footprint (|dx|+|dz|≤2 diamond,
     * max offset ±2) never crosses chunk boundaries.
     *
     * Shapes avoid rectangular patterns by using diamond masks and per-column
     * hash-driven corner dropping so no two bushes look identical.
     */
    private static void placeIslandBushes(WorldGenRegion region, ChunkAccess chunk) {
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        BlockState log  = Blocks.OAK_LOG.defaultBlockState();
        BlockState leaf = Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, false)
                .setValue(LeavesBlock.DISTANCE, 1);

        for (int lx = 2; lx <= 13; lx++) {
            int wx = startX + lx;
            for (int lz = 2; lz <= 13; lz++) {
                int wz = startZ + lz;
                if (!isIsland(wx, wz)) continue;

                long h = ((long) wx * 374761393L) ^ ((long) wz * 668265263L) ^ 0x50F4B5C3L;
                h = (h ^ (h >>> 16)) * 2246822519L; h ^= h >>> 13;
                if ((h & 0xFF) >= 20) continue;

                int surfY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz);
                if (!chunk.getBlockState(mpos.set(wx, surfY, wz)).is(Blocks.GRASS_BLOCK)) continue;

                int variant = (int)((h >>> 8) & 0x3);

                if (variant == 0) {
                    // Wide tuft: dense inner ring + sparse outer ring, no log
                    // Inner 8 neighbours: always placed
                    int[] dxN = {-1, 1, 0, 0,-1, 1,-1, 1};
                    int[] dzN = { 0, 0, 1,-1,-1,-1, 1, 1};
                    region.setBlock(mpos.set(wx, surfY + 1, wz), leaf, 3);
                    for (int i = 0; i < 8; i++)
                        region.setBlock(new BlockPos(wx + dxN[i], surfY + 1, wz + dzN[i]), leaf, 3);
                    // Outer radius-2 cross arms: hash-gated (~60% each)
                    int bits2 = (int)((h >>> 12) & 0xFF);
                    int[] dxO = {-2, 2, 0, 0, -2, 2, -2, 2};
                    int[] dzO = { 0, 0, 2,-2, -2,-2,  2,  2};
                    for (int i = 0; i < 8; i++) {
                        if ((bits2 >>> i & 1) == 1)
                            region.setBlock(new BlockPos(wx + dxO[i], surfY + 1, wz + dzO[i]), leaf, 3);
                    }
                    // Low cap over centre
                    if (((h >>> 20) & 0x3) != 3)
                        region.setBlock(new BlockPos(wx, surfY + 2, wz), leaf, 3);

                } else if (variant == 1) {
                    // Wide low mound: 5-diamond base + 3×3 hash-dropped cap, no log
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz2 = -2; dz2 <= 2; dz2++) {
                            if (Math.abs(dx) + Math.abs(dz2) > 2) continue;
                            if (Math.abs(dx) == 2 || Math.abs(dz2) == 2) {
                                // Outer ring: hash-drop ~40%
                                if ((long) h2(wx * 3 + dx, wz * 3 + dz2) < 0) continue;
                            }
                            region.setBlock(new BlockPos(wx + dx, surfY + 1, wz + dz2), leaf, 3);
                        }
                    }
                    // Cap: 3×3 with hash-dropped corners
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz2 = -1; dz2 <= 1; dz2++) {
                            if (dx != 0 && dz2 != 0) {
                                if ((long) h2(wx * 7 + dx, wz * 7 + dz2) < 0) continue;
                            }
                            region.setBlock(new BlockPos(wx + dx, surfY + 2, wz + dz2), leaf, 3);
                        }
                    }

                } else if (variant == 2) {
                    // Broad bush: 1 log + radius-3 diamond base + 5-diamond crown
                    region.setBlock(mpos.set(wx, surfY + 1, wz), log, 3);
                    for (int dx = -3; dx <= 3; dx++) {
                        for (int dz2 = -3; dz2 <= 3; dz2++) {
                            if (Math.abs(dx) + Math.abs(dz2) > 3) continue;
                            if (dx == 0 && dz2 == 0) continue;
                            // Outermost ring: hash-drop ~40%
                            if (Math.abs(dx) + Math.abs(dz2) == 3) {
                                if ((long) h2(wx * 5 + dx, wz * 5 + dz2) < 0) continue;
                            }
                            region.setBlock(new BlockPos(wx + dx, surfY + 1, wz + dz2), leaf, 3);
                        }
                    }
                    // Crown: 5-diamond with hash-dropped outer
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz2 = -2; dz2 <= 2; dz2++) {
                            if (Math.abs(dx) + Math.abs(dz2) > 2) continue;
                            if (Math.abs(dx) + Math.abs(dz2) == 2) {
                                if ((long) h2(wx * 9 + dx, wz * 9 + dz2) < 0) continue;
                            }
                            region.setBlock(new BlockPos(wx + dx, surfY + 2, wz + dz2), leaf, 3);
                        }
                    }

                } else {
                    // Fuller bush: 1 log + wide 5-diamond base + 3×3 crown
                    region.setBlock(mpos.set(wx, surfY + 1, wz), log, 3);
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz2 = -2; dz2 <= 2; dz2++) {
                            if (Math.abs(dx) + Math.abs(dz2) > 2) continue;
                            if (dx == 0 && dz2 == 0) continue;
                            region.setBlock(new BlockPos(wx + dx, surfY + 1, wz + dz2), leaf, 3);
                        }
                    }
                    // Crown: full 3×3 with hash-dropped corners
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz2 = -1; dz2 <= 1; dz2++) {
                            if (dx != 0 && dz2 != 0) {
                                if ((long) h2(wx * 11 + dx, wz * 11 + dz2) < 0) continue;
                            }
                            region.setBlock(new BlockPos(wx + dx, surfY + 2, wz + dz2), leaf, 3);
                        }
                    }
                    if (((h >>> 22) & 0x3) != 0)
                        region.setBlock(new BlockPos(wx, surfY + 3, wz), leaf, 3);
                }
            }
        }
    }

    /** Scatters grass, ferns, and fairy-themed flowers across the island surface. */
    private static void placeIslandDecoration(WorldGenRegion region, ChunkAccess chunk) {
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();

        for (int lx = 0; lx < 16; lx++) {
            int wx = startX + lx;
            for (int lz = 0; lz < 16; lz++) {
                int wz = startZ + lz;
                if (!isIsland(wx, wz)) continue;

                long h = ((long) wx * 1299709L) ^ ((long) wz * 2147483647L) ^ 0xA3C5E7F1L;
                h = (h ^ (h >>> 16)) * 2246822519L; h ^= h >>> 13;
                if ((h & 0xFF) >= 90) continue; // ~35% decoration coverage

                int surfY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz);
                if (!chunk.getBlockState(new BlockPos(wx, surfY, wz)).is(Blocks.GRASS_BLOCK)) continue;
                BlockPos above = new BlockPos(wx, surfY + 1, wz);
                if (!region.getBlockState(above).isAir()) continue;

                int type = (int)((h >>> 8) & 0xF);

                if (type < 5) {
                    // Short grass (most common)
                    region.setBlock(above, Blocks.GRASS.defaultBlockState(), 3);
                } else if (type == 5) {
                    region.setBlock(above, Blocks.FERN.defaultBlockState(), 3);
                } else if (type == 6) {
                    region.setBlock(above, Blocks.ALLIUM.defaultBlockState(), 3);
                } else if (type == 7) {
                    region.setBlock(above, Blocks.LILY_OF_THE_VALLEY.defaultBlockState(), 3);
                } else if (type == 8) {
                    region.setBlock(above, Blocks.AZURE_BLUET.defaultBlockState(), 3);
                } else if (type == 9) {
                    region.setBlock(above, Blocks.BLUE_ORCHID.defaultBlockState(), 3);
                } else if (type == 10) {
                    region.setBlock(above, Blocks.OXEYE_DAISY.defaultBlockState(), 3);
                } else if (type == 11) {
                    region.setBlock(above, Blocks.CORNFLOWER.defaultBlockState(), 3);
                } else if (type == 12) {
                    region.setBlock(above, Blocks.POPPY.defaultBlockState(), 3);
                } else if (type == 13) {
                    // Tall grass: check room above
                    BlockPos upper = above.above();
                    if (region.getBlockState(upper).isAir()) {
                        region.setBlock(above, Blocks.TALL_GRASS.defaultBlockState()
                                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), 3);
                        region.setBlock(upper, Blocks.TALL_GRASS.defaultBlockState()
                                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), 3);
                    }
                } else if (type == 14) {
                    // Large fern: check room above
                    BlockPos upper = above.above();
                    if (region.getBlockState(upper).isAir()) {
                        region.setBlock(above, Blocks.LARGE_FERN.defaultBlockState()
                                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), 3);
                        region.setBlock(upper, Blocks.LARGE_FERN.defaultBlockState()
                                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), 3);
                    }
                } else {
                    region.setBlock(above, Blocks.DANDELION.defaultBlockState(), 3);
                }
            }
        }
    }

    @Override
    public void createStructures(RegistryAccess registryAccess,
            ChunkGeneratorStructureState structureState,
            StructureManager structureManager,
            ChunkAccess chunk,
            StructureTemplateManager structureTemplateManager) {
        // No vanilla structures (villages, outposts, etc.) in the fairy realm.
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState,
            BiomeManager biomeManager, StructureManager structureManager,
            ChunkAccess chunk, GenerationStep.Carving carving) {
        // Intentionally empty: caves are generated in fillFromNoise.
        // Running the vanilla aquifer here would flood caves with water.
    }

    @Override public void spawnOriginalMobs(WorldGenRegion region) { delegate.spawnOriginalMobs(region); }
    @Override public int getGenDepth() { return delegate.getGenDepth(); }
    @Override public int getSeaLevel() { return RIVER_LEVEL; }
    @Override public int getMinY()     { return delegate.getMinY(); }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level,
            RandomState randomState) {
        if (isWater(x, z)) {
            return (type == Heightmap.Types.OCEAN_FLOOR || type == Heightmap.Types.OCEAN_FLOOR_WG)
                    ? RIVER_FLOOR : RIVER_LEVEL + 1;
        }
        return Math.max(surfaceHeight(x, z), RIVER_LEVEL + 2) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        int minY = level.getMinBuildHeight(), height = level.getHeight();
        BlockState[] states = new BlockState[height];
        Arrays.fill(states, Blocks.AIR.defaultBlockState());
        boolean water = isWater(x, z);
        int solidTop  = water ? RIVER_FLOOR : Math.max(surfaceHeight(x, z), RIVER_LEVEL + 2);
        for (int y = minY; y <= solidTop; y++) {
            int i = y - minY;
            if (i < 0 || i >= height) continue;
            if (y <= minY + 3)         states[i] = Blocks.BEDROCK.defaultBlockState();
            else if (y < solidTop - 3) states[i] = stoneAt(y, x, z);
            else if (y < solidTop)     states[i] = Blocks.DIRT.defaultBlockState();
            else states[i] = water ? Blocks.GRAVEL.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (water) {
            for (int y = RIVER_FLOOR + 1; y <= RIVER_LEVEL; y++) {
                int i = y - minY;
                if (i >= 0 && i < height) states[i] = Blocks.WATER.defaultBlockState();
            }
        }
        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        delegate.addDebugScreenInfo(info, randomState, pos);
    }

    // ─── Geographic post-processing ──────────────────────────────────────────────

    private static void postProcessGeography(ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        for (int lx = 0; lx < 16; lx++) {
            int wx = startX + lx;
            for (int lz = 0; lz < 16; lz++) {
                int wz = startZ + lz;
                if (isIsland(wx, wz)) { flattenIsland(chunk, pos, wx, wz); continue; }
                if (isIslandMoat(wx, wz)) {
                    double rr = Math.sqrt((long) wx * wx + (long) wz * wz);
                    double distFromInner = rr - ISLAND_RADIUS;
                    double distFromOuter = ISLAND_MOAT_OUTER - rr;
                    if (distFromInner < MOAT_INNER_BANK_W) {
                        // Inner bank slope: heights already set by fillFromNoise, just material
                        blendTowardRiver(chunk, pos, wx, wz, distFromInner);
                    } else {
                        double moatDist = -Math.min(distFromInner, distFromOuter);
                        carveToWater(chunk, pos, wx, wz, moatDist);
                    }
                    continue;
                }

                double rEdge = riverEdgeDist(wx, wz);
                double moatOuterDist = Math.sqrt((long) wx * wx + (long) wz * wz) - ISLAND_MOAT_OUTER;
                double distToWater = Math.min(rEdge, moatOuterDist);

                if (distToWater < 0)                 carveToWater(chunk, pos, wx, wz, distToWater);
                else if (distToWater < BLEND_RADIUS)  blendTowardRiver(chunk, pos, wx, wz, distToWater);
            }
        }
    }

    /**
     * Carve a water column. distToWater is negative (how far inside the water zone).
     * The same sandWidth noise used by blendTowardRiver is evaluated here so the sandy
     * bank material flows continuously from the bank into the shallow river floor with
     * no hard material boundary at the water's edge.
     */
    private static void carveToWater(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
            int wx, int wz, double distToWater) {
        double depthIntoWater = -distToWater; // positive = how far inside water zone

        int depthRaw = (int)(1.6 * n2(wx * 0.025, wz * 0.027)
                           + 0.9 * n2(wx * 0.080, wz * 0.082));
        int floorY = Math.max(RIVER_FLOOR - 2, Math.min(RIVER_FLOOR + 2, RIVER_FLOOR + depthRaw));

        for (int y = chunk.getMaxBuildHeight() - 1; y > floorY; y--) {
            if (!chunk.getBlockState(pos.set(wx, y, wz)).isAir()) {
                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
            }
        }

        // Same noise pattern as blendTowardRiver; sand only within 1-4 blocks of
        // the bank edge; gravel/clay takes over for the majority of the river floor.
        double sandWidth = 4.5
                + 1.5 * n2(wx * 0.055, wz * 0.058)
                + 1.0 * n2(wx * 0.150, wz * 0.155);

        BlockState floorTop, floorSub;
        if (depthIntoWater < sandWidth) {
            floorTop = Blocks.SAND.defaultBlockState();
            floorSub = Blocks.SAND.defaultBlockState();
        } else {
            double matA = n2(wx * 0.18, wz * 0.20);
            double matB = n2(wx * 0.55, wz * 0.53);
            double mat  = 0.60 * matA + 0.40 * matB;
            if (mat > 0.28) {
                floorTop = Blocks.SAND.defaultBlockState();
                floorSub = Blocks.SAND.defaultBlockState();
            } else if (mat < -0.30) {
                floorTop = Blocks.CLAY.defaultBlockState();
                floorSub = Blocks.GRAVEL.defaultBlockState();
            } else {
                floorTop = Blocks.GRAVEL.defaultBlockState();
                floorSub = Blocks.GRAVEL.defaultBlockState();
            }
        }
        chunk.setBlockState(pos.set(wx, floorY, wz), floorTop, false);
        if (floorY - 1 >= chunk.getMinBuildHeight()) {
            chunk.setBlockState(pos.set(wx, floorY - 1, wz), floorSub, false);
        }

        for (int y = floorY + 1; y <= RIVER_LEVEL; y++) {
            chunk.setBlockState(pos.set(wx, y, wz), Blocks.WATER.defaultBlockState(), false);
        }

        // Plant life: deterministic hash per column, no grid patterns
        long h = ((long) wx * 374761393L) ^ ((long) wz * 668265263L) ^ 0xABCDEF12L;
        h = (h ^ (h >>> 16)) * 2246822519L; h ^= h >>> 13;
        int r = (int)(h & 0xFF);

        int seagrassY = floorY + 1;
        boolean noSeagrass = floorTop.is(Blocks.CLAY);
        if (!noSeagrass && seagrassY <= RIVER_LEVEL) {
            if (r < 55) {
                chunk.setBlockState(pos.set(wx, seagrassY, wz),
                        Blocks.SEAGRASS.defaultBlockState(), false);
            } else if (r < 72 && seagrassY + 1 <= RIVER_LEVEL) {
                chunk.setBlockState(pos.set(wx, seagrassY, wz),
                        Blocks.TALL_SEAGRASS.defaultBlockState()
                                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), false);
                chunk.setBlockState(pos.set(wx, seagrassY + 1, wz),
                        Blocks.TALL_SEAGRASS.defaultBlockState()
                                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), false);
            }
        }

        if ((h & 0x3F) == 0) {
            chunk.setBlockState(pos.set(wx, RIVER_LEVEL + 1, wz),
                    Blocks.LILY_PAD.defaultBlockState(), false);
        }
    }

    /**
     * Assign surface material for blend-zone columns whose terrain height was already
     * set by fillFromNoise.
     *
     * Above water: sand within beachHeight blocks of RIVER_LEVEL, grass beyond.
     * This mirrors the island beach logic and naturally produces a visible sandy
     * strip right where the slope meets the waterline regardless of edgeDist.
     *
     * Below water: sand for the first 3 depths, noise-blended sand/gravel for
     * depths 4-5, pure gravel beyond, producing a smooth
     * sand→gravel transition on the submerged bank.
     */
    private static void blendTowardRiver(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
            int wx, int wz, double edgeDist) {
        int terrainTop = findTopSolid(chunk, pos, wx, wz);

        if (terrainTop >= RIVER_LEVEL) {
            // Above water: sandy beach within beachHeight blocks of the waterline
            double beachHeight = 4.0 + 2.0 * n2(wx * 0.09, wz * 0.11); // ≈ 2–6 blocks
            boolean isSand = (terrainTop - RIVER_LEVEL) < beachHeight;
            chunk.setBlockState(pos.set(wx, terrainTop, wz),
                    isSand ? Blocks.SAND.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState(), false);
            if (terrainTop > chunk.getMinBuildHeight()) {
                chunk.setBlockState(pos.set(wx, terrainTop - 1, wz),
                        isSand ? Blocks.SAND.defaultBlockState() : Blocks.DIRT.defaultBlockState(), false);
            }
        } else {
            // Submerged slope: sand near surface → mix → gravel deeper
            int depth = RIVER_LEVEL - terrainTop; // 1 = just below surface
            BlockState mat;
            if (depth <= 3) {
                mat = Blocks.SAND.defaultBlockState();
            } else if (depth <= 5) {
                // Noisy transition band
                mat = (n2(wx * 0.28, wz * 0.31) > 0.0)
                        ? Blocks.SAND.defaultBlockState()
                        : Blocks.GRAVEL.defaultBlockState();
            } else {
                mat = Blocks.GRAVEL.defaultBlockState();
            }
            chunk.setBlockState(pos.set(wx, terrainTop, wz), mat, false);
            if (terrainTop > chunk.getMinBuildHeight()) {
                chunk.setBlockState(pos.set(wx, terrainTop - 1, wz), mat, false);
            }
            // Seagrass on the submerged slope extends coverage toward the banks
            int seagrassY = terrainTop + 1;
            if (seagrassY <= RIVER_LEVEL) {
                long h = ((long) wx * 668265263L) ^ ((long) wz * 374761393L) ^ 0xFEDCBA98L;
                h = (h ^ (h >>> 16)) * 2246822519L; h ^= h >>> 13;
                int rv = (int)(h & 0xFF);
                if (rv < 40) {
                    chunk.setBlockState(pos.set(wx, seagrassY, wz),
                            Blocks.SEAGRASS.defaultBlockState(), false);
                } else if (rv < 55 && seagrassY + 1 <= RIVER_LEVEL) {
                    chunk.setBlockState(pos.set(wx, seagrassY, wz),
                            Blocks.TALL_SEAGRASS.defaultBlockState()
                                    .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), false);
                    chunk.setBlockState(pos.set(wx, seagrassY + 1, wz),
                            Blocks.TALL_SEAGRASS.defaultBlockState()
                                    .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), false);
                }
            }
        }
    }

    private static void flattenIsland(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
            int wx, int wz) {
        double r = Math.sqrt((long) wx * wx + (long) wz * wz);
        double t = (r < ISLAND_RADIUS - 12) ? 0.0
                : (r - (ISLAND_RADIUS - 12)) / 12.0;
        t = Math.min(1.0, t); t = t * t * (3.0 - 2.0 * t);
        int targetY = (int)(ISLAND_FLAT_Y - t * (ISLAND_FLAT_Y - RIVER_LEVEL));
        if (targetY < RIVER_LEVEL) targetY = RIVER_LEVEL;

        int solidTop = findTopSolid(chunk, pos, wx, wz);
        if (solidTop > targetY) {
            for (int y = solidTop; y > targetY; y--) {
                chunk.setBlockState(pos.set(wx, y, wz), Blocks.AIR.defaultBlockState(), false);
            }
        } else if (solidTop < targetY) {
            for (int y = solidTop + 1; y <= targetY; y++) {
                chunk.setBlockState(pos.set(wx, y, wz), Blocks.STONE.defaultBlockState(), false);
            }
        }
        if (targetY >= RIVER_LEVEL) {
            // Sandy beach in the outer ring of the island, matching the moat bank material
            double distFromMoat = ISLAND_RADIUS - r;
            double beachWidth = 5.0 + 2.0 * n2(wx * 0.09, wz * 0.11);
            boolean isBeach = distFromMoat < beachWidth;
            BlockState surface    = isBeach ? Blocks.SAND.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
            BlockState subSurface = isBeach ? Blocks.SAND.defaultBlockState() : Blocks.DIRT.defaultBlockState();
            if (targetY > chunk.getMinBuildHeight()) {
                chunk.setBlockState(pos.set(wx, targetY - 1, wz), subSurface, false);
            }
            chunk.setBlockState(pos.set(wx, targetY, wz), surface, false);

            // Sugar cane only on the single-block strip right at the water edge
            if (isBeach && targetY == RIVER_LEVEL && distFromMoat < 1.5 + 0.5 * n2(wx * 0.17, wz * 0.19)) {
                double caneNoise = h2(wx * 7 + 991, wz * 13 + 337); // [-1, 1]
                if (caneNoise > 0.40) { // ~30 % of eligible cells
                    int caneHeight = 1 + (int)((caneNoise - 0.40) / 0.60 * 3); // 1-3 blocks
                    caneHeight = Math.min(caneHeight, 3);
                    for (int cy = 1; cy <= caneHeight; cy++) {
                        chunk.setBlockState(pos.set(wx, targetY + cy, wz),
                                Blocks.SUGAR_CANE.defaultBlockState(), false);
                    }
                }
            }
        }
    }

    private static int findTopSolid(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int wx, int wz) {
        for (int y = chunk.getMaxBuildHeight() - 1; y >= chunk.getMinBuildHeight(); y--) {
            BlockState st = chunk.getBlockState(pos.set(wx, y, wz));
            if (!st.isAir() && !st.is(Blocks.WATER)) return y;
        }
        return chunk.getMinBuildHeight();
    }
}
