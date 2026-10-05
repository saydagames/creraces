package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.BiomeChecker;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.territory.FactionLeaderManager;
import mc.sayda.creraces.territory.TerritoryManager;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Claims the territory island around the interacted block (or the caster) for the caster's race,
 * then runs the branch matching the claim result.
 */
public class ClaimTerritoryAction implements ActionRegistry.RaceAction {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "claim_territory");

    private final List<String> validBiomes;
    private final boolean leaderOnly;
    private final int anchorYOffset;
    private final ResourceLocation nodeXState;
    private final ResourceLocation nodeYState;
    private final ResourceLocation nodeZState;
    private final List<ActionRegistry.RaceAction> onSuccess;
    private final List<ActionRegistry.RaceAction> onNotLeader;
    private final List<ActionRegistry.RaceAction> onInvalidBiome;
    private final List<ActionRegistry.RaceAction> onEnemyTerritory;
    private final List<ActionRegistry.RaceAction> onInsideOwn;

    public ClaimTerritoryAction(List<String> validBiomes, boolean leaderOnly, int anchorYOffset,
            ResourceLocation nodeXState, ResourceLocation nodeYState, ResourceLocation nodeZState,
            List<ActionRegistry.RaceAction> onSuccess, List<ActionRegistry.RaceAction> onNotLeader,
            List<ActionRegistry.RaceAction> onInvalidBiome, List<ActionRegistry.RaceAction> onEnemyTerritory,
            List<ActionRegistry.RaceAction> onInsideOwn) {
        this.validBiomes = validBiomes;
        this.leaderOnly = leaderOnly;
        this.anchorYOffset = anchorYOffset;
        this.nodeXState = nodeXState;
        this.nodeYState = nodeYState;
        this.nodeZState = nodeZState;
        this.onSuccess = onSuccess;
        this.onNotLeader = onNotLeader;
        this.onInvalidBiome = onInvalidBiome;
        this.onEnemyTerritory = onEnemyTerritory;
        this.onInsideOwn = onInsideOwn;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }

        ResourceLocation raceId = DataUtils.getVariables(player).map(IPlayerVariables::getRace).orElse(null);
        if (raceId == null || raceId.getPath().equals("none")) {
            return ActionRegistry.runChain(onInvalidBiome, player, target, slot, interactPos);
        }
        Race race = RaceRegistry.get(raceId);
        if (race == null || !race.enableTerritory()) {
            return false;
        }

        // Fall back to the race's own biome list so the sapling placement check and the map preview agree.
        List<String> biomes = validBiomes.isEmpty() ? race.claimValidBiomes() : validBiomes;
        float threshold = race.claimBiomeThreshold();
        if (!biomes.isEmpty() && !isChunkBiomeValid(serverPlayer, interactPos, biomes, threshold)) {
            return ActionRegistry.runChain(onInvalidBiome, player, target, slot, interactPos);
        }

        // Elect a leader first so the leader map is always populated.
        FactionLeaderManager.electIfAbsent(serverPlayer);
        if (leaderOnly && !FactionLeaderManager.isLeader(player)) {
            ActionRegistry.runChain(onNotLeader, player, target, slot, interactPos);
            return false;
        }

        // Centre the claim on the interacted block's chunk (e.g. a placed sapling), or the caster's for plain casts.
        BlockPos origin = interactPos != null ? interactPos : player.blockPosition();
        int sampleY = origin.getY();
        Predicate<ChunkPos> biomeFilter = biomes.isEmpty()
                ? chunk -> true
                : chunk -> BiomeChecker.matchesChunk(serverPlayer.level(), chunk, sampleY, biomes, threshold);

        TerritoryManager territories = TerritoryManager.get();
        TerritoryManager.ClaimResult result = territories.claimIsland(raceId, new ChunkPos(origin),
                player.getUUID(), biomeFilter);
        if ((result.type == TerritoryManager.ClaimResultType.SUCCESS
                || result.type == TerritoryManager.ClaimResultType.INSIDE_OWN_TERRITORY)
                && (!result.newClaims.isEmpty() || !result.preExistingClaims.isEmpty())) {
            BlockPos anchorPos = interactPos != null ? interactPos.offset(0, anchorYOffset, 0) : player.blockPosition();
            territories.placeRootBlock(anchorPos, result.newClaims, result.preExistingClaims);
        }
        return switch (result.type) {
            case SUCCESS              -> ActionRegistry.runChain(onSuccess, player, target, slot, interactPos);
            case ENEMY_TERRITORY      -> ActionRegistry.runChain(onEnemyTerritory, player, target, slot, interactPos);
            case INSIDE_OWN_TERRITORY -> ActionRegistry.runChain(onInsideOwn, player, target, slot, interactPos);
            case INVALID_BIOME        -> ActionRegistry.runChain(onInvalidBiome, player, target, slot, interactPos);
            case INSUFFICIENT_COINS, ANCHOR_CHUNK, UNCLAIM_SUCCESS, OUT_OF_RANGE, NOT_LEADER, MAX_NODES_REACHED -> false;
        };
    }

    /**
     * Whether the chunk being claimed meets the biome threshold. In the spirit realm (a state flag,
     * not a dimension) the caster's own position means nothing, so the chunk of their stored
     * overworld node position is checked instead.
     */
    private boolean isChunkBiomeValid(ServerPlayer player, @Nullable BlockPos interactPos, List<String> biomes,
            float threshold) {
        LevelReader level;
        ChunkPos chunk;
        IPlayerVariables vars = DataUtils.getVariables(player).orElse(null);
        if (vars != null && vars.isInSpiritRealm()) {
            double nx = vars.getPersistentState(nodeXState);
            double ny = vars.getPersistentState(nodeYState);
            double nz = vars.getPersistentState(nodeZState);
            if (nx == 0 && ny == 0 && nz == 0) {
                return true;
            }
            ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
            if (overworld == null) {
                return true;
            }
            level = overworld;
            chunk = new ChunkPos(new BlockPos((int) nx, (int) ny, (int) nz));
        } else {
            level = player.level();
            chunk = new ChunkPos(interactPos != null ? interactPos : player.blockPosition());
        }
        int sampleY = interactPos != null ? interactPos.getY() : player.blockPosition().getY();
        return BiomeChecker.matchesChunk(level, chunk, sampleY, biomes, threshold);
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            List<String> validBiomes = new ArrayList<>();
            if (json.has("valid_biomes")) {
                for (JsonElement biome : json.getAsJsonArray("valid_biomes")) {
                    validBiomes.add(biome.getAsString());
                }
            }
            return new ClaimTerritoryAction(
                    validBiomes,
                    GsonHelper.getAsBoolean(json, "leader_only", false),
                    GsonHelper.getAsInt(json, "anchor_y_offset", -1),
                    stateKey(json, "node_x_state", "node_x"),
                    stateKey(json, "node_y_state", "node_y"),
                    stateKey(json, "node_z_state", "node_z"),
                    ActionRegistry.listFromJson(json, "on_success"),
                    ActionRegistry.listFromJson(json, "on_not_leader"),
                    ActionRegistry.listFromJson(json, "on_invalid_biome"),
                    ActionRegistry.listFromJson(json, "on_enemy_territory"),
                    ActionRegistry.listFromJson(json, "on_inside_own"));
        });
    }

    private static ResourceLocation stateKey(JsonObject json, String key, String defaultPath) {
        return json.has(key)
                ? ResourceLocation.parse(json.get(key).getAsString())
                : ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, defaultPath);
    }
}
