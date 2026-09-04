package mc.sayda.creraces.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;

/** Transient block placed by race abilities. Ported from CreRaces Classic. */
@SuppressWarnings("null")
public class SummonedDirtBlock extends Block {
    public SummonedDirtBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.GRAVEL).instabreak().replaceable());
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 15;
    }
}
