package mc.sayda.creraces;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.network.BoundaryHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Resolves incidents (events) within the world.
 * Manages data synchronization and the flow of time (cooldowns).
 */
public class IncidentResolver {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

public static void init() {
        // Player Events
        PlayerEvent.PLAYER_JOIN.register(IncidentResolver::onIncidentBegin); // Login
        PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wonGame) -> {
            onIncidentClone(oldPlayer, newPlayer, !wonGame);
        });
        PlayerEvent.CHANGE_DIMENSION.register((player, oldLevel, newLevel) -> {
            onIncidentTransition(player, oldLevel, newLevel);
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
        TickEvent.SERVER_POST.register(IncidentResolver::onIncidentTick);

        // Elect faction leader on join
        PlayerEvent.PLAYER_JOIN.register(player -> {
            mc.sayda.creraces.territory.FactionLeaderManager.onPlayerJoin((ServerPlayer) player);
        });

        // Disconnect: clear transient engine states and reassign faction leadership
        PlayerEvent.PLAYER_QUIT.register(player -> {
            mc.sayda.creraces.engine.ActionRegistry.cleanup(player);
            mc.sayda.creraces.quest.QuestSessionRegistry.clearPlayer(player.getUUID());
            mc.sayda.creraces.territory.TerritoryManager.get().clearPlayerTracking(player.getUUID());
            var server = ((ServerPlayer) player).getServer();
            if (server != null) {
                mc.sayda.creraces.territory.FactionLeaderManager.onPlayerLeave((ServerPlayer) player, server);
            }
        });

        // Load persisted server state (teams, pockets, territory)
        dev.architectury.event.events.common.LifecycleEvent.SERVER_STARTED.register(server -> {
            mc.sayda.creraces.team.RaceTeamManager.load(server);
            mc.sayda.creraces.util.PocketManager.load(server);
            mc.sayda.creraces.territory.TerritoryManager.load(server);
        });
        // Save persisted server state (teams, pockets, territory), then clean up transient state
        dev.architectury.event.events.common.LifecycleEvent.SERVER_STOPPING.register(server -> {
            mc.sayda.creraces.team.RaceTeamManager.save(server);
            mc.sayda.creraces.util.PocketManager.save(server);
            mc.sayda.creraces.territory.TerritoryManager.save(server);
            mc.sayda.creraces.util.Scheduler.clear();
            mc.sayda.creraces.territory.FactionLeaderManager.clear();
            mc.sayda.creraces.util.PocketManager.onServerStop();
            mc.sayda.creraces.worldgen.ModWorldgen.onServerStop();
        });

        // Social Passives (defendedByEntities)
        mc.sayda.creraces.race.SocialPassivesEvent.register();

        // Entity Events

        // Prevent hostile mobs from spawning in the fairy realm
        dev.architectury.event.events.common.EntityEvent.ADD.register((entity, level) -> {
            if (!level.isClientSide()
                    && entity instanceof net.minecraft.world.entity.monster.Monster
                    && level instanceof net.minecraft.server.level.ServerLevel sl
                    && sl.dimension().location().equals(
                            new net.minecraft.resources.ResourceLocation(CreRaces.MODID, "fairy_realm"))) {
                entity.discard();
                return dev.architectury.event.EventResult.interruptFalse();
            }
            return dev.architectury.event.EventResult.pass();
        });

        dev.architectury.event.events.common.EntityEvent.LIVING_DEATH.register((entity, source) -> {
            net.minecraft.world.entity.player.Player killer = mc.sayda.creraces.util.CombatUtils
                    .getRootOwner(source.getEntity());
            if (killer instanceof ServerPlayer sp) {
                onIncidentVictory(sp, entity);
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

        dev.architectury.event.events.common.BlockEvent.BREAK.register((level, pos, state, player, xp) -> {
            if (!level.isClientSide() && player != null) {
                mc.sayda.creraces.quest.QuestTracker.onBlockBroken(player, state);
            }
            return dev.architectury.event.EventResult.pass();
        });

        // Guild Receptionist trade offers are injected here rather than in Villager's own
        // trade list registration, since vanilla only ever activates 2 trades per level -
        // syncing on interaction guarantees all 5 tiers are present the moment a player opens
        // the trade GUI, regardless of the villager's current level.
        dev.architectury.event.events.common.InteractionEvent.INTERACT_ENTITY.register((player, entity, hand) -> {
            if (player instanceof ServerPlayer && entity instanceof net.minecraft.world.entity.npc.Villager villager) {
                mc.sayda.creraces.villager.GuildReceptionistOfferSync.sync(villager);
            }
            return dev.architectury.event.EventResult.pass();
        });
    }

    private static boolean isPlayerLocked(ServerPlayer player) {
        var stunned = mc.sayda.creraces.registry.ModMobEffects.STUNNED.get();
        if (stunned != null && player.hasEffect(stunned)) {
            return true;
        }
        if (mc.sayda.creraces.config.CreRacesConfig.FORCED_SELECTION.get()) {
            return !DataUtils.getVariables(player).map(mc.sayda.creraces.capability.IPlayerVariables::hasChosenRace)
                    .orElse(true);
        }
        return false;
    }

    private static dev.architectury.event.CompoundEventResult<net.minecraft.world.item.ItemStack> onIncidentInteraction(
            ServerPlayer player, net.minecraft.world.InteractionHand hand) {
        if (isPlayerLocked(player)) {
            return dev.architectury.event.CompoundEventResult.interruptTrue(player.getItemInHand(hand));
        }

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
        if (isPlayerLocked(player)) {
            return dev.architectury.event.EventResult.interruptTrue();
        }

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
        if (isPlayerLocked(player)) {
            return dev.architectury.event.EventResult.interruptTrue();
        }

        java.util.Optional<mc.sayda.creraces.capability.IPlayerVariables> varsOpt = DataUtils.getVariables(player);
        if (varsOpt.isPresent()) {
            mc.sayda.creraces.capability.IPlayerVariables vars = varsOpt.get();
            mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
            if (race != null && race.traits() != null) {
                for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                    if (trait.onBlockPlace(player, pos, state)) {
                        return dev.architectury.event.EventResult.interruptTrue();
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

        // Attack cancellation is also in PlayerMixin, but double checking here
        if (isPlayerLocked(player)) {
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
        mc.sayda.creraces.quest.QuestTracker.onPlayerKill(killer, victim);

        DataUtils.getVariables(killer).ifPresent(vars -> {
            // Trigger data-driven on_kill traits
            mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
            if (race != null && race.traits() != null) {
                for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                    trait.onKill(killer, victim);
                }
            }
        });

        // Coin drop logic
        if (mc.sayda.creraces.config.CreRacesConfig.COIN_DROP_ENABLED.get()
                && victim instanceof net.minecraft.world.entity.monster.AbstractIllager) {
            var taxingEnchant = mc.sayda.creraces.registry.ModEnchantments.TAXING.get();
            int taxingLevel = taxingEnchant != null
                    ? net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(taxingEnchant, killer)
                    : 0;

            float chance = 0.2f + (0.2f * taxingLevel);
            if (killer.getRandom().nextFloat() < chance) {
                // 20% chance for a Dime, 80% for a Penny (within the successful drop)
                net.minecraft.world.item.Item coin = killer.getRandom().nextFloat() < 0.2f
                        ? mc.sayda.creraces.registry.ModItems.DIME.get()
                        : mc.sayda.creraces.registry.ModItems.PENNY.get();

                victim.spawnAtLocation(new net.minecraft.world.item.ItemStack(coin));
            }
        }
    }

    private static void onIncidentPickup(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        mc.sayda.creraces.quest.QuestTracker.onItemPickup(player, stack);

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
        mc.sayda.creraces.race.AttributeIncidents.eikiJudgment(player);

        // Apply pending team removals for offline-kicked players (also called from onClientRequestedSync)
        mc.sayda.creraces.team.RaceTeamManager.handlePlayerJoin(player);

        // Re-apply race elements on login (Persistence Fix)
        mc.sayda.creraces.race.RaceIncidents.refreshPlayer(player);

        // If the player logged back in while inside the fairy realm, restore the full-scale override.
        // refreshPlayer above would have re-applied the race's base tiny scale (0.25), undoing it.
        net.minecraft.resources.ResourceLocation fairyRealmLoc =
                new net.minecraft.resources.ResourceLocation(CreRaces.MODID, "fairy_realm");
        if (player.level().dimension().location().equals(fairyRealmLoc)) {
            net.minecraft.server.level.ServerLevel fairyLevel =
                    (net.minecraft.server.level.ServerLevel) player.serverLevel();
            // Ensure world border is set; CHANGE_DIMENSION doesn't fire on direct login
            mc.sayda.creraces.worldgen.ModWorldgen.placeFairyTreeIfNeeded(fairyLevel);
                mc.sayda.creraces.worldgen.ModWorldgen.placeSeasonalTreesIfNeeded(fairyLevel);

            // Send border packet once the client has fully loaded in (20-tick delay on login)
            net.minecraft.world.level.border.WorldBorder border = fairyLevel.getWorldBorder();
            mc.sayda.creraces.util.Scheduler.delay(20, () -> {
                    if (player.isRemoved() || player.connection == null) return;
                    player.connection.send(
                            new net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket(border));
            });

            mc.sayda.creraces.capability.DataUtils.getVariables(player).ifPresent(vars -> {
                mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
                if (race != null) {
                    mc.sayda.creraces.race.RaceIncidents.applyFairyRealmScale(player, race);
                }
            });
        }

        // Sync the joining player's data to all currently online players,
        // and sync all online players' data to the joining player.
        // Iterates all online players but avoids an O(n²) full cross-sync:
        // each existing player only needs to receive the new player's data once.
        if (player.getServer() != null) {
            player.getServer().getPlayerList().getPlayers().forEach(other -> {
                if (other != player) {
                    BoundaryHandler.resyncVariables(player, other);
                    BoundaryHandler.resyncVariables(other, player);
                }
            });
        }
    }

    public static void onClientRequestedSync(ServerPlayer player) {
        LOGGER.debug("IncidentResolver: Client {} explicitly requested data sync.", player.getScoreboardName());
        // Initial sync when a player joins and is ready
        BoundaryHandler.resyncVariables(player, player);

        // Sync race, ability, and quest definitions
        BoundaryHandler.syncRacesToPlayer(player, mc.sayda.creraces.race.RaceManager.createSyncPacket());
        BoundaryHandler.syncAbilitiesToPlayer(player, mc.sayda.creraces.ability.AbilityManager.createSyncPacket());
        BoundaryHandler.syncQuestsToPlayer(player, mc.sayda.creraces.quest.QuestManager.createSyncPacket());
        
        // Handle team logic (offline kicks, etc.)
        mc.sayda.creraces.team.RaceTeamManager.handlePlayerJoin(player);
    }

    private static void onIncidentTransition(ServerPlayer player,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> oldLevel,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> newLevel) {
        BoundaryHandler.resyncVariables(player, player);

        net.minecraft.resources.ResourceLocation fairyRealm =
                new net.minecraft.resources.ResourceLocation(CreRaces.MODID, "fairy_realm");

        if (newLevel.location().equals(fairyRealm)) {
            // Place the island tree on first entry (level is loaded at this point)
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> fairyKey =
                    net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION, fairyRealm);
            net.minecraft.server.level.ServerLevel fairyLevel = player.server.getLevel(fairyKey);
            if (fairyLevel != null) {
                mc.sayda.creraces.worldgen.ModWorldgen.placeFairyTreeIfNeeded(fairyLevel);
                mc.sayda.creraces.worldgen.ModWorldgen.placeSeasonalTreesIfNeeded(fairyLevel);
                // Explicitly sync the world border to this player 2 ticks later.
                // CHANGE_DIMENSION fires before the client finishes loading the new level,
                // so the broadcast from setSize() misses them; we must send it directly.
                net.minecraft.world.level.border.WorldBorder border = fairyLevel.getWorldBorder();
                mc.sayda.creraces.util.Scheduler.delay(2, () -> {
                        if (player.isRemoved() || player.connection == null) return;
                        player.connection.send(
                                new net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket(border));
                });
            }

            DataUtils.getVariables(player).ifPresent(vars -> {
                mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
                if (race != null) {
                    mc.sayda.creraces.race.RaceIncidents.applyFairyRealmScale(player, race);
                }
            });
        } else if (oldLevel.location().equals(fairyRealm)) {
            mc.sayda.creraces.race.RaceIncidents.refreshPlayer(player);
        }
    }

    private static void onIncidentClone(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean wasDeath) {
        LOGGER.debug("IncidentResolver: onIncidentClone triggered! wasDeath: {}", wasDeath);
        DataUtils.getVariables(oldPlayer).ifPresent(oldVars -> {
            LOGGER.debug("IncidentResolver: Found oldVars. Race: {}", oldVars.getRace());
            DataUtils.getVariables(newPlayer).ifPresent(newVars -> {
                newVars.deserialize(oldVars.serialize());
                LOGGER.debug("IncidentResolver: Copied oldVars to newVars. New Race: {}", newVars.getRace());

                if (wasDeath) {
                    newVars.resetOnDeath();
                }
                mc.sayda.creraces.race.RaceIncidents.refreshPlayer(newPlayer);

                // After cloning, sync to the new player
                BoundaryHandler.resyncVariables(newPlayer, newPlayer);
            });
        });
    }

    private static void onIncidentTick(net.minecraft.server.MinecraftServer server) {
        if (mc.sayda.creraces.config.CreRacesConfig.FORCED_SELECTION.get()) { // FORCED_SELECTION default is true
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                DataUtils.getVariables(player).ifPresent(vars -> {
                    if (!vars.hasChosenRace()) {
                        net.minecraft.world.effect.MobEffect res = net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE;
                        if (res != null) {
                            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                    res, 40, 255, false, false, false));
                        }
                        var stunned = mc.sayda.creraces.registry.ModMobEffects.STUNNED.get();
                        if (stunned != null) {
                            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                    stunned, 40, 0, false, false, false));
                        }
                    }
                });
            }
        }

        // Ticking down resources/cooldowns
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            mc.sayda.creraces.race.ResourceTicker.tick(player);
            if (player.tickCount % 300 == 0) {
                mc.sayda.creraces.quest.QuestTracker.tickQuests(player);
            }
            if (player.tickCount % 20 == 0) {
                mc.sayda.creraces.quest.QuestTracker.recheckCollectProgress(player);
            }
        }

        mc.sayda.creraces.util.Scheduler.tick();
        mc.sayda.creraces.team.RaceTeamManager.tick(server);
        mc.sayda.creraces.territory.TerritoryManager.get().tick(server);
        mc.sayda.creraces.engine.ChannelingManager.tick(server);
    }

