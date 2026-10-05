package mc.sayda.creraces;

import com.mojang.logging.LogUtils;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import mc.sayda.creraces.ability.AbilityManager;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ChannelingManager;
import mc.sayda.creraces.engine.TraitDispatch;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.global.GlobalDispatcher;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.quest.QuestManager;
import mc.sayda.creraces.quest.QuestSessionRegistry;
import mc.sayda.creraces.quest.QuestTracker;
import mc.sayda.creraces.race.AttributeIncidents;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceIncidents;
import mc.sayda.creraces.race.RaceManager;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.race.ResourceTicker;
import mc.sayda.creraces.race.SocialPassivesEvent;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.registry.ModEnchantments;
import mc.sayda.creraces.registry.ModItems;
import mc.sayda.creraces.registry.ModMobEffects;
import mc.sayda.creraces.team.RaceTeamManager;
import mc.sayda.creraces.territory.FactionLeaderManager;
import mc.sayda.creraces.territory.TerritoryManager;
import mc.sayda.creraces.util.CombatUtils;
import mc.sayda.creraces.util.DamageGuard;
import mc.sayda.creraces.util.PocketManager;
import mc.sayda.creraces.util.ReturnPoint;
import mc.sayda.creraces.util.Scheduler;
import mc.sayda.creraces.villager.GuildReceptionistOfferSync;
import mc.sayda.creraces.worldgen.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import org.slf4j.Logger;

/**
 * Resolves incidents (events) within the world.
 * Manages data synchronization and the flow of time (cooldowns).
 */
