package mc.sayda.creraces.block;

import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

public class DryadLeavesFloweringBlock extends LeavesBlock {
    public DryadLeavesFloweringBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos,
            @Nonnull RandomSource random) {
        super.randomTick(state, level, pos, random);
        // The leaf decay in super may have just removed this block.
        if (!level.getBlockState(pos).is(this)) {
            return;
        }

        // 4% chance; the engine already scales call frequency by randomTickSpeed
        if (random.nextFloat() < 0.04f) {
            level.setBlockAndUpdate(pos, ModBlocks.DRYAD_LEAVES_FRUIT.get().withPropertiesOf(state));
        }
    }
}
