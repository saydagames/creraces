package mc.sayda.creraces.block;

import mc.sayda.creraces.block.entity.ElysianVeilBloomBlockEntity;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.WorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings("null")
public class ElysianVeilBloomBlock extends BaseEntityBlock {

    public static final com.mojang.serialization.MapCodec<ElysianVeilBloomBlock> CODEC = simpleCodec(ElysianVeilBloomBlock::new);
    public static final BooleanProperty BLOOMING = BooleanProperty.create("blooming");
    private static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 10.0, 11.0);

    public ElysianVeilBloomBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BLOOMING, false));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends ElysianVeilBloomBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BLOOMING);
    }

    protected boolean mayPlaceOn(BlockState groundState, BlockGetter world, BlockPos pos) {
        return groundState.is(BlockTags.DIRT) || groundState.is(Blocks.FARMLAND);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getFluidState(pos.above()).isEmpty()
                && mayPlaceOn(level.getBlockState(below), level, below);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
            LevelAccessor level, BlockPos pos, BlockPos facingPos) {
        return !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, facing, facingState, level, pos, facingPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean shouldGlow = WorldState.isSpiritMoon(level);
        if (state.getValue(BLOOMING) != shouldGlow) {
            level.setBlockAndUpdate(pos, state.setValue(BLOOMING, shouldGlow));
        }
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
            @javax.annotation.Nullable BlockEntity blockEntity, ItemStack tool) {
        player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        boolean canHarvest = DataUtils.getVariables(player).map(v -> v.isInSpiritRealm()).orElse(false)
                || WorldState.isSpiritMoon(level);
        if (canHarvest) {
            popResource(level, pos, new ItemStack(this.asItem()));
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElysianVeilBloomBlockEntity(pos, state);
    }
}
