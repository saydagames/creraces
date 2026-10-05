package mc.sayda.creraces.block;

import com.mojang.serialization.MapCodec;
import mc.sayda.creraces.block.entity.MicroBlockEntity;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The host block that contains a 4x4x4 grid of mini-blocks.
 * Invisible to normal rendering; MiniBlockEntityRenderer draws the contents.
 */
public class MicroBlock extends BaseEntityBlock {

    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);

    // Tiny stand-in shape while the grid is empty. The bounds were found by trial to avoid
    // entity collision crashes and may need adjusting.
    public static final VoxelShape BOX = Block.box(0.01, 0.01, 0.01, 0.02, 0.02, 0.02);

    public static final BlockBehaviour.Properties PROPERTIES = BlockBehaviour.Properties.of()
            .noOcclusion()
            .strength(0.5f) // no preferred tool
            .lightLevel(state -> state.getValue(LIGHT))
            .dynamicShape();

    public static final MapCodec<MicroBlock> CODEC = simpleCodec(MicroBlock::new);

    public MicroBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LIGHT, 0));
    }

    @Override
    protected MapCodec<? extends MicroBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MicroBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return createTickerHelper(type, ModBlocks.MICRO_BLOCK_ENTITY.get(), MicroBlockEntity::serverTick);
        }
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED; // delegates to our BER
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return gridShape(level, pos, false);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return gridShape(level, pos, true);
    }

    private static VoxelShape gridShape(BlockGetter level, BlockPos pos, boolean collision) {
        if (level.getBlockEntity(pos) instanceof MicroBlockEntity micro) {
            VoxelShape shape = micro.getOrCreateShape(collision, level);
            return shape.isEmpty() ? BOX : shape;
        }
        return BOX;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        boolean isSmallBuild = DataUtils.getVariables(player)
                .map(IPlayerVariables::isSmallBuild)
                .orElse(false);
        if (isSmallBuild || !player.isShiftKeyDown()) {
            return 0.0f;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public boolean isCollisionShapeFullBlock(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos) {
        return false; // Grid is rarely a full block
    }

    @Override
    public boolean canBeReplaced(BlockState state, Fluid fluid) {
        if (CreRacesConfig.MINI_BLOCK_WATER_RESISTANT.get()) {
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
            if (level.getBlockEntity(pos) instanceof MicroBlockEntity micro) {
                micro.dropAllInventories();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    @SuppressWarnings("null")
    public ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        if (!DataUtils.canInteractWithMiniBuild(player)) {
            // Consume bucket clicks so vanilla doesn't place or pick up liquid next to the block
            boolean isBucket = held.is(Items.WATER_BUCKET) || held.is(Items.LAVA_BUCKET) || held.is(Items.BUCKET);
            return isBucket ? ItemInteractionResult.CONSUME : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!(level.getBlockEntity(pos) instanceof MicroBlockEntity micro)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // Debounce: the natural use and MiniUsePacket can both land in the same tick
        if (level.getGameTime() == micro.getLastUseTime()) {
            return ItemInteractionResult.SUCCESS;
        }
        micro.setLastUseTime(level.getGameTime());

        // Same sub-slot resolution as MiniBlockPlaceMixin: nudge the hit point just inside the clicked face
        Vec3 normal = Vec3.atLowerCornerOf(hitResult.getDirection().getNormal());
        Vec3 hitCenter = hitResult.getLocation().subtract(normal.scale(0.005));
        int slotX = MicroBlockEntity.clampSlot(hitCenter.x);
        int slotY = MicroBlockEntity.clampSlot(hitCenter.y);
        int slotZ = MicroBlockEntity.clampSlot(hitCenter.z);

        return toItemInteractionResult(micro.handleSlotUse(player, hand, slotX, slotY, slotZ));
    }

    /** handleSlotUse is shared with MiniUsePacket and still returns the plain InteractionResult. */
    private static ItemInteractionResult toItemInteractionResult(InteractionResult result) {
        return switch (result) {
            case PASS -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            case FAIL -> ItemInteractionResult.FAIL;
            case CONSUME, CONSUME_PARTIAL -> ItemInteractionResult.CONSUME;
            default -> ItemInteractionResult.SUCCESS;
        };
    }

    /**
     * Drops every sub-block as an item. Vanilla calls this after removing the block, so onRemove has
     * already dropped the container contents and {@code blockEntity} is the instance captured beforehand.
     */
    @Override
    public void playerDestroy(@Nonnull Level level, @Nonnull Player player, @Nonnull BlockPos pos,
            @Nonnull BlockState state, @Nullable BlockEntity blockEntity, @Nonnull ItemStack tool) {
        if (!level.isClientSide && blockEntity instanceof MicroBlockEntity micro) {
            micro.forEachOccupied((x, y, z, slotState) -> {
                Item bucket = MicroBlockEntity.filledBucketFor(slotState);
                ItemStack drop = new ItemStack(bucket != null ? bucket : slotState.getBlock().asItem());
                if (!drop.isEmpty()) {
                    popResource(level, pos, drop);
                }
            });
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos,
            boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MicroBlockEntity micro) {
            micro.updateExternalNeighbors();
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIGHT);
    }
}
