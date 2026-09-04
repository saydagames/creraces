package mc.sayda.creraces.block;

import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EssenceClusterBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final VoxelShape SHAPE_UP    = Block.box(3, 0, 3, 13, 11, 13);
    private static final VoxelShape SHAPE_DOWN  = Block.box(3, 5, 3, 13, 16, 13);
    private static final VoxelShape SHAPE_NORTH = Block.box(3, 3, 5, 13, 13, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(3, 3, 0, 13, 13, 11);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 3, 3, 11, 13, 13);
    private static final VoxelShape SHAPE_WEST  = Block.box(5, 3, 3, 16, 13, 13);

    private final EssenceType essenceType;

    public EssenceClusterBlock(EssenceType essenceType) {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.QUARTZ)
            .strength(0.5f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.AMETHYST_CLUSTER)
            .lightLevel(state -> 7)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY));
        this.essenceType = essenceType;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    public EssenceType getEssenceType() {
        return essenceType;
    }

    @Override
    public net.minecraft.network.chat.MutableComponent getName() {
        return net.minecraft.network.chat.Component.translatable("block.creraces.essence_cluster",
                net.minecraft.network.chat.Component.translatable("essence.creraces." + essenceType.getSerializedName()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case DOWN  -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST  -> SHAPE_EAST;
            case WEST  -> SHAPE_WEST;
            default    -> SHAPE_UP;
        };
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean drop) {
        super.spawnAfterBreak(state, level, pos, tool, drop);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) != 0) return;
        int color = essenceType.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        Direction facing = state.getValue(FACING);
        // emit from the crystal tip
        double px = pos.getX() + 0.5 + facing.getStepX() * 0.55 + (random.nextDouble() - 0.5) * 0.3;
        double py = pos.getY() + 0.5 + facing.getStepY() * 0.55 + (random.nextDouble() - 0.5) * 0.3;
        double pz = pos.getZ() + 0.5 + facing.getStepZ() * 0.55 + (random.nextDouble() - 0.5) * 0.3;
        level.addParticle(ModParticles.ESSENCE_PARTICLE.get(), px, py, pz, r, g, b);
    }

}
