package mc.sayda.creraces.block.entity;

import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ToriiBellBlockEntity extends BellBlockEntity {

    public ToriiBellBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlocks.TORII_BELL_ENTITY.get();
    }
}
