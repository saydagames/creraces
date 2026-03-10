package mc.sayda.creraces.block;

import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DryadLeavesFloweringBlock extends LeavesBlock {
    public DryadLeavesFloweringBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);

        // 4% chance scaled by randomTickSpeed
        if (random.nextFloat() < (0.04f * level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING))) {
            level.setBlockAndUpdate(pos, ModBlocks.DRYAD_LEAVES_FRUIT.get().withPropertiesOf(state));
        }
    }
}
