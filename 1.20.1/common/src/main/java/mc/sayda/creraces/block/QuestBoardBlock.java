package mc.sayda.creraces.block;

import mc.sayda.creraces.block.entity.QuestBoardBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * A 3-wide x 2-tall wall, auto-placed all at once around the clicked block - mirrors
 * ResearchTableBlock's single-partner auto-placement, generalized to 5 required cells (the
 * bottom-middle cell has no visible geometry in the model and is never placed - see
 * isOptional()). The clicked position becomes column 0 / row 0 of the wall (bottom-left,
 * relative to the placing player's facing); the wall extends via FACING.getClockWise() and
 * upward. Each cell stores its own FACING/COL/ROW, so any cell can deterministically compute
 * the whole structure's layout without a separate detection/scan step.
 */
public class QuestBoardBlock extends BaseEntityBlock {

    public static final int WIDTH = 3;
    public static final int HEIGHT = 2;
    public static final int MASTER_COL = 0;

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty COL = IntegerProperty.create("col", 0, WIDTH - 1);
    public static final IntegerProperty ROW = IntegerProperty.create("row", 0, HEIGHT - 1);

    public QuestBoardBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH).setValue(COL, 0).setValue(ROW, 0));
    }

    // Raw (unrotated, "facing=north" reference frame) geometry mirroring the Blockbench
    // models exactly, clipped to each cell's own 0-16 local Y range - col 0/2 share the post
    // model's boxes (col 2 is the same raw geometry rotated an extra 180deg, matching how the
    // blockstate reuses quest_board_post with y+180 rather than a separate mirrored model).
    // The middle column forms the crossbar of an upside-down U: its one required block sits
    // at the top (row 1), spanning between the two posts' tops - the bottom-middle cell
    // (col 1, row 0) has no geometry at all and is never placed, see isOptional().
    private static final double[][] POST_ROW0 = {
            {10, 0, 6, 14, 1, 10},      // foot
            {11, 1, 7, 13, 16, 9},      // post, clipped to this cell's height
    };
    private static final double[][] POST_ROW1 = {
            {11, 0, 7, 13, 16, 9},      // post continuation, fills this cell's full height
            {0, 0, 7.5, 11, 15, 8.5},   // board panel continuation
    };
    private static final double[][] PANEL_ROW1 = {
            {0, 0, 7.5, 16, 15, 8.5},
    };

    // Indexed [facing steps clockwise from north][col][row].
    private static final VoxelShape[][][] SHAPES = buildShapes();

    private static VoxelShape[][][] buildShapes() {
        VoxelShape[][][] shapes = new VoxelShape[4][WIDTH][HEIGHT];
        for (int steps = 0; steps < 4; steps++) {
            // Hitbox-only correction: the side-column (post) collision boxes need an extra 180deg
            // relative to the model specifically when facing north/south (steps 0 and 2) - the
            // model's own rotation (blockstate y values) is untouched, this only affects getShape().
            int sideSteps = (steps == 0 || steps == 2) ? (steps + 2) % 4 : steps;
            shapes[steps][0][0] = rotatedUnion(POST_ROW0, sideSteps);
            shapes[steps][0][1] = rotatedUnion(POST_ROW1, sideSteps);
            shapes[steps][1][0] = Shapes.empty(); // col 1, row 0 is never placed - see isOptional()
            shapes[steps][1][1] = rotatedUnion(PANEL_ROW1, steps);
            shapes[steps][2][0] = rotatedUnion(POST_ROW0, (sideSteps + 2) % 4);
            shapes[steps][2][1] = rotatedUnion(POST_ROW1, (sideSteps + 2) % 4);
        }
        return shapes;
    }

    private static VoxelShape rotatedUnion(double[][] boxes, int steps) {
        VoxelShape shape = Shapes.empty();
        for (double[] b : boxes) {
            shape = Shapes.join(shape, rotatedBox(b[0], b[1], b[2], b[3], b[4], b[5], steps), BooleanOp.OR);
        }
        return shape;
    }

    /** Rotates a box {@code steps} x 90deg clockwise (viewed from above) around the cell's vertical center. */
    private static VoxelShape rotatedBox(double x1, double y1, double z1, double x2, double y2, double z2, int steps) {
        double rx1 = x1, rz1 = z1, rx2 = x2, rz2 = z2;
        for (int i = 0; i < steps; i++) {
            double nx1 = rz1, nz1 = 16 - rx1;
            double nx2 = rz2, nz2 = 16 - rx2;
            rx1 = nx1; rz1 = nz1; rx2 = nx2; rz2 = nz2;
        }
        return Block.box(Math.min(rx1, rx2), y1, Math.min(rz1, rz2), Math.max(rx1, rx2), y2, Math.max(rz1, rz2));
    }

    private static int steps(Direction facing) {
        return switch (facing) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SHAPES[steps(state.getValue(FACING))][state.getValue(COL)][state.getValue(ROW)];
    }

    @Override
    public boolean isPathfindable(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
            PathComputationType pathComputationType) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, COL, ROW);
    }

    // Keeps FACING in sync with structure rotation, since originPos/masterPos/isIntact derive a
    // cell's neighbors from FACING+COL+ROW alone.
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public static boolean isMaster(BlockState state) {
        return state.getValue(COL) == MASTER_COL && state.getValue(ROW) == 0;
    }

    /** The bottom-middle cell has no visible geometry (the crossbar sits at the top), so it's never required/placed. */
    private static boolean isOptional(int col, int row) {
        return col == 1 && row == 0;
    }

    /** The bottom-left (col 0, row 0) position of the wall this cell belongs to. */
    public static BlockPos originPos(BlockState state, BlockPos pos) {
        Direction sideDir = state.getValue(FACING).getClockWise();
        return pos.relative(sideDir, -state.getValue(COL)).below(state.getValue(ROW));
    }

    public static BlockPos masterPos(BlockState state, BlockPos pos) {
        BlockPos origin = originPos(state, pos);
        Direction sideDir = state.getValue(FACING).getClockWise();
        return origin.relative(sideDir, MASTER_COL);
    }

    /** True if every required cell (all but the optional bottom-middle) still holds a quest board block. */
    public static boolean isIntact(Level level, BlockState state, BlockPos pos) {
        BlockPos origin = originPos(state, pos);
        Direction sideDir = state.getValue(FACING).getClockWise();
        for (int c = 0; c < WIDTH; c++) {
            for (int r = 0; r < HEIGHT; r++) {
                if (isOptional(c, r)) continue;
                if (!level.getBlockState(origin.relative(sideDir, c).above(r)).is(state.getBlock())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        Direction sideDir = facing.getClockWise();
        BlockPos origin = context.getClickedPos();
        Level level = context.getLevel();

        for (int c = 0; c < WIDTH; c++) {
            for (int r = 0; r < HEIGHT; r++) {
                if ((c == 0 && r == 0) || isOptional(c, r)) continue;
                BlockPos cellPos = origin.relative(sideDir, c).above(r);
                if (!level.getBlockState(cellPos).canBeReplaced(context)
                        || !level.getWorldBorder().isWithinBounds(cellPos)) {
                    return null;
                }
            }
        }
        return this.defaultBlockState().setValue(FACING, facing).setValue(COL, 0).setValue(ROW, 0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;

        Direction sideDir = state.getValue(FACING).getClockWise();
        for (int c = 0; c < WIDTH; c++) {
            for (int r = 0; r < HEIGHT; r++) {
                if ((c == 0 && r == 0) || isOptional(c, r)) continue;
                BlockPos cellPos = pos.relative(sideDir, c).above(r);
                level.setBlock(cellPos, state.setValue(COL, c).setValue(ROW, r), 3);
            }
        }
        level.blockUpdated(pos, Blocks.AIR);
        state.updateNeighbourShapes(level, pos, 3);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos origin = originPos(state, pos);
            Direction sideDir = state.getValue(FACING).getClockWise();
            for (int c = 0; c < WIDTH; c++) {
                for (int r = 0; r < HEIGHT; r++) {
                    BlockPos cellPos = origin.relative(sideDir, c).above(r);
                    if (cellPos.equals(pos)) continue;
                    BlockState cellState = level.getBlockState(cellPos);
                    if (cellState.is(state.getBlock())) {
                        level.setBlock(cellPos, Blocks.AIR.defaultBlockState(), 35);
                        level.levelEvent(player, 2001, cellPos, Block.getId(cellState));
                    }
                }
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.CONSUME;

        if (!isIntact(level, state, pos)) {
            player.displayClientMessage(Component.translatable("block.creraces.quest_board.incomplete"), true);
            return InteractionResult.CONSUME;
        }

        BlockPos masterPos = masterPos(state, pos);
        if (level.getBlockEntity(masterPos) instanceof QuestBoardBlockEntity be) {
            be.prepareOfferedIds(sp);
            dev.architectury.registry.menu.MenuRegistry.openExtendedMenu(sp, be);
            // Client-reconstructed menus start taken[]/locked[] all-false, so force a resync on open.
            mc.sayda.creraces.network.QuestBoardStateSyncPacket.resyncIfOpen(sp);
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (isMaster(state)) {
            return new QuestBoardBlockEntity(pos, state);
        }
        return null;
    }
}
