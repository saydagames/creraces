package mc.sayda.creraces.block.entity;

import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class VeilWillowSaplingBlockEntity extends BlockEntity {
    public VeilWillowSaplingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.VEIL_WILLOW_SAPLING_BE.get(), pos, state);
    }
}
