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

    // VoxelShape values below are empirically tuned to avoid entity collision
    // crashes; the exact bounds are approximate and may need adjustment.
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
    public static final VoxelShape BOX = Block.box(0.01, 0.01, 0.01, 0.02, 0.02, 0.02);

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
            VoxelShape shape = micro.getOrCreateShape(false, level);
            return shape.isEmpty() ? BOX : shape;
        }
        return BOX;

    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof MicroBlockEntity micro) {
            VoxelShape shape = micro.getOrCreateShape(true, level);
            return shape.isEmpty() ? BOX : shape;
        }
        return BOX;

    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        boolean isSmallBuild = mc.sayda.creraces.capability.DataUtils.getVariables(player)
                .map(mc.sayda.creraces.capability.IPlayerVariables::isSmallBuild)
                .orElse(false);

        if (isSmallBuild) {
            return 0.0f;
        } else if (!player.isShiftKeyDown()) {
            return 0.0f;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    // ─── Solidity & Occlusion ───────────────────────────────────────────────────

    @Override
    public boolean isCollisionShapeFullBlock(@javax.annotation.Nonnull BlockState state,
            @javax.annotation.Nonnull BlockGetter level, @javax.annotation.Nonnull BlockPos pos) {
        return false; // Grid is rarely a full block
    }

    @Override
    public boolean canBeReplaced(BlockState state, net.minecraft.world.level.material.Fluid fluid) {
        if (mc.sayda.creraces.config.CreRacesConfig.MINI_BLOCK_WATER_RESISTANT.get()) {
            return false;
        }
        return super.canBeReplaced(state, fluid);
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

        // --- PERMISSION CHECK ---
        if (!mc.sayda.creraces.capability.DataUtils.canInteractWithMiniBuild(player)) {
            // Consume bucket interactions so vanilla doesn't place liquid adjacent to the block
            net.minecraft.world.item.Item heldItem = player.getItemInHand(hand).getItem();
            if (heldItem == net.minecraft.world.item.Items.WATER_BUCKET
                    || heldItem == net.minecraft.world.item.Items.LAVA_BUCKET
                    || heldItem == net.minecraft.world.item.Items.BUCKET) {
                return net.minecraft.world.InteractionResult.CONSUME;
            }
            return net.minecraft.world.InteractionResult.PASS;
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
            ItemStack held = player.getItemInHand(hand);

            if (slotState.isAir()) {
                if (held.getItem() == net.minecraft.world.item.Items.WATER_BUCKET) {
                    micro.setSlot(slotX, slotY, slotZ,
                            net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
                    if (!player.getAbilities().instabuild)
                        player.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.BUCKET));
                    return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
                } else if (held.getItem() == net.minecraft.world.item.Items.LAVA_BUCKET) {
                    micro.setSlot(slotX, slotY, slotZ,
                            net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState());
                    if (!player.getAbilities().instabuild)
                        player.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.BUCKET));
                    return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
                }
                return net.minecraft.world.InteractionResult.PASS;
            }

            if (held.getItem() == net.minecraft.world.item.Items.BUCKET
                    && (slotState.getBlock() == net.minecraft.world.level.block.Blocks.WATER
                            || slotState.getBlock() == net.minecraft.world.level.block.Blocks.LAVA)) {
                net.minecraft.world.item.Item filledItem =
                        slotState.getBlock() == net.minecraft.world.level.block.Blocks.WATER
                                ? net.minecraft.world.item.Items.WATER_BUCKET
                                : net.minecraft.world.item.Items.LAVA_BUCKET;
                micro.setSlot(slotX, slotY, slotZ,
                        net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                    ItemStack filled = new ItemStack(filledItem);
                    if (!player.getInventory().add(filled))
                        player.drop(filled, false);
                }
                return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
            }

            return micro.handleSlotUse(player, hand, slotX, slotY, slotZ);
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
    public void playerDestroy(@javax.annotation.Nonnull Level level, @javax.annotation.Nonnull Player player,
            @javax.annotation.Nonnull BlockPos pos,
            @javax.annotation.Nonnull BlockState state, @Nullable BlockEntity blockEntity,
            @javax.annotation.Nonnull ItemStack tool) {
        if (!level.isClientSide && blockEntity instanceof MicroBlockEntity micro) {
            // Drop regular mini-blocks
            micro.forEachOccupied((x, y, z, slotState) -> {
                if (slotState.getBlock() == net.minecraft.world.level.block.Blocks.WATER) {
                    popResource(level, pos, new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
                    return;
                }
                if (slotState.getBlock() == net.minecraft.world.level.block.Blocks.LAVA) {
                    popResource(level, pos, new ItemStack(net.minecraft.world.item.Items.LAVA_BUCKET));
                    return;
                }
                net.minecraft.world.level.ItemLike itemLike = slotState.getBlock().asItem();
                if (itemLike != net.minecraft.world.item.Items.AIR) {
                    @SuppressWarnings("null")
                    @javax.annotation.Nonnull
                    ItemStack drop = new ItemStack(itemLike);
                    if (!drop.isEmpty()) {
                        popResource(level, pos, drop);
                    }
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
