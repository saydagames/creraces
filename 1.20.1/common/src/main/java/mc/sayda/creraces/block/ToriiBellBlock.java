package mc.sayda.creraces.block;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.BlockHitResult;


public class ToriiBellBlock extends BellBlock {
    private final boolean isWeathered;

    public ToriiBellBlock(Properties properties, boolean isWeathered) {
        super(properties);
        this.isWeathered = isWeathered;
    }

    @Override
    public BellBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new mc.sayda.creraces.block.entity.ToriiBellBlockEntity(pos, state);
    }

    @Override
    @javax.annotation.Nullable
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlocks.TORII_BELL_ENTITY.get(),
                level.isClientSide() ? BellBlockEntity::clientTick : BellBlockEntity::serverTick);
    }

    @Override
    @SuppressWarnings("null")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        boolean shouldRing = true;
        if (!level.isClientSide() && hand == InteractionHand.MAIN_HAND) {
            shouldRing = handleInteraction(level, pos, player);
        }
        if (!shouldRing) {
            return InteractionResult.CONSUME;
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    @SuppressWarnings("null")
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        // Optional: Do we want projectiles to trigger spirit realm? Probably not, just
        // ring.
        super.onProjectileHit(level, state, hit, projectile);
    }

    private boolean handleInteraction(Level level, BlockPos pos, Player player) {
        boolean[] shouldRing = { true };
        DataUtils.getVariables(player).ifPresent(vars -> {
            Race playerRace = RaceRegistry.get(vars.getRace());
            boolean isKitsune = playerRace != null && vars.getRace().getPath().contains("kitsune");

            if (isWeathered) {
                if (isKitsune && level.dimension() == Level.OVERWORLD) {
                    if (!checkAndPlaceStructure((ServerLevel) level, pos, player, vars)) {
                        player.displayClientMessage(Component.translatable("block.creraces.torii_bell.pattern_missing"),
                                true);
                    }
                } else {
                    player.displayClientMessage(Component.translatable("block.creraces.torii_bell.weathered_silent"),
                            true);
                    shouldRing[0] = false;
                }
            } else {
                boolean isSpirit = playerRace != null && playerRace.isSpirit();
                boolean isSpiritMoon = mc.sayda.creraces.engine.WorldState.isSpiritMoon(level);
                if (isSpirit || isSpiritMoon) {
                    toggleSpiritRealm(player, level, pos, vars);
                } else {
                    player.displayClientMessage(Component.translatable("block.creraces.torii_bell.silent"), true);
                    shouldRing[0] = false;
                }
            }
        });
        return shouldRing[0];
    }

    @SuppressWarnings("null")
    private boolean checkAndPlaceStructure(ServerLevel level, BlockPos pos, Player player, IPlayerVariables vars) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        boolean foundNS = checkPattern(level, x, y, z, true);
        boolean foundEW = checkPattern(level, x, y, z, false);

        if (foundNS || foundEW) {
            return level.getStructureManager()
                    .get(new ResourceLocation("creraces", "torii_gate"))
                    .map(template -> {
                        BlockPos placePos = foundNS ? new BlockPos(x - 6, y - 3, z - 1) : new BlockPos(x + 1, y - 3, z - 6);
                        Rotation rotation = foundNS ? Rotation.NONE : Rotation.CLOCKWISE_90;

                        // Place the structure first so its air blocks don't erase the bell
                        template.placeInWorld(level, placePos, placePos,
                                new StructurePlaceSettings().setRotation(rotation).setMirror(Mirror.NONE)
                                        .setIgnoreEntities(false),
                                level.random, 3);

                        // Replace the weathered bell with a normal one after placement
                        BlockState currentState = level.getBlockState(pos);
                        level.setBlock(pos, ModBlocks.TORII_BELL.get().defaultBlockState()
                                .setValue(FACING, currentState.getValue(FACING))
                                .setValue(ATTACHMENT, currentState.getValue(ATTACHMENT)), 3);

                        return true;
                    })
                    .orElse(false);
        }
        return false;
    }

    private boolean checkPattern(ServerLevel level, int x, int y, int z, boolean ns) {
        Block log = ModBlocks.WEATHERED_RED_STRIPPED_OAK_LOG.get();
        if (ns) {
            return isLog(level, x + 3, y - 2, z, log) && isLog(level, x - 3, y - 2, z, log) &&
                    isLog(level, x + 3, y, z, log) && isLog(level, x - 3, y, z, log) &&
                    isLog(level, x + 3, y + 2, z, log) && isLog(level, x - 3, y + 2, z, log) &&
                    isLog(level, x + 3, y + 4, z, log) && isLog(level, x - 3, y + 4, z, log) &&
                    isLog(level, x + 5, y + 4, z, log) && isLog(level, x - 5, y + 4, z, log) &&
                    isLog(level, x, y + 3, z, log);
        } else {
            return isLog(level, x, y - 2, z + 3, log) && isLog(level, x, y - 2, z - 3, log) &&
                    isLog(level, x, y, z + 3, log) && isLog(level, x, y, z - 3, log) &&
                    isLog(level, x, y + 2, z + 3, log) && isLog(level, x, y + 2, z - 3, log) &&
                    isLog(level, x, y + 4, z + 3, log) && isLog(level, x, y + 4, z - 3, log) &&
                    isLog(level, x, y + 4, z + 5, log) && isLog(level, x, y + 4, z - 5, log) &&
                    isLog(level, x, y + 3, z, log);
        }
    }

    private boolean isLog(ServerLevel level, int x, int y, int z, Block expected) {
        return level.getBlockState(new BlockPos(x, y, z)).is(expected);
    }

    private void toggleSpiritRealm(Player player, Level level, BlockPos pos, IPlayerVariables vars) {
        vars.setInSpiritRealm(!vars.isInSpiritRealm());
        BoundaryHandler.resyncForAllTrackers(player);
        BoundaryHandler.resyncVariables(player, player);
    }
}