public class IncidentResolver {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceKey<Level> FAIRY_REALM =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "fairy_realm"));

    public static void init() {
        registerPlayerEvents();
        registerLifecycleEvents();
        SocialPassivesEvent.register();
        registerWorldEvents();
    }

    private static void registerPlayerEvents() {
        PlayerEvent.PLAYER_JOIN.register(IncidentResolver::onIncidentBegin);
        PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wonGame) -> onIncidentClone(oldPlayer, newPlayer, !wonGame));
        PlayerEvent.CHANGE_DIMENSION.register(IncidentResolver::onIncidentTransition);
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, removalReason) -> onRespawn(player));
        PlayerEvent.PICKUP_ITEM_POST.register((player, itemEntity, stack) -> {
            if (player instanceof ServerPlayer sp) {
                onIncidentPickup(sp, stack);
            }
        });

        TickEvent.SERVER_POST.register(IncidentResolver::onIncidentTick);

        PlayerEvent.PLAYER_JOIN.register(FactionLeaderManager::onPlayerJoin);

        // Disconnect: clear transient engine state and hand faction leadership on
        PlayerEvent.PLAYER_QUIT.register(player -> {
            ActionRegistry.cleanup(player);
            QuestSessionRegistry.clearPlayer(player.getUUID());
            TerritoryManager.get().clearPlayerTracking(player.getUUID());
            MinecraftServer server = player.getServer();
            if (server != null) {
                FactionLeaderManager.onPlayerLeave(player, server);
            }
        });
    }

    private static void registerLifecycleEvents() {
        LifecycleEvent.SERVER_STARTED.register(server -> {
            RaceTeamManager.load(server);
            PocketManager.load(server);
            TerritoryManager.load(server);
        });
        // Save persisted state first, then drop everything transient
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            RaceTeamManager.save(server);
            PocketManager.save(server);
            TerritoryManager.save(server);
            Scheduler.clear();
            FactionLeaderManager.clear();
            PocketManager.onServerStop();
            ModWorldgen.onServerStop();
        });
    }

    private static void registerWorldEvents() {
        EntityEvent.ADD.register((entity, level) -> {
            // No hostile mobs in the fairy realm
            if (!level.isClientSide()
                    && entity instanceof Monster
                    && level instanceof ServerLevel sl
                    && isFairyRealm(sl.dimension())) {
                entity.discard();
                return EventResult.interruptFalse();
            }
            GlobalDispatcher.onEntitySpawn(entity);
            return EventResult.pass();
        });

        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            Player killer = CombatUtils.getRootOwner(source.getEntity());
            if (killer instanceof ServerPlayer sp) {
                onIncidentVictory(sp, entity);
            }
            GlobalDispatcher.onLivingDeath(entity, source);
            return EventResult.pass();
        });

        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> {
            if (player instanceof ServerPlayer sp) {
                return onIncidentInteraction(sp, hand);
            }
            return CompoundEventResult.<ItemStack>pass();
        });

        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, direction) -> {
            if (player instanceof ServerPlayer sp) {
                return onIncidentBlockInteraction(sp, hand, pos);
            }
            return EventResult.pass();
        });

        BlockEvent.PLACE.register((level, pos, state, placer) -> {
            if (placer instanceof ServerPlayer sp && !level.isClientSide()) {
                return onIncidentBlockPlace(sp, pos, state);
            }
            return EventResult.pass();
        });

        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            if (source.getEntity() instanceof ServerPlayer player) {
                onIncidentAttack(player, entity);
            }
            GlobalDispatcher.onLivingHurt(entity, source);
            return EventResult.pass();
        });

        BlockEvent.BREAK.register((level, pos, state, player, xp) -> {
            if (!level.isClientSide() && player != null) {
                QuestTracker.onBlockBroken(player, state);
            }
            return EventResult.pass();
        });

        // Guild Receptionist trade offers are injected here rather than in Villager's own
        // trade list registration, since vanilla only ever activates 2 trades per level -
        // syncing on interaction guarantees all 5 tiers are present the moment a player opens
        // the trade GUI, regardless of the villager's current level.
        InteractionEvent.INTERACT_ENTITY.register((player, entity, hand) -> {
            if (player instanceof ServerPlayer && entity instanceof Villager villager) {
                GuildReceptionistOfferSync.sync(villager);
            }
            return EventResult.pass();
        });
    }

    private static boolean isFairyRealm(ResourceKey<Level> dimension) {
        return dimension.location().equals(FAIRY_REALM.location());
    }

    private static boolean isPlayerLocked(ServerPlayer player) {
        var stunned = ModMobEffects.STUNNED;
        if (stunned != null && player.hasEffect(stunned)) {
            return true;
        }
        if (CreRacesConfig.FORCED_SELECTION.get()) {
            return !DataUtils.getVariables(player).map(IPlayerVariables::hasChosenRace).orElse(true);
        }
        return false;
    }

    private static CompoundEventResult<ItemStack> onIncidentInteraction(ServerPlayer player, InteractionHand hand) {
        if (isPlayerLocked(player)) {
            return CompoundEventResult.interruptTrue(player.getItemInHand(hand));
        }

        ItemStack stack = player.getItemInHand(hand);
        boolean handled = TraitDispatch.runUntilTrue("onIncidentInteraction", player,
                trait -> trait.onInteraction(player, stack));
        if (handled) {
            return CompoundEventResult.interruptTrue(stack);
        }
        return CompoundEventResult.pass();
    }

    private static EventResult onIncidentBlockInteraction(ServerPlayer player, InteractionHand hand, BlockPos pos) {
        if (isPlayerLocked(player)) {
            return EventResult.interruptTrue();
        }

        BlockState state = player.level().getBlockState(pos);
        boolean handled = TraitDispatch.runUntilTrue("onIncidentBlockInteraction", player,
                trait -> trait.onBlockInteraction(player, pos, state));
        if (handled) {
            return EventResult.interruptTrue();
        }
        return EventResult.pass();
    }

    private static EventResult onIncidentBlockPlace(ServerPlayer player, BlockPos pos, BlockState state) {
        if (isPlayerLocked(player)) {
            return EventResult.interruptTrue();
        }

        boolean handled = TraitDispatch.runUntilTrue("onIncidentBlockPlace", player,
                trait -> trait.onBlockPlace(player, pos, state));
        if (handled) {
            return EventResult.interruptTrue();
        }
        return EventResult.pass();
    }

    private static void onIncidentAttack(ServerPlayer player, LivingEntity victim) {
        if (DamageGuard.isProcessing()) {
            return;
        }

        // PlayerMixin cancels attacks from locked players too; this is the second line of defence
        if (isPlayerLocked(player)) {
            return;
        }

        DamageGuard.setProcessing(true);
        try {
            TraitDispatch.runVoid("onIncidentAttack", player, trait -> trait.onHit(player, victim));
        } finally {
            DamageGuard.setProcessing(false);
        }
    }

    private static void onIncidentVictory(ServerPlayer killer, LivingEntity victim) {
        QuestTracker.onPlayerKill(killer, victim);
        TraitDispatch.runVoid("onIncidentVictory", killer, trait -> trait.onKill(killer, victim));

        if (CreRacesConfig.COIN_DROP_ENABLED.get() && victim instanceof AbstractIllager) {
            var taxingEnchant = ModEnchantments.get(killer.level(), ModEnchantments.TAXING);
            int taxingLevel = taxingEnchant != null
                    ? EnchantmentHelper.getEnchantmentLevel(taxingEnchant, killer)
                    : 0;

            float chance = 0.2f + (0.2f * taxingLevel);
            if (killer.getRandom().nextFloat() < chance) {
                // 20% chance for a Dime, 80% for a Penny (within the successful drop)
                Item coin = killer.getRandom().nextFloat() < 0.2f
                        ? ModItems.DIME.get()
                        : ModItems.PENNY.get();

                victim.spawnAtLocation(new ItemStack(coin));
            }
        }
    }

    private static void onIncidentPickup(ServerPlayer player, ItemStack stack) {
        QuestTracker.onItemPickup(player, stack);
        TraitDispatch.runVoid("onIncidentPickup", player, trait -> trait.onItemPickup(player, stack));
    }

    private static void onIncidentBegin(ServerPlayer player) {
        AttributeIncidents.eikiJudgment(player);

        // Applies team removals for players kicked while offline (onClientRequestedSync repeats this)
        RaceTeamManager.handlePlayerJoin(player);

        // Race elements have to be re-applied on every login
        RaceIncidents.refreshPlayer(player);

        // refreshPlayer just re-applied the race's base scale, which would undo the fairy realm's
        // full-scale override for a player logging back in there
        if (isFairyRealm(player.level().dimension())) {
            // CHANGE_DIMENSION doesn't fire on a direct login, and the client needs ~20 ticks to load in
            prepareFairyRealm(player, player.serverLevel(), 20);
            applyFairyRealmScale(player);
        }

        // Existing players learn about the newcomer and the newcomer about each of them, once each
        MinecraftServer server = player.getServer();
        if (server != null) {
            server.getPlayerList().getPlayers().forEach(other -> {
                if (other != player) {
                    BoundaryHandler.resyncVariables(player, other);
                    BoundaryHandler.resyncVariables(other, player);
                }
            });
        }
    }

    public static void onClientRequestedSync(ServerPlayer player) {
        LOGGER.debug("IncidentResolver: Client {} explicitly requested data sync.", player.getScoreboardName());
        BoundaryHandler.resyncVariables(player, player);

        BoundaryHandler.syncRacesToPlayer(player, RaceManager.createSyncPacket());
        BoundaryHandler.syncAbilitiesToPlayer(player, AbilityManager.createSyncPacket());
        BoundaryHandler.syncQuestsToPlayer(player, QuestManager.createSyncPacket());

        // Offline kicks and other pending team changes
        RaceTeamManager.handlePlayerJoin(player);
    }

    private static void onIncidentTransition(ServerPlayer player, ResourceKey<Level> oldLevel, ResourceKey<Level> newLevel) {
        BoundaryHandler.resyncVariables(player, player);
        ReturnPoint.forgetUnlessIn(player, newLevel);

        if (isFairyRealm(newLevel)) {
            ServerLevel fairyLevel = player.server.getLevel(FAIRY_REALM);
            if (fairyLevel != null) {
                // CHANGE_DIMENSION fires before the client finishes loading the new level, so the
                // border broadcast from setSize() misses them; a short delay lets it land.
                prepareFairyRealm(player, fairyLevel, 2);
            }
            applyFairyRealmScale(player);
        } else if (isFairyRealm(oldLevel)) {
            RaceIncidents.refreshPlayer(player);
        }
    }

    /** Places the island trees on first entry and sends the realm's world border straight to the player. */
    private static void prepareFairyRealm(ServerPlayer player, ServerLevel fairyLevel, int borderDelayTicks) {
        ModWorldgen.placeFairyTreeIfNeeded(fairyLevel);
        ModWorldgen.placeSeasonalTreesIfNeeded(fairyLevel);

        WorldBorder border = fairyLevel.getWorldBorder();
        Scheduler.delay(borderDelayTicks, () -> {
            if (player.isRemoved() || player.connection == null) return;
            player.connection.send(new ClientboundInitializeBorderPacket(border));
        });
    }

    private static void applyFairyRealmScale(ServerPlayer player) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race != null) {
                RaceIncidents.applyFairyRealmScale(player, race);
            }
        });
    }

    private static void onIncidentClone(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean wasDeath) {
        LOGGER.debug("IncidentResolver: onIncidentClone (wasDeath: {})", wasDeath);
        DataUtils.getVariables(oldPlayer).ifPresent(oldVars -> {
            LOGGER.debug("IncidentResolver: Found oldVars. Race: {}", oldVars.getRace());
            DataUtils.getVariables(newPlayer).ifPresent(newVars -> {
                newVars.deserialize(oldVars.serialize());
                LOGGER.debug("IncidentResolver: Copied oldVars to newVars. New Race: {}", newVars.getRace());

                if (wasDeath) {
                    newVars.resetOnDeath();
                }
                RaceIncidents.refreshPlayer(newPlayer);
                BoundaryHandler.resyncVariables(newPlayer, newPlayer);
            });
        });
    }

    private static void onIncidentTick(MinecraftServer server) {
        GlobalDispatcher.onWorldTick(server);

        if (CreRacesConfig.FORCED_SELECTION.get()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                holdUntilRaceChosen(player);
            }
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ResourceTicker.tick(player);
            if (player.tickCount % 300 == 0) {
                QuestTracker.tickQuests(player);
            }
            if (player.tickCount % 20 == 0) {
                QuestTracker.recheckCollectProgress(player);
            }
        }

        Scheduler.tick();
        RaceTeamManager.tick(server);
        TerritoryManager.get().tick(server);
        ChannelingManager.tick(server);
    }

    /** Keeps a player who hasn't picked a race yet invulnerable and stunned, refreshed every tick. */
    private static void holdUntilRaceChosen(ServerPlayer player) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            if (vars.hasChosenRace()) {
                return;
            }
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 255, false, false, false));
            var stunned = ModMobEffects.STUNNED;
            if (stunned != null) {
                player.addEffect(new MobEffectInstance(stunned, 40, 0, false, false, false));
            }
        });
    }

    public static void onRespawn(ServerPlayer player) {
        LOGGER.debug("IncidentResolver: onRespawn triggered for {}", player.getName().getString());

        RaceIncidents.refreshPlayer(player);

        DataUtils.getVariables(player).ifPresent(vars -> {
            Holder<Attribute> maxMana = ModAttributes.MAX_MANA;
            Holder<Attribute> maxEnergy = ModAttributes.MAX_ENERGY;
            if (maxMana != null) vars.setMana(player.getAttributeValue(maxMana));
            if (maxEnergy != null) vars.setEnergy(player.getAttributeValue(maxEnergy));
            vars.setRage(0);
            vars.setGrit(0);

            Race race = RaceRegistry.get(vars.getRace());
            if (race != null && race.traits() != null) {
                for (TraitRegistry.RaceTrait trait : race.traits()) {
                    trait.onRespawn(player);
                }
            }

            if (race != null && player.getRespawnPosition() == null) {
                teleportToRaceRespawn(player, race);
            }
        });
    }

    /** Races can define a default respawn point, used when the player has no bed or anchor spawn set. */
    private static void teleportToRaceRespawn(ServerPlayer player, Race race) {
        double[] pos = race.respawnPos();
        if (pos == null || pos.length < 3) {
            return;
        }
        ServerLevel targetLevel = player.serverLevel();
        if (race.respawnDimension() != null) {
            ServerLevel dimLevel = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, race.respawnDimension()));
            if (dimLevel != null) targetLevel = dimLevel;
        }
        player.teleportTo(targetLevel, pos[0], pos[1], pos[2], player.getYRot(), player.getXRot());
    }
}
