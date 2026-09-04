package mc.sayda.creraces.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Non-full-block root block for Nymph node blocks (Aurai, Naiad, Oread).
 * Hitbox matches the totem model bounding box (x:4-12, y:0-14, z:4-12).
 */
@SuppressWarnings("null")
public class NymphNodeBlock extends RootBlock {

    private static final VoxelShape SHAPE = box(4, 0, 4, 12, 14, 12);

    public NymphNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
