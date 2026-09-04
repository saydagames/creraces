package mc.sayda.creraces.block;

import mc.sayda.creraces.block.entity.VeilMushroomBlockEntity;
import mc.sayda.creraces.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VeilMushroomBlock extends BaseEntityBlock {

    public static final com.mojang.serialization.MapCodec<VeilMushroomBlock> CODEC = simpleCodec(VeilMushroomBlock::new);
    private static final VoxelShape SHAPE = box(5.0, 0.0, 5.0, 11.0, 6.0, 11.0);

    public VeilMushroomBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends VeilMushroomBlock> codec() {
        return CODEC;
    }

    // Shape

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // Survival (replicates BushBlock behaviour)

    protected boolean mayPlaceOn(BlockState groundState, BlockGetter world, BlockPos pos) {
        return groundState.is(BlockTags.DIRT) || groundState.is(Blocks.FARMLAND);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState atPos = level.getBlockState(pos);
        if (!atPos.isAir() && !atPos.is(this)) return false;
        BlockPos below = pos.below();
        return mayPlaceOn(level.getBlockState(below), level, below);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
            LevelAccessor level, BlockPos pos, BlockPos facingPos) {
        if (!state.canSurvive(level, pos)) {
            level.scheduleTick(pos, this, 1);
        }
        return super.updateShape(state, facing, facingState, level, pos, facingPos);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    // Particles

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.isDay()) return;
        int count = 1 + (random.nextFloat() < 0.35f ? 1 : 0);
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + random.nextDouble() * 6.0 - 3.0;
            double y = pos.getY() + 0.5 + random.nextDouble() * 2.0;
            double z = pos.getZ() + random.nextDouble() * 6.0 - 3.0;
            level.addParticle(ModParticles.VEIL_MIST.get(), x, y, z, 0, 0, 0);
        }
    }

    // Block entity

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VeilMushroomBlockEntity(pos, state);
    }
}
