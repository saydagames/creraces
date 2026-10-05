package mc.sayda.creraces.block;

import mc.sayda.creraces.block.entity.ToriiBellBlockEntity;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.WorldState;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.GateBuiltPacket;
import mc.sayda.creraces.registry.ModBlocks;
import mc.sayda.creraces.util.RaceUtils;
import mc.sayda.creraces.util.SpiritRealmUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class ToriiBellBlock extends BellBlock {
    /** Players within this radius are carried along on every ring, whether they meant to follow or not. */
    private static final double CARRY_RADIUS = 3.0;

    /**
     * Weathered log positions that make up a torii frame around the bell, as {along the gate, up}.
     * The gate may run along either horizontal axis.
     */
    private static final int[][] GATE_LOG_OFFSETS = {
            {3, -2}, {-3, -2},
            {3, 0}, {-3, 0},
            {3, 2}, {-3, 2},
            {3, 4}, {-3, 4},
            {5, 4}, {-5, 4},
            {0, 3},
    };

    private final boolean isWeathered;

    public ToriiBellBlock(Properties properties, boolean isWeathered) {
        super(properties);
        this.isWeathered = isWeathered;
    }

    @Override
    public BellBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ToriiBellBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlocks.TORII_BELL_ENTITY.get(),
                level.isClientSide() ? BellBlockEntity::clientTick : BellBlockEntity::serverTick);
    }

    @Override
    @SuppressWarnings("null")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        // A click on the frame doesn't ring the bell, so it doesn't work the gate either
        if (!isBellHit(state, hit)) {
            return InteractionResult.PASS;
        }
        boolean shouldRing = true;
        if (!level.isClientSide() && hand == InteractionHand.MAIN_HAND) {
            shouldRing = handleInteraction(level, pos, player);
        }
        if (!shouldRing) {
            return InteractionResult.CONSUME;
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    /** Vanilla's private BellBlock.isProperHit: true when the click landed on the bell rather than its frame. */
    @SuppressWarnings("null")
    private static boolean isBellHit(BlockState state, BlockHitResult hit) {
        Direction side = hit.getDirection();
        double y = hit.getLocation().y - hit.getBlockPos().getY();
        if (side.getAxis() == Direction.Axis.Y || y > 0.8124F) {
            return false;
        }
        Direction facing = state.getValue(FACING);
        return switch (state.getValue(ATTACHMENT)) {
            case FLOOR -> facing.getAxis() == side.getAxis();
            case SINGLE_WALL, DOUBLE_WALL -> facing.getAxis() != side.getAxis();
            case CEILING -> true;
        };
    }

    /** Runs the bell's effect for this player and returns whether the bell should still ring. */
    private boolean handleInteraction(Level level, BlockPos pos, Player player) {
        IPlayerVariables vars = DataUtils.getVariables(player).orElse(null);
        if (vars == null) {
            return true;
        }
        if (isWeathered) {
            if (!RaceUtils.isKitsune(player) || level.dimension() != Level.OVERWORLD) {
                player.displayClientMessage(Component.translatable("block.creraces.torii_bell.weathered_silent"), true);
                return false;
            }
            if (!checkAndPlaceStructure((ServerLevel) level, pos, player)) {
                player.displayClientMessage(Component.translatable("block.creraces.torii_bell.pattern_missing"), true);
            }
            return true;
        }

        if (!RaceUtils.isSpirit(vars) && !WorldState.isSpiritMoon(level)) {
            player.displayClientMessage(Component.translatable("block.creraces.torii_bell.silent"), true);
            return false;
        }
        toggleSpiritRealm(player, vars);
        return true;
    }

    /** Completes a weathered torii frame around the bell into the full gate structure, if the frame is there. */
    @SuppressWarnings("null")
    private boolean checkAndPlaceStructure(ServerLevel level, BlockPos pos, Player player) {
        boolean foundNS = hasGateFrame(level, pos, true);
        boolean foundEW = !foundNS && hasGateFrame(level, pos, false);
        if (!foundNS && !foundEW) {
            return false;
        }

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return level.getStructureManager()
                .get(new ResourceLocation("creraces", "torii_gate"))
                .map(template -> {
                    BlockPos placePos = foundNS ? new BlockPos(x - 6, y - 3, z - 1) : new BlockPos(x + 1, y - 3, z - 6);
                    Rotation rotation = foundNS ? Rotation.NONE : Rotation.CLOCKWISE_90;

                    // Place the structure first so its air blocks don't erase the bell
                    template.placeInWorld(level, placePos, placePos,
                            new StructurePlaceSettings().setRotation(rotation).setMirror(Mirror.NONE)
                                    .setIgnoreEntities(false),
                            level.random, Block.UPDATE_ALL);

                    // Then swap the weathered bell for a working one
                    BlockState currentState = level.getBlockState(pos);
                    level.setBlock(pos, ModBlocks.TORII_BELL.get().defaultBlockState()
                            .setValue(FACING, currentState.getValue(FACING))
                            .setValue(ATTACHMENT, currentState.getValue(ATTACHMENT)), Block.UPDATE_ALL);

                    // The builder is always a kitsune (gated above); record the gate for them
                    // immediately instead of waiting for them to walk through it.
                    if (player instanceof ServerPlayer builder) {
                        BoundaryHandler.sendGateBuilt(builder,
                                new GateBuiltPacket(level.dimension().location().toString(), pos));
                    }
                    return true;
                })
                .orElse(false);
    }

    /** True if the weathered logs of a torii frame surround the bell, with the gate running along X or Z. */
    private static boolean hasGateFrame(ServerLevel level, BlockPos bell, boolean alongX) {
        Block log = ModBlocks.WEATHERED_RED_STRIPPED_OAK_LOG.get();
        for (int[] offset : GATE_LOG_OFFSETS) {
            BlockPos logPos = alongX ? bell.offset(offset[0], offset[1], 0) : bell.offset(0, offset[1], offset[0]);
            if (!level.getBlockState(logPos).is(log)) {
                return false;
            }
        }
        return true;
    }

    private void toggleSpiritRealm(Player player, IPlayerVariables vars) {
        boolean next = !vars.isInSpiritRealm();
        SpiritRealmUtils.setInSpiritRealm(player, next);
        SpiritRealmUtils.applyToNearby(player, CARRY_RADIUS, next);
    }
}
