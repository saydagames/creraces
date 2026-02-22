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
        // We no longer sync data immediately upon login because the client is often
        // not ready to receive custom payloads, causing the packet to drop silently.
        // The client will explicitly send a RequestSyncPacket when it enters the world.
        mc.sayda.creraces.race.AttributeIncidents.eikiJudgment(player);

        // Also sync everyone else to this player
        if (player.level() != null) {
            player.level().players().forEach(other -> {
                BoundaryHandler.resyncVariables(other, player);
                if (other instanceof ServerPlayer sp) {
                    BoundaryHandler.resyncVariables(player, sp);
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
                    // Reset non-persistent states on death
                    for (mc.sayda.creraces.ability.AbilitySlot slot : mc.sayda.creraces.ability.AbilitySlot.values()) {
                        ResourceLocation abilityId = newVars.getAbilityInSlot(slot);
                        if (abilityId != null) {
                            mc.sayda.creraces.ability.Ability ability = mc.sayda.creraces.ability.AbilityRegistry
                                    .get(abilityId);
                            if (ability != null && !ability.persistent()) {
                                LOGGER.debug("IncidentResolver: Resetting non-persistent state for {}", abilityId);
                                newVars.setAbilityState(abilityId, 0);
                            }
                        }
                    }

                    // Restore Tweilight Lib addons
                    mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(newVars.getRace());
                    if (race != null) {
                        mc.sayda.creraces.race.CosmeticIncidents.applyCustomizations(newPlayer,
                                newVars.getCustomizations(),
                                race);
                    }
                }

                // After cloning, sync to the new player
                BoundaryHandler.resyncVariables(newPlayer, newPlayer);
            });
        });
    }

    private static void onGensokyoTick(net.minecraft.server.MinecraftServer server) {
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
        BoundaryHandler.resyncVariables(player, player);
        mc.sayda.creraces.race.AttributeIncidents.eikiJudgment(player);

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
