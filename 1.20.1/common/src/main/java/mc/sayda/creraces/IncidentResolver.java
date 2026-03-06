package mc.sayda.creraces;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.network.BoundaryHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Resolves incidents (events) within the world.
 * Manages data synchronization and the flow of time (cooldowns).
 */
public class IncidentResolver {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final AtomicLong sakuyaWatchTick = new AtomicLong(0);

    public static void init() {
        // Player Events
        PlayerEvent.PLAYER_JOIN.register(IncidentResolver::onIncidentBegin); // Login
        PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wonGame) -> {
            onIncidentClone(oldPlayer, newPlayer, !wonGame);
        });
        PlayerEvent.CHANGE_DIMENSION.register((player, oldLevel, newLevel) -> {
            onIncidentTransition(player); // Dimension change
        });
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd) -> {
            onRespawn(player);
        });

        // Registering pickup event via Architectury standard
        PlayerEvent.PICKUP_ITEM_POST.register((player, itemEntity, stack) -> {
            if (player instanceof ServerPlayer sp) {
                onIncidentPickup(sp, stack);
            }
        });

        // Tick Events
        TickEvent.SERVER_POST.register(IncidentResolver::onGensokyoTick);

        // Disconnect: clear beam cache and save teams
        PlayerEvent.PLAYER_QUIT.register(player -> {
            mc.sayda.creraces.engine.actions.BeamAction.clearForPlayer(player);
        });

        // Team persistence
        dev.architectury.event.events.common.LifecycleEvent.SERVER_STARTED.register(server -> {
            mc.sayda.creraces.team.RaceTeamManager.load(server);
        });
        dev.architectury.event.events.common.LifecycleEvent.SERVER_STOPPING.register(server -> {
            mc.sayda.creraces.team.RaceTeamManager.save(server);
        });

        // Social Passives (defendedByEntities)
        mc.sayda.creraces.race.SocialPassivesEvent.register();

        // Entity Events
        dev.architectury.event.events.common.EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (source.getEntity() instanceof ServerPlayer killer) {
                onIncidentVictory(killer, entity);
            }
            return dev.architectury.event.EventResult.pass();
        });

        dev.architectury.event.events.common.InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> {
            if (player instanceof ServerPlayer sp) {
                return onIncidentInteraction(sp, hand);
            }
            return dev.architectury.event.CompoundEventResult.<net.minecraft.world.item.ItemStack>pass();
        });

        dev.architectury.event.events.common.InteractionEvent.RIGHT_CLICK_BLOCK
                .register((player, hand, pos, direction) -> {
                    if (player instanceof ServerPlayer sp) {
                        return onIncidentBlockInteraction(sp, hand, pos);
                    }
                    return dev.architectury.event.EventResult.pass();
                });

        dev.architectury.event.events.common.BlockEvent.PLACE.register((level, pos, state, placer) -> {
            if (placer instanceof ServerPlayer sp && !level.isClientSide()) {
                return onIncidentBlockPlace(sp, pos, state);
            }
            return dev.architectury.event.EventResult.pass();
        });

        dev.architectury.event.events.common.EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            if (source.getEntity() instanceof ServerPlayer player) {
                onIncidentAttack(player, entity);
            }
            return dev.architectury.event.EventResult.pass();
        });
    }

    private static dev.architectury.event.CompoundEventResult<net.minecraft.world.item.ItemStack> onIncidentInteraction(
            ServerPlayer player, net.minecraft.world.InteractionHand hand) {
        net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
        java.util.Optional<mc.sayda.creraces.capability.IPlayerVariables> varsOpt = DataUtils.getVariables(player);
        if (varsOpt.isPresent()) {
            mc.sayda.creraces.capability.IPlayerVariables vars = varsOpt.get();
            mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
            if (race != null && race.traits() != null) {
                for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                    if (trait.onInteraction(player, stack)) {
                        return dev.architectury.event.CompoundEventResult.interruptTrue(stack);
                    }
                }
            }
        }
        return dev.architectury.event.CompoundEventResult.pass();
    }

    private static dev.architectury.event.EventResult onIncidentBlockInteraction(ServerPlayer player,
            net.minecraft.world.InteractionHand hand, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.level.block.state.BlockState state = player.level().getBlockState(pos);
        java.util.Optional<mc.sayda.creraces.capability.IPlayerVariables> varsOpt = DataUtils.getVariables(player);
        if (varsOpt.isPresent()) {
            mc.sayda.creraces.capability.IPlayerVariables vars = varsOpt.get();
            mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
            if (race != null && race.traits() != null) {
                for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                    if (trait.onBlockInteraction(player, pos, state)) {
                        return dev.architectury.event.EventResult.interruptTrue();
                    }
                }
            }
        }
        return dev.architectury.event.EventResult.pass();
    }

    private static dev.architectury.event.EventResult onIncidentBlockPlace(ServerPlayer player,
            net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        java.util.Optional<mc.sayda.creraces.capability.IPlayerVariables> varsOpt = DataUtils.getVariables(player);
        if (varsOpt.isPresent()) {
            mc.sayda.creraces.capability.IPlayerVariables vars = varsOpt.get();
            mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
            if (race != null && race.traits() != null) {
                for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                    if (trait.onBlockPlace(player, pos, state)) {
                        return dev.architectury.event.EventResult.interruptTrue(); // Was always pass() — bug: trait
                                                                                   // result was ignored
                    }
                }
            }
        }
        return dev.architectury.event.EventResult.pass();
    }

    private static void onIncidentAttack(ServerPlayer player, net.minecraft.world.entity.LivingEntity victim) {
        if (mc.sayda.creraces.util.DamageGuard.isProcessing()) {
            return;
        }

        mc.sayda.creraces.util.DamageGuard.setProcessing(true);
        try {
            DataUtils.getVariables(player).ifPresent(vars -> {
                mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
                if (race != null && race.traits() != null) {
                    for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                        trait.onHit(player, victim);
                    }
                }
            });
        } finally {
            mc.sayda.creraces.util.DamageGuard.setProcessing(false);
        }
    }

    private static void onIncidentVictory(ServerPlayer killer, net.minecraft.world.entity.LivingEntity victim) {
        DataUtils.getVariables(killer).ifPresent(vars -> {
            // Trigger data-driven on_kill traits
            mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
            if (race != null && race.traits() != null) {
                for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                    trait.onKill(killer, victim);
                }
            }
        });

    }

    private static void onIncidentPickup(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
            if (race != null && race.traits() != null) {
                for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                    trait.onItemPickup(player, stack);
                }
            }
        });
    }

    private static void onIncidentBegin(ServerPlayer player) {
        // Attributes need setup before client sync
        mc.sayda.creraces.race.AttributeIncidents.eikiJudgment(player);

        // Sync the joining player's data to all currently online players,
        // and sync all online players' data to the joining player.
        // NOTE: We iterate level().players() but avoid an O(n²) full cross-sync:
        // each existing player only needs to receive the new player's data once.
        if (player.level() != null) {
            player.level().players().forEach(other -> {
                if (other != player) {
                    // Send this player's vars to the existing player
                    BoundaryHandler.resyncVariables(player, (ServerPlayer) other);
                    // Send the existing player's vars to this player
                    BoundaryHandler.resyncVariables(other, player);
                }
            });
        }
    }

    public static void onClientRequestedSync(ServerPlayer player) {
        LOGGER.info("IncidentResolver: Client {} explicitly requested data sync.", player.getScoreboardName());
        // Initial sync when a player joins and is ready
        BoundaryHandler.resyncVariables(player, player);

        // Sync race and ability definitions
        BoundaryHandler.syncRacesToPlayer(player, mc.sayda.creraces.race.RaceManager.createSyncPacket());
        BoundaryHandler.syncAbilitiesToPlayer(player, mc.sayda.creraces.ability.AbilityManager.createSyncPacket());
    }

    private static void onIncidentTransition(ServerPlayer player) {
        // Sync when moving between dimensions
        BoundaryHandler.resyncVariables(player, player);
    }

    private static void onIncidentClone(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean wasDeath) {
        LOGGER.info("IncidentResolver: onIncidentClone triggered! wasDeath: {}", wasDeath);
        DataUtils.getVariables(oldPlayer).ifPresent(oldVars -> {
            LOGGER.debug("IncidentResolver: Found oldVars. Race: {}", oldVars.getRace());
            DataUtils.getVariables(newPlayer).ifPresent(newVars -> {
                newVars.deserialize(oldVars.serialize());
                LOGGER.info("IncidentResolver: Copied oldVars to newVars. New Race: {}", newVars.getRace());

                if (wasDeath) {
                    // resetOnDeath clears resources, cooldowns, channeled state, AND all
                    // non-persistent ability states (not just equipped ones — fixes the
                    // previous incomplete manual loop over slots).
                    newVars.resetOnDeath();

                    // Re-apply Scale and Cosmetics after death/respawn
                    mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(newVars.getRace());
                    if (race != null) {
                        mc.sayda.creraces.race.RaceIncidents.applyScale(newPlayer, race.scale());
                        mc.sayda.creraces.race.CosmeticIncidents.applyCustomizations(newPlayer,
                                newVars.getCustomizations(), race);
                    }
                }

                // After cloning, sync to the new player
                BoundaryHandler.resyncVariables(newPlayer, newPlayer);
            });
        });
    }

    private static void onGensokyoTick(net.minecraft.server.MinecraftServer server) {
        if (mc.sayda.creraces.config.CreRacesConfig.FORCED_SELECTION.get()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                DataUtils.getVariables(player).ifPresent(vars -> {
                    if (!vars.hasChosenRace()) {
                        net.minecraft.world.effect.MobEffect res = net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE;
                        if (res != null) {
                            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                    res, 40, 255, false, false, false));
                        }
                    }
                });
            }
        }

        sakuyaWatchTick.incrementAndGet();

        // Ticking down resources/cooldowns
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            mc.sayda.creraces.race.ResourceTicker.tick(player);
        }
        mc.sayda.creraces.util.Scheduler.tick();
    }

    public static void onTrackingBegin(ServerPlayer tracker, net.minecraft.world.entity.Entity target) {
        if (target instanceof Player targetPlayer) {
            BoundaryHandler.resyncVariables(targetPlayer, tracker);
        }
    }

    public static void onRespawn(ServerPlayer player) {
        LOGGER.info("IncidentResolver: onRespawn triggered for {}", player.getName().getString());

        // Re-apply race elements that Pehkui/Vanilla might have cleared
        DataUtils.getVariables(player).ifPresent(vars -> {
            mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
            if (race != null) {
                mc.sayda.creraces.race.RaceIncidents.applyScale(player, race.scale());
            }
        });

        BoundaryHandler.resyncVariables(player, player);
        mc.sayda.creraces.race.AttributeIncidents.eikiJudgment(player);

        // Set default respawn states for resources
        DataUtils.getVariables(player).ifPresent(vars -> {
            vars.setMana(player.getAttributeValue(
                    java.util.Objects.requireNonNull(mc.sayda.creraces.registry.ModAttributes.MAX_MANA.get())));
            vars.setEnergy(player.getAttributeValue(
                    java.util.Objects.requireNonNull(mc.sayda.creraces.registry.ModAttributes.MAX_ENERGY.get())));
            vars.setRage(0);
            vars.setGrit(0);
        });

        // Trigger on_respawn traits
        DataUtils.getVariables(player).ifPresent(vars -> {
            mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
            if (race != null && race.traits() != null) {
                for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                    trait.onRespawn(player);
                }
            }
        });
    }
}
