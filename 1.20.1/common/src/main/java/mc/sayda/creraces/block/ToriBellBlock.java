package mc.sayda.creraces.block;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

public class ToriBellBlock extends BellBlock {
    private final boolean isWeathered;

    public ToriBellBlock(Properties properties, boolean isWeathered) {
        super(properties);
        this.isWeathered = isWeathered;
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        // First, handle the spirit realm logic
        if (!level.isClientSide() && hand == InteractionHand.MAIN_HAND) {
            handleInteraction(level, pos, player);
        }

        // Then, allow the bell to "ring" (vanilla behavior)
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        // Optional: Do we want projectiles to trigger spirit realm? Probably not, just
        // ring.
        super.onProjectileHit(level, state, hit, projectile);
    }

    private void handleInteraction(Level level, BlockPos pos, Player player) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            Race playerRace = RaceRegistry.get(vars.getRace());
            boolean isKitsune = playerRace != null && vars.getRace().getPath().contains("kitsune");

            if (isWeathered) {
                if (isKitsune && level.dimension() == Level.OVERWORLD) {
                    if (checkAndPlaceStructure((ServerLevel) level, pos, player, vars)) {
                        level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0f, 1.0f);
                    } else {
                        player.displayClientMessage(Component.translatable("block.creraces.tori_bell.pattern_missing"),
                                true);
                    }
                } else {
                    player.displayClientMessage(Component.translatable("block.creraces.tori_bell.weathered_silent"),
                            true);
                }
            } else {
                boolean isSpirit = playerRace != null && playerRace.isSpirit();
                if (isSpirit) {
                    toggleSpiritRealm(player, level, pos, vars, true);
                } else {
                    List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(5));
                    boolean guided = false;
                    for (Player nearby : nearbyPlayers) {
                        if (nearby == player)
                            continue;
                        var nearbyVars = DataUtils.getVariables(nearby).orElse(null);
                        Race nearbyRace = nearbyVars != null ? RaceRegistry.get(nearbyVars.getRace()) : null;
                        if (nearbyRace != null && nearbyRace.isSpirit()) {
                            guided = true;
                            break;
                        }
                    }

                    if (guided) {
                        toggleSpiritRealm(player, level, pos, vars, false);
                    } else {
                        player.displayClientMessage(Component.translatable("block.creraces.torii_gate.silent"), true);
                    }
                }
            }
        });
    }

    private boolean checkAndPlaceStructure(ServerLevel level, BlockPos pos, Player player, IPlayerVariables vars) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        boolean foundNS = checkPattern(level, x, y, z, true);
        boolean foundEW = checkPattern(level, x, y, z, false);

        if (foundNS || foundEW) {
            StructureTemplate template = level.getStructureManager()
                    .getOrCreate(new ResourceLocation("creraces", "kitsune_gate_overworld"));
            if (template != null) {
                BlockPos placePos = foundNS ? new BlockPos(x - 4, y - 3, z - 1) : new BlockPos(x + 1, y - 3, z - 6);
                Rotation rotation = foundNS ? Rotation.NONE : Rotation.CLOCKWISE_90;

                // 1. Place the structure first
                template.placeInWorld(level, placePos, placePos,
                        new StructurePlaceSettings().setRotation(rotation).setMirror(Mirror.NONE)
                                .setIgnoreEntities(false),
                        level.random, 3);

                // 2. Replace the weathered bell with a normal one AFTER the structure is
                // placed.
                // This prevents the structure's air blocks from overwriting the new bell.
                BlockState currentState = level.getBlockState(pos);
                level.setBlock(pos, ModBlocks.TORI_BELL.get().defaultBlockState()
                        .setValue(FACING, currentState.getValue(FACING))
                        .setValue(ATTACHMENT, currentState.getValue(ATTACHMENT)), 3);

                vars.setPocketX(x);
                vars.setPocketY(y);
                vars.setPocketZ(z);

                return true;
            }
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

    private void toggleSpiritRealm(Player player, Level level, BlockPos pos, IPlayerVariables vars,
            boolean isGuidingSpirit) {
        boolean newState = !vars.isInSpiritRealm();
        vars.setInSpiritRealm(newState);
        BoundaryHandler.resyncForAllTrackers(player);
        BoundaryHandler.resyncVariables(player, player);

        level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0f, 1.0f);

        if (isGuidingSpirit) {
            List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(5));
            for (Player nearby : nearbyPlayers) {
                if (nearby == player)
                    continue;
                DataUtils.getVariables(nearby).ifPresent(nearbyVars -> {
                    Race nearbyRace = RaceRegistry.get(nearbyVars.getRace());
                    if (nearbyRace != null && !nearbyRace.isSpirit()) {
                        nearbyVars.setInSpiritRealm(newState);
                        BoundaryHandler.resyncForAllTrackers(nearby);
                        BoundaryHandler.resyncVariables(nearby, nearby);
                    }
                });
            }
        }
    }
}