public static void onTrackingBegin(ServerPlayer tracker, net.minecraft.world.entity.Entity target) {
        if (target instanceof Player targetPlayer) {
            BoundaryHandler.resyncVariables(targetPlayer, tracker);
        }
    }

    public static void onRespawn(ServerPlayer player) {
        LOGGER.debug("IncidentResolver: onRespawn triggered for {}", player.getName().getString());

        mc.sayda.creraces.race.RaceIncidents.refreshPlayer(player);

        // Set default respawn states for resources
        DataUtils.getVariables(player).ifPresent(vars -> {
            net.minecraft.world.entity.ai.attributes.Attribute maxMana = mc.sayda.creraces.registry.ModAttributes.MAX_MANA.get();
            net.minecraft.world.entity.ai.attributes.Attribute maxEnergy = mc.sayda.creraces.registry.ModAttributes.MAX_ENERGY.get();
            if (maxMana != null) vars.setMana(player.getAttributeValue(maxMana));
            if (maxEnergy != null) vars.setEnergy(player.getAttributeValue(maxEnergy));
            vars.setRage(0);
            vars.setGrit(0);

            // Trigger on_respawn traits
            mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
            if (race != null && race.traits() != null) {
                for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                    trait.onRespawn(player);
                }
            }

            // Race-defined default respawn: used when the player has no bed/anchor spawn set
            if (race != null && race.respawnPos() != null && player.getRespawnPosition() == null) {
                double[] pos = race.respawnPos();
                if (pos != null && pos.length >= 3) {
                    net.minecraft.server.level.ServerLevel targetLevel = player.serverLevel();
                    if (race.respawnDimension() != null) {
                        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimKey =
                                net.minecraft.resources.ResourceKey.create(
                                        net.minecraft.core.registries.Registries.DIMENSION,
                                        race.respawnDimension());
                        net.minecraft.server.level.ServerLevel dimLevel = player.server.getLevel(dimKey);
                        if (dimLevel != null) targetLevel = dimLevel;
                    }
                    player.teleportTo(targetLevel, pos[0], pos[1], pos[2], player.getYRot(), player.getXRot());
                }
            }
        });
    }
}
