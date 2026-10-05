package mc.sayda.creraces.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Forest Totem from CreRaces Classic. Placeable but inert for now: no race trait or ability
 * uses it yet. Shares the totem model, and therefore the hitbox, with NymphNodeBlock.
 */
@SuppressWarnings("null")
public class DryadTotemBlock extends Block {

    private static final VoxelShape SHAPE = box(4, 0, 4, 12, 14, 12);

    public DryadTotemBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
