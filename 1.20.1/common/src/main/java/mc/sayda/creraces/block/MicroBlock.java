package mc.sayda.creraces.block;

import mc.sayda.creraces.block.entity.MicroBlockEntity;
import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import javax.annotation.Nullable;

/**
 * The host block that contains a 4x4x4 grid of mini-blocks.
 * Invisible to normal rendering; the MicroBlockEntityRenderer draws the
 * contents.
 */
public class MicroBlock extends BaseEntityBlock {

    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);

    public static final BlockBehaviour.Properties PROPERTIES = BlockBehaviour.Properties.of()
            .noOcclusion()
            .strength(0.5f) // approx 1.5s, no preferred tool
            .lightLevel(state -> state.getValue(LIGHT))
            .dynamicShape();

    public MicroBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LIGHT, 0));
    }

    // ─── BlockEntity ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MicroBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            net.minecraft.world.level.Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return createTickerHelper(type, ModBlocks.MICRO_BLOCK_ENTITY.get(), MicroBlockEntity::serverTick);
        }
        return null;
    }

    // ─── Rendering ───────────────────────────────────────────────────────────────

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED; // delegates to our BER
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof MicroBlockEntity micro) {
            return micro.getOrCreateShape(false, level);
        }
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof MicroBlockEntity micro) {
            return micro.getOrCreateShape(true, level);
        }
        return Shapes.empty();
    }

    // ─── Solidity & Occlusion ───────────────────────────────────────────────────

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false; // Grid is rarely a full block
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true; // Allow rain visuals and skylight to pass through visually
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MicroBlockEntity micro) {
                micro.dropAllInventories();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    @SuppressWarnings("null")
    public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof MicroBlockEntity micro) {
            // Debounce: prevent natural interaction + packet interaction from
            // double-cycling in the same tick
            if (level.getGameTime() == micro.getLastUseTime()) {
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            micro.setLastUseTime(level.getGameTime());

            // Calculate which sub-slot was hit using the same logic as the client Mixin
            net.minecraft.world.phys.Vec3 normal = net.minecraft.world.phys.Vec3
                    .atLowerCornerOf(hitResult.getDirection().getNormal());
            net.minecraft.world.phys.Vec3 hitCenter = hitResult.getLocation().subtract(normal.scale(0.005));

            int slotX = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(hitCenter.x);
            int slotY = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(hitCenter.y);
            int slotZ = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(hitCenter.z);

            BlockState slotState = micro.getSlot(slotX, slotY, slotZ);
            if (slotState.isAir()) {
                return net.minecraft.world.InteractionResult.PASS;
            }

            if (micro != null) {
                return micro.handleSlotUse(player, hand, slotX, slotY, slotZ);
            }
        }

        return net.minecraft.world.InteractionResult.PASS;
    }

    // ─── Block Breaking ────────────────────────────────────────────────

    /**
     * When the host block is broken in normal mode: drop all contained
     * mini-block items.
     * playerDestroy is the correct 1.20.1 hook (called AFTER the block is
     * removed but before the BlockEntity is discarded via onRemove).
     */

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos,
            BlockState state, @Nullable BlockEntity blockEntity,
            ItemStack tool) {
        if (!level.isClientSide && blockEntity instanceof MicroBlockEntity micro) {
            // Drop regular mini-blocks
            micro.forEachOccupied((x, y, z, slotState) -> {
                ItemStack drop = new ItemStack(slotState.getBlock().asItem());
                if (!drop.isEmpty()) {
                    popResource(level, pos, drop);
                }
            });
            // Inventories are handled by onRemove
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos,
            boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof MicroBlockEntity micro) {
                micro.updateExternalNeighbors();
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIGHT);
    }

}
