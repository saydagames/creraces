package mc.sayda.creraces.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.Optional;

/**
 * Jigsaw structure that places with Y=0 at (worldSurface - surfaceOffset).
 * Set surfaceOffset=3 when the NBT ground layer is at local Y=3 so the
 * grass aligns with terrain instead of floating 3 blocks above it.
 */
public class SurfaceOffsetJigsawStructure extends Structure {

    public static StructureType<SurfaceOffsetJigsawStructure> TYPE;

    public static final MapCodec<SurfaceOffsetJigsawStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Structure.settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter),
            Codec.INT.optionalFieldOf("surface_offset", 0).forGetter(s -> s.surfaceOffset)
        ).apply(instance, SurfaceOffsetJigsawStructure::new)
    );

    private final Holder<StructureTemplatePool> startPool;
    private final int maxDistanceFromCenter;
    private final int surfaceOffset;

    public SurfaceOffsetJigsawStructure(StructureSettings settings,
                                        Holder<StructureTemplatePool> startPool,
                                        int maxDistanceFromCenter,
                                        int surfaceOffset) {
        super(settings);
        this.startPool = startPool;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.surfaceOffset = surfaceOffset;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        int surfaceY = context.chunkGenerator().getFirstOccupiedHeight(
            x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()
        );
        BlockPos startPos = new BlockPos(x, surfaceY - surfaceOffset, z);
        return JigsawPlacement.addPieces(
            context,
            startPool,
            Optional.empty(),
            1,
            startPos,
            false,
            Optional.empty(),
            maxDistanceFromCenter
        );
    }

    @Override
    public StructureType<?> type() {
        return TYPE;
    }
}
