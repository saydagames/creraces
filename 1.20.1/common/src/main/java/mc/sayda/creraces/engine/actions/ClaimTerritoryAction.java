package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.territory.ClaimData;
import mc.sayda.creraces.territory.FactionData;
import mc.sayda.creraces.territory.FactionRank;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClaimTerritoryAction implements ActionRegistry.RaceAction {

    public static final ResourceLocation ID = new ResourceLocation("creraces", "claim_territory");

    private record PendingClaim(List<ActionRegistry.RaceAction> onSuccess,
                                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
                                @javax.annotation.Nullable BlockPos interactPos) {}

    private static final java.util.concurrent.ConcurrentHashMap<UUID, PendingClaim> PENDING = new java.util.concurrent.ConcurrentHashMap<>();

    /** Called by FactionActionPacket after faction is confirmed to execute the deferred on_success branch. */
    public static void resumePending(ServerPlayer player) {
        PendingClaim pending = PENDING.remove(player.getUUID());
        if (pending == null) return;
        for (ActionRegistry.RaceAction action : pending.onSuccess()) {
            action.execute(player, pending.target(), pending.slot(), pending.interactPos());
        }
    }

    /** Called on PLAYER_QUIT to discard any stale pending claim for this player. */
    public static void clearPending(java.util.UUID playerUUID) {
        PENDING.remove(playerUUID);
    }

    private final List<String> validBiomes;
    private final List<ActionRegistry.RaceAction> onSuccess;
    private final List<ActionRegistry.RaceAction> onPartial;
    private final List<ActionRegistry.RaceAction> onInvalidBiome;
    private final List<ActionRegistry.RaceAction> onEnemyTerritory;
    private final List<ActionRegistry.RaceAction> onInsideOwn;
    private final List<ActionRegistry.RaceAction> onInvalidRank;

    public ClaimTerritoryAction(List<String> validBiomes,
            List<ActionRegistry.RaceAction> onSuccess,
            List<ActionRegistry.RaceAction> onPartial,
            List<ActionRegistry.RaceAction> onInvalidBiome,
            List<ActionRegistry.RaceAction> onEnemyTerritory,
            List<ActionRegistry.RaceAction> onInsideOwn,
            List<ActionRegistry.RaceAction> onInvalidRank) {
        this.validBiomes = validBiomes;
        this.onSuccess = onSuccess;
        this.onPartial = onPartial;
        this.onInvalidBiome = onInvalidBiome;
        this.onEnemyTerritory = onEnemyTerritory;
        this.onInsideOwn = onInsideOwn;
        this.onInvalidRank = onInvalidRank;
    }

    private static final ResourceLocation SPIRIT_REALM = new ResourceLocation("creraces", "spirit_realm");
    private static final ResourceLocation TX = new ResourceLocation("creraces", "tx");
    private static final ResourceLocation TY = new ResourceLocation("creraces", "ty");
    private static final ResourceLocation TZ = new ResourceLocation("creraces", "tz");

    @Override
    public boolean execute(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable BlockPos interact_pos) {

        if (!(player instanceof ServerPlayer serverPlayer)) return true;

        if (!validBiomes.isEmpty() && !isBiomeValid(serverPlayer)) {
            return runBranch(onInvalidBiome, player, target, slot, interact_pos);
        }

        TerritoryManager tm = TerritoryManager.get();
        ChunkPos playerChunk = new ChunkPos(player.blockPosition());
        ClaimData existingClaim = tm.getClaimAt(playerChunk);
        boolean hasFaction = tm.hasFaction(player.getUUID());

        if (!hasFaction) {
            if (existingClaim != null) {
                // Inside another faction's territory: notify their officers and run the enemy branch.
                notifyFactionOfficers(serverPlayer, existingClaim.getFactionId(), tm);
                serverPlayer.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "creraces.territory.anchor.join_request_sent"),
                        true);
                return runBranch(onEnemyTerritory, player, target, slot, interact_pos);
            }
            // Outside all territory: store on_success for deferred execution after the player
            // names and confirms their faction. Do NOT run onPartial here — it would fire costs
            // (cooldowns, resource drain) before the player has committed to anything.
            PENDING.put(player.getUUID(), new PendingClaim(onSuccess, target, slot, interact_pos));
            BoundaryHandler.openFactionCreate(serverPlayer);
            return true;
        }

        // Player has a faction
        UUID factionId = tm.getFactionId(player.getUUID());
        FactionData faction = tm.getFaction(factionId);
        if (faction == null) return true;

        if (existingClaim != null && existingClaim.getFactionId().equals(factionId)) {
            // Inside own territory: place as dormant node
            return runBranch(onInsideOwn, player, target, slot, interact_pos);
        }

        if (existingClaim != null) {
            // Inside another faction's claimed area
            return runBranch(onEnemyTerritory, player, target, slot, interact_pos);
        }

        // Outside territory: expansion — officer+ only
        FactionRank rank = faction.getRank(player.getUUID());
        if (rank == null || !rank.isAtLeast(FactionRank.OFFICER)) {
            return runBranch(onInvalidRank, player, target, slot, interact_pos);
        }

        TerritoryManager.ClaimResult result = tm.claimAdjacentChunk(factionId, playerChunk, rank, player.getUUID());
        return switch (result.type) {
            case SUCCESS          -> runBranch(onSuccess, player, target, slot, interact_pos);
            case PARTIAL          -> runBranch(onPartial, player, target, slot, interact_pos);
            case ENEMY_TERRITORY  -> runBranch(onEnemyTerritory, player, target, slot, interact_pos);
            case INSIDE_OWN_TERRITORY -> runBranch(onInsideOwn, player, target, slot, interact_pos);
            default               -> true;
        };
    }

    private boolean isBiomeValid(ServerPlayer player) {
        // When in the spirit realm the player's position is not overworld — check the stored anchor instead
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> holder;
        if (player.level().dimension().location().equals(SPIRIT_REALM)) {
            var vars = mc.sayda.creraces.capability.DataUtils.getVariables(player).orElse(null);
            if (vars == null) return true; // can't validate, allow through
            double tx = vars.getPersistentState(TX);
            double ty = vars.getPersistentState(TY);
            double tz = vars.getPersistentState(TZ);
            if (tx == 0 && ty == 0 && tz == 0) return true; // no anchor yet, allow through
            net.minecraft.server.level.ServerLevel overworld =
                    player.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (overworld == null) return true;
            holder = overworld.getBiome(new BlockPos((int) tx, (int) ty, (int) tz));
        } else {
            @SuppressWarnings("null")
            net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> h =
                    player.level().getBiome(player.blockPosition());
            holder = h;
        }
        for (String entry : validBiomes) {
            if (entry.startsWith("#")) {
                String tagStr = entry.substring(1);
                try {
                    @SuppressWarnings("null")
                    net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome> tagKey =
                            net.minecraft.tags.TagKey.create(
                                    net.minecraft.core.registries.Registries.BIOME,
                                    new ResourceLocation(tagStr));
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

    private static void notifyFactionOfficers(ServerPlayer applicant, UUID targetFactionId, TerritoryManager tm) {
        mc.sayda.creraces.territory.FactionData faction = tm.getFaction(targetFactionId);
        if (faction == null) return;
        var pkt = new mc.sayda.creraces.network.JoinRequestNotifyPacket(
                applicant.getUUID(),
                applicant.getName().getString(),
                mc.sayda.creraces.capability.DataUtils.getVariables(applicant)
                        .map(mc.sayda.creraces.capability.IPlayerVariables::getRace)
                        .orElse(new ResourceLocation("creraces", "none")));
        for (java.util.Map.Entry<UUID, mc.sayda.creraces.territory.FactionRank> e : faction.getMembers().entrySet()) {
            if (!e.getValue().isAtLeast(mc.sayda.creraces.territory.FactionRank.OFFICER)) continue;
            ServerPlayer officer = applicant.getServer().getPlayerList().getPlayer(e.getKey());
            if (officer != null) mc.sayda.creraces.network.BoundaryHandler.sendJoinRequestNotify(officer, pkt);
        }
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
                    parseList(json, "on_inside_own"),
                    parseList(json, "on_invalid_rank"));
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
