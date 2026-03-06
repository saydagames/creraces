package mc.sayda.creraces.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Rat Hole — a flat, nearly invisible block placed by Ratkin's Rat Tunnels
 * ability.
 * <ul>
 * <li>No collision (entities walk through it).</li>
 * <li>No occlusion (transparent to light).</li>
 * <li>Gravel-like sound and feel.</li>
 * <li>Indestructible by normal means (unbreakable, explosion-immune).</li>
 * </ul>
 *
 * Teleportation logic is handled entirely by the engine via
 * {@code creraces:teleport}
 * and {@code creraces:set_customization} actions in {@code rat_tunnels.json}.
 * This block is purely a visual marker placed on the ground.
 */
public class RatHoleBlock extends Block {

    // 15×1×15 pixel-thin floor-level hitbox, same as legacy
    private static final VoxelShape SHAPE = Block.box(0.5, 0, 0.5, 15.5, 1, 15.5);

    public RatHoleBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.GRAVEL)
                .strength(-1.0f, 3600000.0f) // indestructible
                .noCollission()
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK)
                .isRedstoneConductor((bs, bl, bp) -> false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return 0;
    }
}
