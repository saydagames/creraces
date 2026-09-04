package mc.sayda.creraces.block;

import mc.sayda.creraces.block.entity.ResearchTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ResearchTableBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<ResearchTablePart> PART = EnumProperty.create("part", ResearchTablePart.class);

    private static final Map<Direction, VoxelShape> LEFT_SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> RIGHT_SHAPES = new EnumMap<>(Direction.class);

    static {
        // Shapes for FACING=NORTH; each facing rotates via blockstate y-rotation rules
        // LEFT NORTH: legs at x=1-3, shelf extends east (x=1.1-16)
        LEFT_SHAPES.put(Direction.NORTH, Shapes.or(
            Block.box(1, 0, 1, 3, 14, 3),
            Block.box(1, 0, 13, 3, 14, 15),
            Block.box(0, 14, 0, 16, 16, 16),
            Block.box(1.1, 7, 1.1, 16, 8, 14.9)
        ));
        // LEFT EAST (y=90): rotate NORTH 90° CW: (minX,minZ,maxX,maxZ) → (16-maxZ, minX, 16-minZ, maxX)
        LEFT_SHAPES.put(Direction.EAST, Shapes.or(
            Block.box(13, 0, 1, 15, 14, 3),
            Block.box(1, 0, 1, 3, 14, 3),
            Block.box(0, 14, 0, 16, 16, 16),
            Block.box(1.1, 7, 1.1, 14.9, 8, 16)
        ));
        // LEFT SOUTH (y=180): rotate NORTH 180°
        LEFT_SHAPES.put(Direction.SOUTH, Shapes.or(
            Block.box(13, 0, 13, 15, 14, 15),
            Block.box(13, 0, 1, 15, 14, 3),
            Block.box(0, 14, 0, 16, 16, 16),
            Block.box(0, 7, 1.1, 14.9, 8, 14.9)
        ));
        // LEFT WEST (y=270): rotate NORTH 270° CW
        LEFT_SHAPES.put(Direction.WEST, Shapes.or(
            Block.box(1, 0, 13, 3, 14, 15),
            Block.box(13, 0, 13, 15, 14, 15),
            Block.box(0, 14, 0, 16, 16, 16),
            Block.box(1.1, 7, 0, 14.9, 8, 14.9)
        ));
        // RIGHT NORTH: legs at x=13-15, shelf extends west (x=0-14.9)
        RIGHT_SHAPES.put(Direction.NORTH, Shapes.or(
            Block.box(13, 0, 1, 15, 14, 3),
            Block.box(13, 0, 13, 15, 14, 15),
            Block.box(0, 14, 0, 16, 16, 16),
            Block.box(0, 7, 1.1, 14.9, 8, 14.9)
        ));
        RIGHT_SHAPES.put(Direction.EAST, Shapes.or(
            Block.box(13, 0, 13, 15, 14, 15),
            Block.box(1, 0, 13, 3, 14, 15),
            Block.box(0, 14, 0, 16, 16, 16),
            Block.box(1.1, 7, 0, 14.9, 8, 14.9)
        ));
        RIGHT_SHAPES.put(Direction.SOUTH, Shapes.or(
            Block.box(1, 0, 13, 3, 14, 15),
            Block.box(1, 0, 1, 3, 14, 3),
            Block.box(0, 14, 0, 16, 16, 16),
            Block.box(1.1, 7, 1.1, 16, 8, 14.9)
        ));
        RIGHT_SHAPES.put(Direction.WEST, Shapes.or(
            Block.box(1, 0, 1, 3, 14, 3),
            Block.box(13, 0, 1, 15, 14, 3),
            Block.box(0, 14, 0, 16, 16, 16),
            Block.box(1.1, 7, 1.1, 14.9, 8, 16)
        ));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return state.getValue(PART) == ResearchTablePart.LEFT
                ? LEFT_SHAPES.get(facing)
                : RIGHT_SHAPES.get(facing);
    }

    public ResearchTableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, ResearchTablePart.LEFT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos pos = context.getClickedPos();
        BlockPos rightPos = pos.relative(facing.getClockWise());
        Level level = context.getLevel();
        if (level.getBlockState(rightPos).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(rightPos)) {
            return this.defaultBlockState().setValue(FACING, facing).setValue(PART, ResearchTablePart.LEFT);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            BlockPos rightPos = pos.relative(facing.getClockWise());
            level.setBlock(rightPos, state.setValue(PART, ResearchTablePart.RIGHT), 3);
            level.blockUpdated(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, 3);
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            ResearchTablePart part = state.getValue(PART);
            BlockPos partnerPos = part == ResearchTablePart.LEFT
                    ? pos.relative(facing.getClockWise())
                    : pos.relative(facing.getCounterClockWise());
            BlockState partnerState = level.getBlockState(partnerPos);
            if (partnerState.is(this) && partnerState.getValue(PART) != part) {
                level.setBlock(partnerPos, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, partnerPos, Block.getId(partnerState));
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Belt-and-suspenders guard against creative-mode drops: the loot table should only ever
     * be consulted by vanilla's own destroy flow in survival, but this intercepts at the single
     * choke point every drop path (dropResources/playerDestroy/etc) funnels through, so a
     * creative/instabuild breaker never gets a physical item regardless of how the break was
     * triggered.
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        Entity entity = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return List.of();
        }
        return super.getDrops(state, params);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        ResearchTablePart part = state.getValue(PART);
        Direction partnerDir = part == ResearchTablePart.LEFT
                ? facing.getClockWise()
                : facing.getCounterClockWise();
        if (direction == partnerDir) {
            if (neighborState.is(this) && neighborState.getValue(PART) != part) {
                return state;
            }
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        Direction facing = state.getValue(FACING);
        ResearchTablePart part = state.getValue(PART);
        BlockPos mainPos = part == ResearchTablePart.LEFT
                ? pos.relative(facing.getClockWise())
                : pos;

        if (level.getBlockEntity(mainPos) instanceof ResearchTableBlockEntity tableEntity
                && player instanceof ServerPlayer sp) {
            dev.architectury.registry.menu.MenuRegistry.openExtendedMenu(sp, tableEntity);
            mc.sayda.creraces.network.BoundaryHandler.syncHexGrid(sp, tableEntity);
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(PART) == ResearchTablePart.RIGHT) {
            return new ResearchTableBlockEntity(pos, state);
        }
        return null;
    }
}
