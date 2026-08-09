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
    private final List<ActionRegistry.RaceAction> onSuccess;
    private final List<ActionRegistry.RaceAction> onPartial;
    private final List<ActionRegistry.RaceAction> onInvalidBiome;
    private final List<ActionRegistry.RaceAction> onEnemyTerritory;
    private final List<ActionRegistry.RaceAction> onInsideOwn;

    public ClaimTerritoryAction(List<String> validBiomes,
            List<ActionRegistry.RaceAction> onSuccess,
            List<ActionRegistry.RaceAction> onPartial,
            List<ActionRegistry.RaceAction> onInvalidBiome,
            List<ActionRegistry.RaceAction> onEnemyTerritory,
            List<ActionRegistry.RaceAction> onInsideOwn) {
        this.validBiomes = validBiomes;
        this.onSuccess = onSuccess;
        this.onPartial = onPartial;
        this.onInvalidBiome = onInvalidBiome;
        this.onEnemyTerritory = onEnemyTerritory;
        this.onInsideOwn = onInsideOwn;
    }

    private static final ResourceLocation SPIRIT_REALM = new ResourceLocation("creraces", "spirit_realm");
    private static final ResourceLocation NODE_X = new ResourceLocation("creraces", "node_x");
    private static final ResourceLocation NODE_Y = new ResourceLocation("creraces", "node_y");
    private static final ResourceLocation NODE_Z = new ResourceLocation("creraces", "node_z");

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

        // Gate: the target block position (or player position) must be in a valid biome
        if (!effectiveBiomes.isEmpty() && !isCenterBiomeValid(serverPlayer, interact_pos, effectiveBiomes)) {
            return runBranch(onInvalidBiome, player, target, slot, interact_pos);
        }

        // Leader gate: all territory races require the player to be leader
        if (race.enableTerritory()) {
            mc.sayda.creraces.territory.FactionLeaderManager.electIfAbsent(serverPlayer);
            if (!mc.sayda.creraces.territory.FactionLeaderManager.isLeader(player)) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("msg.creraces.faction.not_leader"), false);
                return false;
            }
        }

        TerritoryManager tm = TerritoryManager.get();
        // Center the claim on the target block's chunk (e.g. where the sapling is placed),
        // falling back to the player's chunk for ability-cast usage where interact_pos is null.
        ChunkPos centerChunk = interact_pos != null
                ? new ChunkPos(interact_pos)
                : new ChunkPos(player.blockPosition());

        // Per-chunk biome filter: only claim chunks that are in the valid biome
        final int sampleY = interact_pos != null ? interact_pos.getY() : player.blockPosition().getY();
        final List<String> biomesForFilter = effectiveBiomes;
        java.util.function.Predicate<ChunkPos> biomeFilter = biomesForFilter.isEmpty()
                ? cp -> true
                : cp -> {
                    @SuppressWarnings("null")
                    net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> h =
                            serverPlayer.level().getBiome(new BlockPos(cp.x * 16 + 8, sampleY, cp.z * 16 + 8));
                    return matchesBiome(h, biomesForFilter);
                };

        TerritoryManager.ClaimResult result = tm.claimIsland(raceId, centerChunk, player.getUUID(), biomeFilter);
        if ((result.type == TerritoryManager.ClaimResultType.SUCCESS
                || result.type == TerritoryManager.ClaimResultType.INSIDE_OWN_TERRITORY)
                && !result.newClaims.isEmpty()) {
            // Anchor only the chunks this placement actually created — pre-existing claims from
            // allies or earlier placements are not tracked here and survive tree removal untouched.
            net.minecraft.core.BlockPos anchorPos = interact_pos != null ? interact_pos.below() : player.blockPosition();
            tm.placeRootBlock(anchorPos, result.newClaims);
        }
        return switch (result.type) {
            case SUCCESS              -> runBranch(onSuccess, player, target, slot, interact_pos);
            case PARTIAL              -> runBranch(onPartial, player, target, slot, interact_pos);
            case ENEMY_TERRITORY      -> runBranch(onEnemyTerritory, player, target, slot, interact_pos);
            case INSIDE_OWN_TERRITORY -> runBranch(onInsideOwn, player, target, slot, interact_pos);
            case INVALID_BIOME        -> runBranch(onInvalidBiome, player, target, slot, interact_pos);
            case INSUFFICIENT_COINS, ANCHOR_CHUNK, UNCLAIM_SUCCESS, OUT_OF_RANGE, NOT_LEADER -> false;
        };
    }

    /**
     * Gate check: is the target position in a valid biome?
     * Uses interactPos when provided (block-place context), otherwise the player's own position.
     * Spirit-realm players always check their stored overworld node position.
     */
    private boolean isCenterBiomeValid(ServerPlayer player, @javax.annotation.Nullable BlockPos interactPos,
            List<String> biomes) {
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> holder;
        if (player.level().dimension().location().equals(SPIRIT_REALM)) {
            var vars = DataUtils.getVariables(player).orElse(null);
            if (vars == null) return true;
            double nx = vars.getPersistentState(NODE_X);
            double ny = vars.getPersistentState(NODE_Y);
            double nz = vars.getPersistentState(NODE_Z);
            if (nx == 0 && ny == 0 && nz == 0) return true;
            net.minecraft.server.level.ServerLevel overworld =
                    player.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (overworld == null) return true;
            holder = overworld.getBiome(new BlockPos((int) nx, (int) ny, (int) nz));
        } else if (interactPos != null) {
            @SuppressWarnings("null")
            net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> h =
                    player.level().getBiome(interactPos);
            holder = h;
        } else {
            @SuppressWarnings("null")
            net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> h =
                    player.level().getBiome(player.blockPosition());
            holder = h;
        }
        return matchesBiome(holder, biomes);
    }

    private boolean matchesBiome(net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> holder,
            List<String> biomes) {
        for (String entry : biomes) {
            if (entry.startsWith("#")) {
                try {
                    @SuppressWarnings("null")
                    net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome> tagKey =
                            net.minecraft.tags.TagKey.create(
                                    net.minecraft.core.registries.Registries.BIOME,
                                    new ResourceLocation(entry.substring(1)));
                    if (holder.is(tagKey)) return true;
                } catch (Exception ignored) {}
            } else {
                if (holder.unwrapKey().map(k -> k.location().toString().equals(entry)).orElse(false)) {
                    return true;
                }
            }
        }
        return false;
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

            return new ClaimTerritoryAction(
                    validBiomes,
                    parseList(json, "on_success"),
                    parseList(json, "on_partial"),
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
