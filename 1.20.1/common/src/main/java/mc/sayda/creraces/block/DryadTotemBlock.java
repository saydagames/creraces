package mc.sayda.creraces.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ported from CreRaces Classic's Forest Totem. Not yet wired into any race trait/ability
 * (registered as a placeable block only), intentionally left inert for now.
 * Hitbox matches NymphNodeBlock's (Aurai/Naiad/Oread), which shares the same totem model.
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
