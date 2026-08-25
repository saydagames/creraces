package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

public class ClaimTerritoryAction implements ActionRegistry.RaceAction {

    public static final ResourceLocation ID = new ResourceLocation("creraces", "claim_territory");

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

    public ClaimTerritoryAction(List<String> validBiomes,
            boolean leaderOnly,
            int anchorYOffset,
            ResourceLocation nodeXState,
            ResourceLocation nodeYState,
            ResourceLocation nodeZState,
            List<ActionRegistry.RaceAction> onSuccess,
            List<ActionRegistry.RaceAction> onNotLeader,
            List<ActionRegistry.RaceAction> onInvalidBiome,
            List<ActionRegistry.RaceAction> onEnemyTerritory,
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
    public boolean execute(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable BlockPos interact_pos) {

        if (!(player instanceof ServerPlayer serverPlayer)) return true;

        ResourceLocation raceId = DataUtils.getVariables(player)
                .map(IPlayerVariables::getRace)
                .orElse(null);
        if (raceId == null || raceId.getPath().equals("none")) {
            return runBranch(onInvalidBiome, player, target, slot, interact_pos);
        }

        mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(raceId);
        if (race == null || !race.enableTerritory()) return false;

        // Prefer the action's own valid_biomes list; fall back to the race's territory_valid_biomes
        // so the sapling placement check and the map preview always agree.
        List<String> effectiveBiomes = validBiomes.isEmpty() ? race.claimValidBiomes() : validBiomes;

        float threshold = race.claimBiomeThreshold();

        // Gate: the chunk containing the interact position must meet the biome threshold
        if (!effectiveBiomes.isEmpty() && !isChunkBiomeValid(serverPlayer, interact_pos, effectiveBiomes, threshold)) {
            return runBranch(onInvalidBiome, player, target, slot, interact_pos);
        }

        // Elect a faction leader if none exists yet, so the leader map is always populated.
        mc.sayda.creraces.territory.FactionLeaderManager.electIfAbsent(serverPlayer);
        if (leaderOnly && !mc.sayda.creraces.territory.FactionLeaderManager.isLeader(player)) {
            runBranch(onNotLeader, player, target, slot, interact_pos);
            return false;
        }

        TerritoryManager tm = TerritoryManager.get();
        // Center the claim on the target block's chunk (e.g. where the sapling is placed),
        // falling back to the player's chunk for ability-cast usage where interact_pos is null.
        ChunkPos centerChunk = interact_pos != null
                ? new ChunkPos(interact_pos)
                : new ChunkPos(player.blockPosition());

        // Per-chunk biome filter: only claim chunks whose biome grid meets the threshold
        final int sampleY = interact_pos != null ? interact_pos.getY() : player.blockPosition().getY();
        final List<String> biomesForFilter = effectiveBiomes;
        final float chunkThreshold = threshold;
        java.util.function.Predicate<ChunkPos> biomeFilter = biomesForFilter.isEmpty()
                ? cp -> true
                : cp -> mc.sayda.creraces.engine.BiomeChecker.matchesChunk(
                        serverPlayer.level(), cp, sampleY, biomesForFilter, chunkThreshold);

        TerritoryManager.ClaimResult result = tm.claimIsland(raceId, centerChunk, player.getUUID(), biomeFilter);
        if ((result.type == TerritoryManager.ClaimResultType.SUCCESS
                || result.type == TerritoryManager.ClaimResultType.INSIDE_OWN_TERRITORY)
                && (!result.newClaims.isEmpty() || !result.preExistingClaims.isEmpty())) {
            net.minecraft.core.BlockPos anchorPos = interact_pos != null ? interact_pos.offset(0, anchorYOffset, 0) : player.blockPosition();
            tm.placeRootBlock(anchorPos, result.newClaims, result.preExistingClaims);
        }
        return switch (result.type) {
            case SUCCESS              -> runBranch(onSuccess, player, target, slot, interact_pos);
            case ENEMY_TERRITORY      -> runBranch(onEnemyTerritory, player, target, slot, interact_pos);
            case INSIDE_OWN_TERRITORY -> runBranch(onInsideOwn, player, target, slot, interact_pos);
            case INVALID_BIOME        -> runBranch(onInvalidBiome, player, target, slot, interact_pos);
            case INSUFFICIENT_COINS, ANCHOR_CHUNK, UNCLAIM_SUCCESS, OUT_OF_RANGE, NOT_LEADER, MAX_NODES_REACHED -> false;
        };
    }

    /**
     * Gate check: does the chunk containing the target position meet the biome threshold?
     * If the player is currently in spirit form (Phaseshift, a state flag - not a dimension
     * change), checks the chunk of their stored overworld node position instead, since their
     * physical position while phased isn't a meaningful claim location.
     */
    private boolean isChunkBiomeValid(ServerPlayer player, @javax.annotation.Nullable BlockPos interactPos,
            List<String> biomes, float threshold) {
        net.minecraft.world.level.LevelReader level;
        ChunkPos cp;
        var vars = DataUtils.getVariables(player).orElse(null);
        if (vars != null && vars.isInSpiritRealm()) {
            double nx = vars.getPersistentState(nodeXState);
            double ny = vars.getPersistentState(nodeYState);
            double nz = vars.getPersistentState(nodeZState);
            if (nx == 0 && ny == 0 && nz == 0) return true;
            net.minecraft.server.level.ServerLevel overworld =
                    player.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (overworld == null) return true;
            level = overworld;
            cp = new ChunkPos(new BlockPos((int) nx, (int) ny, (int) nz));
        } else {
            BlockPos pos = interactPos != null ? interactPos : player.blockPosition();
            level = player.level();
            cp = new ChunkPos(pos);
        }
        return mc.sayda.creraces.engine.BiomeChecker.matchesChunk(level, cp,
                interactPos != null ? interactPos.getY() : player.blockPosition().getY(),
                biomes, threshold);
    }

    private static boolean runBranch(List<ActionRegistry.RaceAction> branch, Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable BlockPos interact_pos) {
        for (ActionRegistry.RaceAction action : branch) {
            if (!action.execute(player, target, slot, interact_pos)) return false;
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            List<String> validBiomes = new ArrayList<>();
            if (json.has("valid_biomes")) {
                for (JsonElement e : json.getAsJsonArray("valid_biomes")) {
                    validBiomes.add(e.getAsString());
                }
            }

            boolean leaderOnly = json.has("leader_only") && json.get("leader_only").getAsBoolean();
            int anchorYOffset = json.has("anchor_y_offset") ? json.get("anchor_y_offset").getAsInt() : -1;

            ResourceLocation nodeX = json.has("node_x_state")
                    ? new ResourceLocation(json.get("node_x_state").getAsString())
                    : new ResourceLocation("creraces", "node_x");
            ResourceLocation nodeY = json.has("node_y_state")
                    ? new ResourceLocation(json.get("node_y_state").getAsString())
                    : new ResourceLocation("creraces", "node_y");
            ResourceLocation nodeZ = json.has("node_z_state")
                    ? new ResourceLocation(json.get("node_z_state").getAsString())
                    : new ResourceLocation("creraces", "node_z");

            return new ClaimTerritoryAction(
                    validBiomes,
                    leaderOnly,
                    anchorYOffset,
                    nodeX,
                    nodeY,
                    nodeZ,
                    parseList(json, "on_success"),
                    parseList(json, "on_not_leader"),
                    parseList(json, "on_invalid_biome"),
                    parseList(json, "on_enemy_territory"),
                    parseList(json, "on_inside_own"));
        });
    }

    private static List<ActionRegistry.RaceAction> parseList(JsonObject json, String key) {
        List<ActionRegistry.RaceAction> list = new ArrayList<>();
        if (!json.has(key)) return list;
        JsonArray arr = json.getAsJsonArray(key);
        for (int i = 0; i < arr.size(); i++) {
            list.add(ActionRegistry.fromJson(arr.get(i).getAsJsonObject()));
        }
        return list;
    }
}
