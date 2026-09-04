package mc.sayda.creraces.worldgen;

import mc.sayda.creraces.ability.EssenceRegistry;
import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.block.EssenceClusterBlock;
import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class EssenceVortexFeature extends Feature<NoneFeatureConfiguration> {

    public EssenceVortexFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if (!CreRacesConfig.ESSENCE_VORTEX_WORLDGEN_ENABLED.get()) return false;
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin(); // at surface Y from heightmap placement
        RandomSource random = ctx.random();

        // Pick a random essence type from all registered types
        EssenceType[] types = EssenceType.values();
        EssenceType chosenType = types[random.nextInt(types.length)];

        Block vortexBlock = EssenceRegistry.VORTEXES.get(chosenType).get();
        Block clusterBlock = EssenceRegistry.CLUSTERS.get(chosenType).get();

        if (!level.getFluidState(origin).isEmpty()) return false;

        // Verify 4 blocks of clearance above the surface for the vortex
        for (int i = 1; i <= 4; i++) {
            if (!level.isEmptyBlock(origin.above(i))) return false;
        }

        level.setBlock(origin.above(4), vortexBlock.defaultBlockState(), 2);

        // Scatter clusters within 5-block horizontal radius on exposed solid faces
        int targetCount = 4 + random.nextInt(5); // 4–8 clusters
        int placed = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int attempt = 0; attempt < 60 && placed < targetCount; attempt++) {
            int dx = random.nextInt(11) - 5; // –5 to +5
            int dz = random.nextInt(11) - 5;
            if (dx * dx + dz * dz > 25) continue; // keep inside circle of radius 5

            // Scan from a few blocks above to a few below origin Y for a solid surface
            for (int dy = 3; dy >= -4; dy--) {
                mutable.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                BlockState below = level.getBlockState(mutable);
                BlockPos abovePos = mutable.above();
                if (!below.isAir() && level.isEmptyBlock(abovePos)
                        && below.isFaceSturdy(level, mutable, Direction.UP)) {
                    level.setBlock(abovePos,
                            clusterBlock.defaultBlockState().setValue(EssenceClusterBlock.FACING, Direction.UP), 2);
                    placed++;
                    break;
                }
            }
        }

        return true;
    }
}
