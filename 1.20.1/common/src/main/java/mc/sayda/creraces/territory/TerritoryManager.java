package mc.sayda.creraces.territory;

import com.google.gson.*;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TerritoryManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SAVE_FILE = "creraces_territory.json";
    // ── Singleton ──────────────────────────────────────────────────────────────
    private static TerritoryManager INSTANCE;

    public static TerritoryManager get() {
        if (INSTANCE == null) INSTANCE = new TerritoryManager();
        return INSTANCE;
    }

    // ── Data ───────────────────────────────────────────────────────────────────
    private final Map<Long,  ClaimData>          claims        = new HashMap<>();
    private final Map<UUID,  FactionData>        factions      = new HashMap<>();
    private final Map<UUID,  ClanData>           clans         = new HashMap<>();
    private final Map<UUID,  UUID>               playerFaction = new HashMap<>();
    private final Map<UUID,  UUID>               playerClan    = new HashMap<>();
    private final Map<UUID,  PlayerActivityData> activity      = new HashMap<>();

    // ── Succession tick counter ────────────────────────────────────────────────
    private int successionTickCounter = 0;

    // ── Territory notification tracking (transient, not saved) ────────────────
    private final Map<UUID, Long> lastChunkKeys = new HashMap<>();

    // ── ClaimResult ────────────────────────────────────────────────────────────
    public enum ClaimResultType {
        SUCCESS, PARTIAL, INVALID_BIOME, ENEMY_TERRITORY, INSIDE_OWN_TERRITORY, INVALID_RANK
    }

    public static final class ClaimResult {
        public final ClaimResultType type;
        public final int claimed;
        public final int truncated;

        private ClaimResult(ClaimResultType type, int claimed, int truncated) {
            this.type = type;
            this.claimed = claimed;
            this.truncated = truncated;
        }

        public static ClaimResult success()                        { return new ClaimResult(ClaimResultType.SUCCESS, 1, 0); }
        public static ClaimResult partial(int ok, int skipped)     { return new ClaimResult(ClaimResultType.PARTIAL, ok, skipped); }
        public static ClaimResult invalidBiome()                   { return new ClaimResult(ClaimResultType.INVALID_BIOME, 0, 0); }
        public static ClaimResult enemyTerritory()                 { return new ClaimResult(ClaimResultType.ENEMY_TERRITORY, 0, 0); }
        public static ClaimResult insideOwn()                      { return new ClaimResult(ClaimResultType.INSIDE_OWN_TERRITORY, 0, 0); }
        public static ClaimResult invalidRank()                    { return new ClaimResult(ClaimResultType.INVALID_RANK, 0, 0); }
    }

    // ── Accessors ──────────────────────────────────────────────────────────────
    public Map<Long, ClaimData>   getClaims()        { return Collections.unmodifiableMap(claims); }
    public Map<UUID, FactionData> getFactions()      { return Collections.unmodifiableMap(factions); }
    public Map<UUID, ClanData>    getClans()         { return Collections.unmodifiableMap(clans); }

    public UUID    getFactionId(UUID playerUUID)     { return playerFaction.get(playerUUID); }
    public UUID    getClanId(UUID playerUUID)        { return playerClan.get(playerUUID); }
    public boolean hasFaction(UUID playerUUID)       { return playerFaction.containsKey(playerUUID); }
    public FactionData getFaction(UUID factionId)    { return factions.get(factionId); }
    public FactionData getFactionOf(UUID playerUUID) { return factions.get(playerFaction.get(playerUUID)); }
    public ClaimData   getClaimAt(long chunkKey)     { return claims.get(chunkKey); }
    public ClaimData   getClaimAt(ChunkPos pos)      { return claims.get(pos.toLong()); }

    // ── Faction Management ─────────────────────────────────────────────────────

    public UUID createFaction(MinecraftServer server, ServerPlayer leader, String name,
                               ResourceLocation raceId, BlockPos anchorPos) {
        UUID fId = UUID.randomUUID();
        FactionData faction = new FactionData(fId, raceId, name);
        faction.getMembers().put(leader.getUUID(), FactionRank.LEADER);
        factions.put(fId, faction);
        playerFaction.put(leader.getUUID(), fId);
        updateActivity(leader.getUUID(), System.currentTimeMillis());

        // Claim initial grid around anchor
        int radius = CreRacesConfig.TERRITORY_DEFAULT_CLAIM_RADIUS.get();
        claimInitialGrid(fId, raceId, anchorPos, radius, leader.getUUID());

        // Register the founding biome so in_habitable_biome fires anywhere in that biome type
        if (anchorPos != null) {
            leader.serverLevel()
                    .getBiome(anchorPos)
                    .unwrapKey()
                    .ifPresent(key -> faction.getHabitableBiomes().add(key.location()));
        }

        mc.sayda.creraces.network.BoundaryHandler.resyncForAllTrackers(leader);
        mc.sayda.creraces.network.BoundaryHandler.resyncVariables(leader, leader);

        CreRaces.LOGGER.info("Faction '{}' ({}) created by {}", name, fId, leader.getName().getString());
        return fId;
    }

    private void claimInitialGrid(UUID factionId, ResourceLocation raceId, BlockPos anchorPos, int radius, UUID ownerUUID) {
        ChunkPos center = new ChunkPos(anchorPos);
        FactionData faction = factions.get(factionId);
        if (faction == null) return;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos cp = new ChunkPos(center.x + dx, center.z + dz);
                long key = cp.toLong();
                ClaimData existing = claims.get(key);
                if (existing != null) continue;
                ClaimData cd = new ClaimData(key, factionId, raceId, false, true, ownerUUID);
                claims.put(key, cd);
                faction.getClaimedChunks().add(key);
            }
        }
    }

    public void disbandFaction(MinecraftServer server, UUID factionId) {
        FactionData faction = factions.get(factionId);
        if (faction == null) return;

        // Remove all chunk claims
        for (long key : faction.getClaimedChunks()) {
            claims.remove(key);
        }

        // Evict all members
        for (UUID memberId : new ArrayList<>(faction.getMembers().keySet())) {
            evictMember(server, factionId, memberId);
        }

        // Remove from parent clan
        if (faction.getClanId() != null) {
            ClanData clan = clans.get(faction.getClanId());
            if (clan != null) clan.getMemberFactionIds().remove(factionId);
        }

        factions.remove(factionId);
        CreRaces.LOGGER.info("Faction {} disbanded", factionId);
    }

    public boolean addMember(UUID factionId, UUID playerUUID, FactionRank rank) {
        FactionData faction = factions.get(factionId);
        if (faction == null) return false;
        faction.getMembers().put(playerUUID, rank);
        playerFaction.put(playerUUID, factionId);
        updateActivity(playerUUID, System.currentTimeMillis());
        if (faction.getClanId() != null) playerClan.put(playerUUID, faction.getClanId());
        return true;
    }

    public void removeMember(MinecraftServer server, UUID factionId, UUID playerUUID, boolean wasKicked) {
        FactionData faction = factions.get(factionId);
        if (faction == null) return;

        faction.getMembers().remove(playerUUID);
        playerFaction.remove(playerUUID);
        playerClan.remove(playerUUID);

        ServerPlayer online = server.getPlayerList().getPlayer(playerUUID);
        if (online != null) {
            mc.sayda.creraces.network.BoundaryHandler.resyncForAllTrackers(online);
            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(online, online);
        }

        // If leader left, trigger succession immediately
        if (faction.getLeader() == null && !faction.getMembers().isEmpty()) {
            doSuccession(server, factionId, faction);
        }
    }

    /**
     * Player leaves their faction, moving their personally-owned chunks into a brand-new faction.
     * Returns the new faction's UUID, or null on failure.
     */
    public UUID leaveSplit(MinecraftServer server, UUID factionId, UUID playerUUID) {
        FactionData oldFaction = factions.get(factionId);
        if (oldFaction == null) return null;

        // Collect chunks owned by this player
        List<Long> myChunks = new ArrayList<>();
        for (Map.Entry<Long, ClaimData> e : claims.entrySet()) {
            ClaimData cd = e.getValue();
            if (cd.getFactionId().equals(factionId) && playerUUID.equals(cd.getOwnerUUID())) {
                myChunks.add(e.getKey());
            }
        }

        // Create new faction
        String newName = oldFaction.getName() + " (Split)";
        UUID newFactionId = UUID.randomUUID();
        FactionData newFaction = new FactionData(newFactionId, oldFaction.getRaceId(), newName);
        newFaction.getMembers().put(playerUUID, FactionRank.LEADER);
        newFaction.getHabitableBiomes().addAll(oldFaction.getHabitableBiomes());
        if (oldFaction.getClanId() != null) {
            newFaction.setClanId(oldFaction.getClanId());
            ClanData clan = clans.get(oldFaction.getClanId());
            if (clan != null) clan.getMemberFactionIds().add(newFactionId);
        }
        factions.put(newFactionId, newFaction);

        // Move chunks to new faction
        for (long key : myChunks) {
            ClaimData old = claims.get(key);
            claims.put(key, new ClaimData(key, newFactionId, old.getRaceId(), old.isDormant(), old.isPersistent(), playerUUID));
            oldFaction.getClaimedChunks().remove(key);
            newFaction.getClaimedChunks().add(key);
        }

        // Move player to new faction
        oldFaction.getMembers().remove(playerUUID);
        playerFaction.put(playerUUID, newFactionId);
        if (oldFaction.getClanId() != null) playerClan.put(playerUUID, oldFaction.getClanId());
        updateActivity(playerUUID, System.currentTimeMillis());

        ServerPlayer online = server.getPlayerList().getPlayer(playerUUID);
        if (online != null) {
            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(online, online);
            mc.sayda.creraces.network.BoundaryHandler.resyncForAllTrackers(online);
        }

        // Succession for old faction if its leader just left
        if (oldFaction.getLeader() == null && !oldFaction.getMembers().isEmpty()) {
            doSuccession(server, factionId, oldFaction);
        } else if (oldFaction.getMembers().isEmpty()) {
            // Old faction is now empty — clean it up
            for (long key : new ArrayList<>(oldFaction.getClaimedChunks())) claims.remove(key);
            factions.remove(factionId);
        }

        CreRaces.LOGGER.info("Player {} split from faction '{}', new faction '{}'",
                playerUUID, oldFaction.getName(), newName);
        return newFactionId;
    }

    /**
     * Player disbands their personally-owned chunks and leaves the faction (returns to spirit form).
     */
    public void leaveDisband(MinecraftServer server, UUID factionId, UUID playerUUID) {
        FactionData faction = factions.get(factionId);
        if (faction == null) return;

        // Remove chunks owned by this player
        List<Long> myChunks = new ArrayList<>();
        for (Map.Entry<Long, ClaimData> e : claims.entrySet()) {
            ClaimData cd = e.getValue();
            if (cd.getFactionId().equals(factionId) && playerUUID.equals(cd.getOwnerUUID())) {
                myChunks.add(e.getKey());
            }
        }
        for (long key : myChunks) {
            claims.remove(key);
            faction.getClaimedChunks().remove(key);
        }

        // Remove from faction (sets spirit state)
        removeMember(server, factionId, playerUUID, false);

        // If faction is now empty, clean it up
        if (faction.getMembers().isEmpty()) {
            for (long key : new ArrayList<>(faction.getClaimedChunks())) claims.remove(key);
            factions.remove(factionId);
        }
    }

    // evictMember is the internal path (disband/forced) — skips succession
    private void evictMember(MinecraftServer server, UUID factionId, UUID playerUUID) {
        FactionData faction = factions.get(factionId);
        if (faction != null) faction.getMembers().remove(playerUUID);
        playerFaction.remove(playerUUID);
        playerClan.remove(playerUUID);

        ServerPlayer online = server.getPlayerList().getPlayer(playerUUID);
        if (online != null) {
            mc.sayda.creraces.network.BoundaryHandler.resyncForAllTrackers(online);
            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(online, online);
        }
    }

    public boolean setRank(UUID factionId, UUID targetUUID, FactionRank newRank,
                           FactionRank requesterRank) {
        FactionData faction = factions.get(factionId);
        if (faction == null) return false;
        FactionRank targetCurrent = faction.getRank(targetUUID);
        if (targetCurrent == null) return false;
        // Requester must outrank the target to modify them
        if (!requesterRank.isAtLeast(FactionRank.OFFICER)) return false;
        if (targetCurrent == FactionRank.LEADER && requesterRank != FactionRank.LEADER) return false;
        // Promoting someone to LEADER demotes current leader to OFFICER
        if (newRank == FactionRank.LEADER) {
            UUID currentLeader = faction.getLeader();
            if (currentLeader != null) faction.getMembers().put(currentLeader, FactionRank.OFFICER);
        }
        faction.getMembers().put(targetUUID, newRank);
        return true;
    }

    // ── Claim Operations ───────────────────────────────────────────────────────

    public ClaimResult claimAdjacentChunk(UUID factionId, ChunkPos chunk, FactionRank requesterRank, UUID claimerUUID) {
        if (!requesterRank.isAtLeast(FactionRank.OFFICER)) return ClaimResult.invalidRank();
        FactionData faction = factions.get(factionId);
        if (faction == null) return ClaimResult.enemyTerritory();

        long key = chunk.toLong();
        ClaimData existing = claims.get(key);
        if (existing != null) {
            if (existing.getFactionId().equals(factionId)) return ClaimResult.insideOwn();
            return ClaimResult.enemyTerritory();
        }

        // Must be adjacent to an owned chunk
        if (!isAdjacentToFaction(factionId, chunk)) return ClaimResult.enemyTerritory();

        ResourceLocation raceId = faction.getRaceId();
        ClaimData cd = new ClaimData(key, factionId, raceId, false, false, claimerUUID);
        claims.put(key, cd);
        faction.getClaimedChunks().add(key);
        return ClaimResult.success();
    }

    public boolean unclaimChunk(UUID factionId, ChunkPos chunk, FactionRank requesterRank) {
        if (!requesterRank.isAtLeast(FactionRank.OFFICER)) return false;
        FactionData faction = factions.get(factionId);
        if (faction == null) return false;
        long key = chunk.toLong();
        ClaimData cd = claims.get(key);
        if (cd == null || !cd.getFactionId().equals(factionId)) return false;
        if (cd.isPersistent()) return false; // anchor territory cannot be unclaimed
        claims.remove(key);
        faction.getClaimedChunks().remove(key);
        return true;
    }

    public boolean whitelistDormantNode(UUID factionId, long chunkKey, FactionRank requesterRank) {
        if (!requesterRank.isAtLeast(FactionRank.OFFICER)) return false;
        ClaimData cd = claims.get(chunkKey);
        if (cd == null || !cd.getFactionId().equals(factionId) || !cd.isDormant()) return false;
        cd.setDormant(false);
        return true;
    }

    private boolean isAdjacentToFaction(UUID factionId, ChunkPos chunk) {
        int[] dx = {-1, 1, 0, 0};
        int[] dz = {0, 0, -1, 1};
        for (int i = 0; i < 4; i++) {
            long neighborKey = new ChunkPos(chunk.x + dx[i], chunk.z + dz[i]).toLong();
            ClaimData neighbor = claims.get(neighborKey);
            if (neighbor != null && neighbor.getFactionId().equals(factionId)) return true;
        }
        return false;
    }

    // ── Biome Helpers ──────────────────────────────────────────────────────────

    public void addHabitableBiome(UUID factionId, ResourceLocation biomeId) {
        FactionData faction = factions.get(factionId);
        if (faction != null) faction.getHabitableBiomes().add(biomeId);
    }

    public boolean isHabitable(UUID factionId, ResourceLocation biomeId) {
        FactionData faction = factions.get(factionId);
        if (faction == null) return false;
        return faction.getHabitableBiomes().contains(biomeId);
    }

    // ── Activity Tracking ──────────────────────────────────────────────────────

    public void updateActivity(UUID playerUUID, long timeMs) {
        PlayerActivityData data = activity.get(playerUUID);
        if (data == null) {
            activity.put(playerUUID, new PlayerActivityData(playerUUID, timeMs));
        } else {
            data.setLastSeenMs(timeMs);
        }
    }

    // ── Succession ─────────────────────────────────────────────────────────────

    public void tick(MinecraftServer server) {
        tickTerritoryNotifications(server);

        int interval = CreRacesConfig.TERRITORY_SUCCESSION_TICK_INTERVAL.get();
        if (++successionTickCounter < interval) return;
        successionTickCounter = 0;
        tickSuccession(server);
    }

    public void clearPlayerTracking(UUID playerUUID) {
        lastChunkKeys.remove(playerUUID);
    }

    private void tickTerritoryNotifications(MinecraftServer server) {
        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            long currentKey = player.chunkPosition().toLong();
            Long lastKey = lastChunkKeys.put(playerId, currentKey);

            if (lastKey == null || lastKey == currentKey) continue;

            UUID newFid = claims.containsKey(currentKey) ? claims.get(currentKey).getFactionId() : null;
            UUID oldFid = claims.containsKey(lastKey)    ? claims.get(lastKey).getFactionId()    : null;

            if (java.util.Objects.equals(newFid, oldFid)) continue;

            UUID myFid = playerFaction.get(playerId);
            net.minecraft.network.chat.MutableComponent msg = null;

            if (newFid != null) {
                FactionData nf = factions.get(newFid);
                String name = nf != null ? nf.getName() : "Unknown";
                if (newFid.equals(myFid)) {
                    msg = net.minecraft.network.chat.Component.literal("§aEntered §f" + name);
                } else if (isAlliedFaction(myFid, newFid)) {
                    msg = net.minecraft.network.chat.Component.literal("§9Entered §f" + name);
                } else if (myFid != null) {
                    msg = net.minecraft.network.chat.Component.literal("§cEntered §f" + name);
                }
            } else if (oldFid != null && oldFid.equals(myFid)) {
                FactionData of = factions.get(oldFid);
                String name = of != null ? of.getName() : "Unknown";
                msg = net.minecraft.network.chat.Component.literal("§7Left §f" + name);
            }

            if (msg != null) player.displayClientMessage(msg, true);
        }
    }

    private boolean isAlliedFaction(UUID myFid, UUID otherFid) {
        if (myFid == null || otherFid == null) return false;
        FactionData mine = factions.get(myFid);
        FactionData other = factions.get(otherFid);
        if (mine == null || other == null) return false;
        return mine.getClanId() != null && mine.getClanId().equals(other.getClanId());
    }

    private void tickSuccession(MinecraftServer server) {
        long thresholdDays = CreRacesConfig.TERRITORY_LEADER_DECAY_THRESHOLD_DAYS.get();
        if (thresholdDays < 0) return;
        long eligibilityDays = CreRacesConfig.TERRITORY_SUCCESSION_WINDOW_DAYS.get();
        long now = System.currentTimeMillis();
        long thresholdMs = TimeUnit.DAYS.toMillis(thresholdDays);
        long eligibilityMs = TimeUnit.DAYS.toMillis(eligibilityDays);

        for (Map.Entry<UUID, FactionData> entry : factions.entrySet()) {
            UUID factionId = entry.getKey();
            FactionData faction = entry.getValue();
            UUID leaderId = faction.getLeader();
            if (leaderId == null) continue;

            PlayerActivityData leaderActivity = activity.get(leaderId);
            long leaderLastSeen = leaderActivity != null ? leaderActivity.getLastSeenMs() : 0L;
            if (now - leaderLastSeen < thresholdMs) continue;

            doSuccession(server, factionId, faction);
        }
    }

    private void doSuccession(MinecraftServer server, UUID factionId, FactionData faction) {
        long eligibilityDays = CreRacesConfig.TERRITORY_SUCCESSION_WINDOW_DAYS.get();
        long now = System.currentTimeMillis();
        long eligibilityMs = TimeUnit.DAYS.toMillis(eligibilityDays);

        // Find best candidate: officers first, then members; most recently seen wins
        UUID best = null;
        FactionRank bestRank = null;
        long bestSeen = -1L;

        for (Map.Entry<UUID, FactionRank> e : faction.getMembers().entrySet()) {
            if (e.getValue() == FactionRank.LEADER) continue;
            PlayerActivityData pData = activity.get(e.getKey());
            long lastSeen = pData != null ? pData.getLastSeenMs() : 0L;
            if (now - lastSeen > eligibilityMs) continue;

            // Strict ordinal comparison so same-rank ties fall through to sameRankButNewer
            // rather than always picking the last entry in iteration order.
            boolean betterRank = bestRank == null || e.getValue().ordinal() > bestRank.ordinal();
            boolean sameRankButNewer = bestRank != null && e.getValue() == bestRank && lastSeen > bestSeen;
            if (betterRank || sameRankButNewer) {
                best = e.getKey();
                bestRank = e.getValue();
                bestSeen = lastSeen;
            }
        }

        if (best == null) return;
        forceTransferLeadership(server, factionId, best);
    }

    public void forceTransferLeadership(MinecraftServer server, UUID factionId, UUID newLeader) {
        FactionData faction = factions.get(factionId);
        if (faction == null || !faction.getMembers().containsKey(newLeader)) return;

        UUID oldLeader = faction.getLeader();
        if (oldLeader != null) faction.getMembers().put(oldLeader, FactionRank.OFFICER);
        faction.getMembers().put(newLeader, FactionRank.LEADER);

        ServerPlayer online = server.getPlayerList().getPlayer(newLeader);
        if (online != null) {
            online.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "creraces.territory.succession.promoted", faction.getName()));
        }

        CreRaces.LOGGER.info("Faction '{}': leadership transferred from {} to {}", faction.getName(), oldLeader, newLeader);
    }

    // ── Clan Management ────────────────────────────────────────────────────────

    public UUID createClan(ResourceLocation raceId, UUID leaderId) {
        UUID clanId = UUID.randomUUID();
        ClanData clan = new ClanData(clanId, raceId, leaderId);
        clans.put(clanId, clan);
        playerClan.put(leaderId, clanId);
        UUID fId = playerFaction.get(leaderId);
        if (fId != null) {
            clan.getMemberFactionIds().add(fId);
            FactionData f = factions.get(fId);
            if (f != null) f.setClanId(clanId);
        }
        return clanId;
    }

    public boolean addFactionToClan(UUID clanId, UUID factionId) {
        ClanData clan = clans.get(clanId);
        FactionData faction = factions.get(factionId);
        if (clan == null || faction == null) return false;
        if (faction.getClanId() != null) return false;
        clan.getMemberFactionIds().add(factionId);
        faction.setClanId(clanId);
        for (UUID memberId : faction.getMembers().keySet()) {
            playerClan.put(memberId, clanId);
        }
        return true;
    }

    public void removeFactionFromClan(UUID clanId, UUID factionId) {
        ClanData clan = clans.get(clanId);
        FactionData faction = factions.get(factionId);
        if (clan == null || faction == null) return;
        clan.getMemberFactionIds().remove(factionId);
        faction.setClanId(null);
        for (UUID memberId : faction.getMembers().keySet()) {
            playerClan.remove(memberId);
        }
    }

    public void disbandClan(MinecraftServer server, UUID clanId) {
        ClanData clan = clans.get(clanId);
        if (clan == null) return;
        for (UUID fId : new ArrayList<>(clan.getMemberFactionIds())) {
            FactionData f = factions.get(fId);
            if (f != null) {
                f.setClanId(null);
                for (UUID memberId : f.getMembers().keySet()) {
                    playerClan.remove(memberId);
                }
            }
        }
        clans.remove(clanId);
    }

    // ── Persistence ────────────────────────────────────────────────────────────

    public static void save(MinecraftServer server) {
        Path path = server.getWorldPath(Objects.requireNonNull(LevelResource.ROOT)).resolve(SAVE_FILE);
        JsonObject root = new JsonObject();
        TerritoryManager tm = get();

        // claims
        JsonObject claimsJson = new JsonObject();
        for (Map.Entry<Long, ClaimData> e : tm.claims.entrySet()) {
            claimsJson.add(String.valueOf(e.getKey()), serializeClaim(e.getValue()));
        }
        root.add("claims", claimsJson);

        // factions
        JsonObject factionsJson = new JsonObject();
        for (Map.Entry<UUID, FactionData> e : tm.factions.entrySet()) {
            factionsJson.add(e.getKey().toString(), serializeFaction(e.getValue()));
        }
        root.add("factions", factionsJson);

        // clans
        JsonObject clansJson = new JsonObject();
        for (Map.Entry<UUID, ClanData> e : tm.clans.entrySet()) {
            clansJson.add(e.getKey().toString(), serializeClan(e.getValue()));
        }
        root.add("clans", clansJson);

        // player_faction
        JsonObject pfJson = new JsonObject();
        for (Map.Entry<UUID, UUID> e : tm.playerFaction.entrySet()) {
            pfJson.addProperty(e.getKey().toString(), e.getValue().toString());
        }
        root.add("player_faction", pfJson);

        // player_clan
        JsonObject pcJson = new JsonObject();
        for (Map.Entry<UUID, UUID> e : tm.playerClan.entrySet()) {
            pcJson.addProperty(e.getKey().toString(), e.getValue().toString());
        }
        root.add("player_clan", pcJson);

        // activity
        JsonObject actJson = new JsonObject();
        for (Map.Entry<UUID, PlayerActivityData> e : tm.activity.entrySet()) {
            actJson.addProperty(e.getKey().toString(), e.getValue().getLastSeenMs());
        }
        root.add("activity", actJson);

        try (Writer w = Files.newBufferedWriter(path)) {
            GSON.toJson(root, w);
        } catch (IOException ex) {
            CreRaces.LOGGER.error("Failed to save territory data: {}", ex.getMessage());
        }
    }

    public static void load(MinecraftServer server) {
        // Build into a local instance; only assign to INSTANCE on success so a
        // corrupt file never leaves a blank singleton as the active manager.
        TerritoryManager fresh = new TerritoryManager();
        Path path = server.getWorldPath(Objects.requireNonNull(LevelResource.ROOT)).resolve(SAVE_FILE);
        if (!Files.exists(path)) {
            INSTANCE = fresh;
            return;
        }

        try (Reader r = Files.newBufferedReader(path)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) {
                INSTANCE = fresh;
                return;
            }
            TerritoryManager tm = fresh;

            if (root.has("claims")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("claims").entrySet()) {
                    try {
                        long key = Long.parseLong(e.getKey());
                        tm.claims.put(key, deserializeClaim(key, e.getValue().getAsJsonObject()));
                    } catch (Exception ex) {
                        CreRaces.LOGGER.warn("Skipping malformed claim entry '{}': {}", e.getKey(), ex.getMessage());
                    }
                }
            }

            if (root.has("factions")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("factions").entrySet()) {
                    try {
                        UUID id = UUID.fromString(e.getKey());
                        tm.factions.put(id, deserializeFaction(id, e.getValue().getAsJsonObject()));
                    } catch (Exception ex) {
                        CreRaces.LOGGER.warn("Skipping malformed faction entry '{}': {}", e.getKey(), ex.getMessage());
                    }
                }
            }

            if (root.has("clans")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("clans").entrySet()) {
                    try {
                        UUID id = UUID.fromString(e.getKey());
                        tm.clans.put(id, deserializeClan(id, e.getValue().getAsJsonObject()));
                    } catch (Exception ex) {
                        CreRaces.LOGGER.warn("Skipping malformed clan entry '{}': {}", e.getKey(), ex.getMessage());
                    }
                }
            }

            if (root.has("player_faction")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("player_faction").entrySet()) {
                    try {
                        tm.playerFaction.put(UUID.fromString(e.getKey()),
                                UUID.fromString(e.getValue().getAsString()));
                    } catch (Exception ex) {
                        CreRaces.LOGGER.warn("Skipping malformed player_faction entry '{}': {}", e.getKey(), ex.getMessage());
                    }
                }
            }

            if (root.has("player_clan")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("player_clan").entrySet()) {
                    try {
                        tm.playerClan.put(UUID.fromString(e.getKey()),
                                UUID.fromString(e.getValue().getAsString()));
                    } catch (Exception ex) {
                        CreRaces.LOGGER.warn("Skipping malformed player_clan entry '{}': {}", e.getKey(), ex.getMessage());
                    }
                }
            }

            if (root.has("activity")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("activity").entrySet()) {
                    try {
                        UUID id = UUID.fromString(e.getKey());
                        tm.activity.put(id, new PlayerActivityData(id, e.getValue().getAsLong()));
                    } catch (Exception ex) {
                        CreRaces.LOGGER.warn("Skipping malformed activity entry '{}': {}", e.getKey(), ex.getMessage());
                    }
                }
            }

            INSTANCE = fresh;
            CreRaces.LOGGER.info("Loaded territory data: {} factions, {} clans, {} claims",
                    tm.factions.size(), tm.clans.size(), tm.claims.size());
        } catch (Exception ex) {
            CreRaces.LOGGER.error("Failed to load territory data: {}", ex.getMessage());
            INSTANCE = fresh; // use whatever was successfully parsed before the failure
        }
    }

    // ── Serialization helpers ──────────────────────────────────────────────────

    private static JsonObject serializeClaim(ClaimData c) {
        JsonObject o = new JsonObject();
        o.addProperty("faction",    c.getFactionId().toString());
        o.addProperty("race",       c.getRaceId().toString());
        o.addProperty("dormant",    c.isDormant());
        o.addProperty("persistent", c.isPersistent());
        if (c.getOwnerUUID() != null) o.addProperty("owner", c.getOwnerUUID().toString());
        return o;
    }

    private static ClaimData deserializeClaim(long key, JsonObject o) {
        UUID faction  = UUID.fromString(o.get("faction").getAsString());
        ResourceLocation race = new ResourceLocation(o.get("race").getAsString());
        boolean dormant    = o.has("dormant")    && o.get("dormant").getAsBoolean();
        boolean persistent = o.has("persistent") && o.get("persistent").getAsBoolean();
        UUID owner = o.has("owner") ? UUID.fromString(o.get("owner").getAsString()) : null;
        return new ClaimData(key, faction, race, dormant, persistent, owner);
    }

    private static JsonObject serializeFaction(FactionData f) {
        JsonObject o = new JsonObject();
        o.addProperty("race", f.getRaceId().toString());
        o.addProperty("name", f.getName());
        if (f.getClanId() != null) o.addProperty("clan", f.getClanId().toString());

        JsonObject members = new JsonObject();
        for (Map.Entry<UUID, FactionRank> e : f.getMembers().entrySet()) {
            members.addProperty(e.getKey().toString(), e.getValue().name());
        }
        o.add("members", members);

        JsonArray biomes = new JsonArray();
        for (ResourceLocation b : f.getHabitableBiomes()) biomes.add(b.toString());
        o.add("habitable_biomes", biomes);

        JsonArray chunks = new JsonArray();
        for (long k : f.getClaimedChunks()) chunks.add(k);
        o.add("claimed_chunks", chunks);

        JsonObject settings = new JsonObject();
        for (Map.Entry<String, String> e : f.getSettings().entrySet()) {
            settings.addProperty(e.getKey(), e.getValue());
        }
        o.add("settings", settings);
        return o;
    }

    private static FactionData deserializeFaction(UUID id, JsonObject o) {
        ResourceLocation race = new ResourceLocation(o.get("race").getAsString());
        String name = o.get("name").getAsString();
        FactionData f = new FactionData(id, race, name);
        if (o.has("clan")) f.setClanId(UUID.fromString(o.get("clan").getAsString()));

        if (o.has("members")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("members").entrySet()) {
                f.getMembers().put(UUID.fromString(e.getKey()),
                        FactionRank.valueOf(e.getValue().getAsString()));
            }
        }
        if (o.has("habitable_biomes")) {
            for (JsonElement e : o.getAsJsonArray("habitable_biomes")) {
                f.getHabitableBiomes().add(new ResourceLocation(e.getAsString()));
            }
        }
        if (o.has("claimed_chunks")) {
            for (JsonElement e : o.getAsJsonArray("claimed_chunks")) {
                f.getClaimedChunks().add(e.getAsLong());
            }
        }
        if (o.has("settings")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("settings").entrySet()) {
                f.getSettings().put(e.getKey(), e.getValue().getAsString());
            }
        }
        return f;
    }

    private static JsonObject serializeClan(ClanData c) {
        JsonObject o = new JsonObject();
        o.addProperty("race",   c.getRaceId().toString());
        o.addProperty("leader", c.getLeaderId().toString());

        JsonArray factions = new JsonArray();
        for (UUID fId : c.getMemberFactionIds()) factions.add(fId.toString());
        o.add("member_factions", factions);

        JsonObject locked = new JsonObject();
        for (Map.Entry<String, String> e : c.getLockedSettings().entrySet()) {
            locked.addProperty(e.getKey(), e.getValue());
        }
        o.add("locked_settings", locked);
        return o;
    }

    private static ClanData deserializeClan(UUID id, JsonObject o) {
        ResourceLocation race = new ResourceLocation(o.get("race").getAsString());
        UUID leader = UUID.fromString(o.get("leader").getAsString());
        ClanData c = new ClanData(id, race, leader);
        if (o.has("member_factions")) {
            for (JsonElement e : o.getAsJsonArray("member_factions")) {
                c.getMemberFactionIds().add(UUID.fromString(e.getAsString()));
            }
        }
        if (o.has("locked_settings")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("locked_settings").entrySet()) {
                c.getLockedSettings().put(e.getKey(), e.getValue().getAsString());
            }
        }
        return c;
    }
}
