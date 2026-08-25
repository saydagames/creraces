package mc.sayda.creraces.territory;

import com.google.gson.*;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TerritoryManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SAVE_FILE = "creraces_territory.json";

    private static volatile TerritoryManager INSTANCE;

    public static TerritoryManager get() {
        if (INSTANCE == null) INSTANCE = new TerritoryManager();
        return INSTANCE;
    }

    // Data
    private final Map<Long, ClaimData>             claims    = new HashMap<>();
    private final Map<ResourceLocation, ClanData>  diplomacy = new HashMap<>();

    // Maps BlockPos.asLong() of a root block → chunks it anchored, split by whether they were new claims
    private final Map<Long, RootAnchorData> rootAnchors = new HashMap<>();

    private static final class RootAnchorData {
        Set<Long> newClaims;
        Set<Long> preExisting;
        RootAnchorData(Set<Long> newClaims, Set<Long> preExisting) {
            this.newClaims = newClaims;
            this.preExisting = preExisting;
        }
    }

    // transient, not saved
    private final Map<UUID, Long> lastChunkKeys = new HashMap<>();

    // ClaimResult
    public enum ClaimResultType {
        SUCCESS, UNCLAIM_SUCCESS, INVALID_BIOME, ENEMY_TERRITORY, INSIDE_OWN_TERRITORY, INSUFFICIENT_COINS, ANCHOR_CHUNK, OUT_OF_RANGE, NOT_LEADER, MAX_NODES_REACHED
    }

    public static final class ClaimResult {
        public final ClaimResultType type;
        public final int claimed;
        public final int alreadyOwned;
        /** Chunk keys newly created by this operation. */
        public final Set<Long> newClaims;
        /** Pre-existing own-race chunk keys in the radius that were not newly claimed. */
        public final Set<Long> preExistingClaims;

        private ClaimResult(ClaimResultType type, int claimed, int alreadyOwned, Set<Long> newClaims, Set<Long> preExistingClaims) {
            this.type = type;
            this.claimed = claimed;
            this.alreadyOwned = alreadyOwned;
            this.newClaims = newClaims;
            this.preExistingClaims = preExistingClaims;
        }

        public static ClaimResult success(int n, Set<Long> keys, Set<Long> preExisting) { return new ClaimResult(ClaimResultType.SUCCESS, n, 0, keys, preExisting); }
        public static ClaimResult insideOwn(Set<Long> preExisting)                      { return new ClaimResult(ClaimResultType.INSIDE_OWN_TERRITORY, 0, 0, Collections.emptySet(), preExisting); }
        public static ClaimResult invalidBiome()                   { return new ClaimResult(ClaimResultType.INVALID_BIOME, 0, 0, Collections.emptySet(), Collections.emptySet()); }
        public static ClaimResult enemyTerritory()                 { return new ClaimResult(ClaimResultType.ENEMY_TERRITORY, 0, 0, Collections.emptySet(), Collections.emptySet()); }
        public static ClaimResult insufficientCoins()              { return new ClaimResult(ClaimResultType.INSUFFICIENT_COINS, 0, 0, Collections.emptySet(), Collections.emptySet()); }
        public static ClaimResult anchorChunk()                    { return new ClaimResult(ClaimResultType.ANCHOR_CHUNK, 0, 0, Collections.emptySet(), Collections.emptySet()); }
        public static ClaimResult maxNodesReached()                { return new ClaimResult(ClaimResultType.MAX_NODES_REACHED, 0, 0, Collections.emptySet(), Collections.emptySet()); }
    }

    // Accessors
    public Map<Long, ClaimData>            getClaims()    { return Collections.unmodifiableMap(claims); }
    public Map<ResourceLocation, ClanData> getDiplomacy() { return Collections.unmodifiableMap(diplomacy); }
    public ClaimData getClaimAt(long chunkKey)            { return claims.get(chunkKey); }
    public ClaimData getClaimAt(ChunkPos pos)             { return claims.get(pos.toLong()); }

    // Claim Operations

    /**
     * Claims a (2*radius+1)² island centered on the given chunk for raceId.
     * Only chunks that pass {@code include} are claimed; the rest are silently skipped.
     * Proceeds even when the center chunk is already owned by this race - only newly-unclaimed
     * chunks in the radius are added and returned in {@link ClaimResult#newClaims}.
     * Returns INSIDE_OWN_TERRITORY when the whole radius was already owned (nothing new claimed).
     * Returns ENEMY_TERRITORY if any included chunk belongs to another race.
     * Both the enemy-chunk check and the overwrite-instead behavior depend on TERRITORY_INTER_RACE_BLOCKING.
     */
    public ClaimResult claimIsland(ResourceLocation raceId, ChunkPos center, UUID claimerUUID,
            java.util.function.Predicate<ChunkPos> include) {
        // Enforce node (root block) limit per player
        int maxNodes = CreRacesConfig.TERRITORY_MAX_NODES_PER_PLAYER.get();
        if (maxNodes > 0) {
            long playerNodeCount = rootAnchors.values().stream()
                .filter(rad -> rad.newClaims.stream().anyMatch(k -> {
                    ClaimData cd = claims.get(k);
                    return cd != null && claimerUUID.equals(cd.getOwnerUUID());
                }))
                .count();
            if (playerNodeCount >= maxNodes) return ClaimResult.maxNodesReached();
        }

        boolean interRaceBlocking = CreRacesConfig.TERRITORY_INTER_RACE_BLOCKING.get();

        long centerKey = center.toLong();
        ClaimData existingCenter = claims.get(centerKey);
        if (interRaceBlocking && existingCenter != null && !existingCenter.getRaceId().equals(raceId)) {
            return ClaimResult.enemyTerritory();
        }

        int radius = CreRacesConfig.TERRITORY_DEFAULT_CLAIM_RADIUS.get();
        // First pass: ensure no enemy chunks anywhere in the radius (only when blocking is on)
        if (interRaceBlocking) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    ChunkPos cp = new ChunkPos(center.x + dx, center.z + dz);
                    if (!include.test(cp)) continue;
                    ClaimData cd = claims.get(cp.toLong());
                    if (cd != null && !cd.getRaceId().equals(raceId)) return ClaimResult.enemyTerritory();
                }
            }
        }

        // Second pass: claim unclaimed chunks (and enemy chunks when blocking is off)
        Set<Long> newKeys = new HashSet<>();
        Set<Long> preExistingKeys = new HashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos cp = new ChunkPos(center.x + dx, center.z + dz);
                if (!include.test(cp)) continue;
                long key = cp.toLong();
                ClaimData existing = claims.get(key);
                if (existing != null) {
                    if (existing.getRaceId().equals(raceId)) {
                        preExistingKeys.add(key);
                    } else if (!interRaceBlocking) {
                        // Overwrite enemy chunk when inter-race blocking is disabled
                        claims.put(key, new ClaimData(key, raceId, false, claimerUUID));
                        newKeys.add(key);
                    }
                    continue;
                }
                claims.put(key, new ClaimData(key, raceId, false, claimerUUID));
                newKeys.add(key);
            }
        }

        if (newKeys.isEmpty()) return ClaimResult.insideOwn(preExistingKeys);
        return ClaimResult.success(newKeys.size(), newKeys, preExistingKeys);
    }

    /** Convenience overload: claims all chunks in the island with no biome filter. */
    public ClaimResult claimIsland(ResourceLocation raceId, ChunkPos center, UUID claimerUUID) {
        return claimIsland(raceId, center, claimerUUID, cp -> true);
    }

    /** Claims exactly one chunk for raceId; used by the territory map (no island expansion). */
    public ClaimResult claimChunk(ResourceLocation raceId, ChunkPos chunk, UUID claimerUUID, int pricePaid) {
        long key = chunk.toLong();
        ClaimData existing = claims.get(key);
        if (existing != null) {
            if (existing.getRaceId().equals(raceId)) return ClaimResult.insideOwn(Set.of(key));
            return ClaimResult.enemyTerritory();
        }
        claims.put(key, new ClaimData(key, raceId, false, claimerUUID, pricePaid));
        return ClaimResult.success(1, Set.of(key), Collections.emptySet());
    }

    /** @deprecated Use {@link #claimChunk(ResourceLocation, ChunkPos, UUID, int)} with explicit price. */
    @Deprecated
    public ClaimResult claimChunk(ResourceLocation raceId, ChunkPos chunk, UUID claimerUUID) {
        return claimChunk(raceId, chunk, claimerUUID, 0);
    }

    public boolean unclaimChunk(ResourceLocation raceId, ChunkPos chunk) {
        long key = chunk.toLong();
        ClaimData cd = claims.get(key);
        if (cd == null || !cd.getRaceId().equals(raceId)) return false;
        if (cd.isPersistent()) return false;
        claims.remove(key);
        return true;
    }

    /** Removes a chunk only if the given player is its owner, regardless of persistence. */
    public boolean unclaimOwnChunk(java.util.UUID callerUUID, ChunkPos chunk) {
        long key = chunk.toLong();
        ClaimData cd = claims.get(key);
        if (cd == null) return false;
        if (!callerUUID.equals(cd.getOwnerUUID())) return false;
        claims.remove(key);
        return true;
    }

    /** Admin: force-remove a single chunk regardless of persistence. */
    public boolean forceUnclaimChunk(ChunkPos chunk) {
        return claims.remove(chunk.toLong()) != null;
    }

    /** Sets or clears the persistent (anchor) flag on an already-claimed chunk. No-op if the chunk is unclaimed. */
    public void setChunkPersistent(ChunkPos pos, boolean persistent) {
        long key = pos.toLong();
        ClaimData cd = claims.get(key);
        if (cd == null) return;
        claims.put(key, new ClaimData(key, cd.getRaceId(), persistent, cd.getOwnerUUID(), cd.getPricePaid()));
    }

    /**
     * Called when a root block is placed. Marks all provided chunk keys as persistent and records
     * new vs. pre-existing chunks so removal can handle them differently.
     */
    public void placeRootBlock(net.minecraft.core.BlockPos rootPos, Set<Long> newClaims, Set<Long> preExisting) {
        Set<Long> anchoredNew = new HashSet<>();
        for (long key : newClaims) {
            ClaimData cd = claims.get(key);
            if (cd != null) {
                claims.put(key, new ClaimData(key, cd.getRaceId(), true, cd.getOwnerUUID(), cd.getPricePaid()));
                anchoredNew.add(key);
            }
        }
        Set<Long> anchoredPre = new HashSet<>();
        for (long key : preExisting) {
            ClaimData cd = claims.get(key);
            if (cd != null) {
                claims.put(key, new ClaimData(key, cd.getRaceId(), true, cd.getOwnerUUID(), cd.getPricePaid()));
                anchoredPre.add(key);
            }
        }
        if (!anchoredNew.isEmpty() || !anchoredPre.isEmpty()) {
            rootAnchors.put(rootPos.asLong(), new RootAnchorData(anchoredNew, anchoredPre));
        }
    }

    /**
     * Called when a root block is removed. Fully unclaims chunks it created; only un-persists
     * pre-existing chunks so they remain claimed but can again be voluntarily unclaimed.
     */
    public void removeRootBlock(net.minecraft.core.BlockPos rootPos) {
        RootAnchorData data = rootAnchors.remove(rootPos.asLong());
        if (data == null) return;
        data.newClaims.forEach(claims::remove);
        for (long key : data.preExisting) {
            ClaimData cd = claims.get(key);
            if (cd != null) claims.put(key, new ClaimData(key, cd.getRaceId(), false, cd.getOwnerUUID(), cd.getPricePaid()));
        }
    }

    /** Admin: remove all claims for a race. */
    public void unclaimAllForRace(ResourceLocation raceId) {
        claims.entrySet().removeIf(e -> e.getValue().getRaceId().equals(raceId));
    }

    /** Called on race reset: removes all claims owned by this player and cleans up their root anchors. */
    public void unclaimAllForPlayer(UUID playerUUID) {
        Set<Long> removed = new HashSet<>();
        claims.entrySet().removeIf(e -> {
            if (e.getValue().getOwnerUUID().equals(playerUUID)) {
                removed.add(e.getKey());
                return true;
            }
            return false;
        });
        if (removed.isEmpty()) return;
        rootAnchors.entrySet().removeIf(e -> {
            RootAnchorData d = e.getValue();
            d.newClaims.removeAll(removed);
            d.preExisting.removeAll(removed);
            return d.newClaims.isEmpty() && d.preExisting.isEmpty();
        });
    }

    // Diplomacy

    public ClanData getOrCreateClan(ResourceLocation raceId) {
        return diplomacy.computeIfAbsent(raceId, ClanData::new);
    }

    public ClanData getClanOrEmpty(ResourceLocation raceId) {
        ClanData existing = diplomacy.get(raceId);
        return existing != null ? existing : new ClanData(raceId);
    }

    public DiplomacyStatus getDiplomacy(ResourceLocation raceA, ResourceLocation raceB) {
        ClanData clan = diplomacy.get(raceA);
        if (clan == null) return DiplomacyStatus.NEUTRAL;
        return clan.getRelation(raceB);
    }

    public void setDiplomacy(ResourceLocation raceA, ResourceLocation raceB, DiplomacyStatus status) {
        ClanData clan = getOrCreateClan(raceA);
        clan.setRelation(raceB, status);
        if (clan.getRelations().isEmpty()) diplomacy.remove(raceA);
    }

    // Tick / Notifications

    public void tick(MinecraftServer server) {
        tickTerritoryNotifications(server);
    }

    public void clearPlayerTracking(UUID playerUUID) {
        lastChunkKeys.remove(playerUUID);
    }

    private void tickTerritoryNotifications(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            long currentKey = player.chunkPosition().toLong();
            Long lastKey = lastChunkKeys.put(playerId, currentKey);

            if (lastKey == null || lastKey == currentKey) continue;

            ResourceLocation newRace = claims.containsKey(currentKey) ? claims.get(currentKey).getRaceId() : null;
            ResourceLocation oldRace = claims.containsKey(lastKey)    ? claims.get(lastKey).getRaceId()    : null;

            if (java.util.Objects.equals(newRace, oldRace)) continue;

            ResourceLocation myRace = mc.sayda.creraces.capability.DataUtils.getVariables(player)
                    .map(mc.sayda.creraces.capability.IPlayerVariables::getRace)
                    .orElse(null);

            net.minecraft.network.chat.MutableComponent msg = null;

            if (newRace != null) {
                String name = raceName(newRace);
                if (newRace.equals(myRace)) {
                    msg = net.minecraft.network.chat.Component.translatable("msg.creraces.territory.entered_own", name);
                } else {
                    DiplomacyStatus rel = myRace != null ? getDiplomacy(myRace, newRace) : DiplomacyStatus.NEUTRAL;
                    String key = switch (rel) {
                        case ALLY    -> "msg.creraces.territory.entered_ally";
                        case ENEMY   -> "msg.creraces.territory.entered_enemy";
                        case NEUTRAL -> "msg.creraces.territory.entered_neutral";
                    };
                    msg = net.minecraft.network.chat.Component.translatable(key, name);
                }
            } else if (oldRace != null && oldRace.equals(myRace)) {
                msg = net.minecraft.network.chat.Component.translatable("msg.creraces.territory.left", raceName(oldRace));
            }

            if (msg != null && mc.sayda.creraces.config.CreRacesConfig.TERRITORY_ENTRY_MESSAGES.get())
                player.displayClientMessage(msg, true);
        }
    }

    private static String raceName(ResourceLocation raceId) {
        mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(raceId);
        return race != null ? race.name().getString() : raceId.getPath();
    }

    // Persistence

    public static void save(MinecraftServer server) {
        Path path = server.getWorldPath(Objects.requireNonNull(LevelResource.ROOT)).resolve(SAVE_FILE);
        TerritoryManager tm = get();
        JsonObject root = new JsonObject();

        JsonObject claimsJson = new JsonObject();
        for (Map.Entry<Long, ClaimData> e : tm.claims.entrySet()) {
            claimsJson.add(String.valueOf(e.getKey()), serializeClaim(e.getValue()));
        }
        root.add("claims", claimsJson);

        JsonObject diplomacyJson = new JsonObject();
        for (Map.Entry<ResourceLocation, ClanData> e : tm.diplomacy.entrySet()) {
            diplomacyJson.add(e.getKey().toString(), serializeClan(e.getValue()));
        }
        root.add("diplomacy", diplomacyJson);

        JsonObject anchorsJson = new JsonObject();
        for (Map.Entry<Long, RootAnchorData> e : tm.rootAnchors.entrySet()) {
            RootAnchorData rad = e.getValue();
            JsonObject radObj = new JsonObject();
            JsonArray newArr = new JsonArray();
            for (long k : rad.newClaims) newArr.add(k);
            radObj.add("new", newArr);
            JsonArray preArr = new JsonArray();
            for (long k : rad.preExisting) preArr.add(k);
            radObj.add("pre", preArr);
            anchorsJson.add(String.valueOf(e.getKey()), radObj);
        }
        root.add("rootAnchors", anchorsJson);

        try (Writer w = Files.newBufferedWriter(path)) {
            GSON.toJson(root, w);
        } catch (IOException ex) {
            CreRaces.LOGGER.error("Failed to save territory data: {}", ex.getMessage());
        }
    }

    public static void load(MinecraftServer server) {
        TerritoryManager fresh = new TerritoryManager();
        Path path = server.getWorldPath(Objects.requireNonNull(LevelResource.ROOT)).resolve(SAVE_FILE);
        if (!Files.exists(path)) { INSTANCE = fresh; return; }

        try (Reader r = Files.newBufferedReader(path)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) { INSTANCE = fresh; return; }

            if (root.has("claims")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("claims").entrySet()) {
                    try {
                        long key = Long.parseLong(e.getKey());
                        fresh.claims.put(key, deserializeClaim(key, e.getValue().getAsJsonObject()));
                    } catch (Exception ex) {
                        CreRaces.LOGGER.warn("Skipping malformed claim '{}': {}", e.getKey(), ex.getMessage());
                    }
                }
            }

            if (root.has("diplomacy")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("diplomacy").entrySet()) {
                    try {
                        ResourceLocation raceId = new ResourceLocation(e.getKey());
                        fresh.diplomacy.put(raceId, deserializeClan(raceId, e.getValue().getAsJsonObject()));
                    } catch (Exception ex) {
                        CreRaces.LOGGER.warn("Skipping malformed diplomacy '{}': {}", e.getKey(), ex.getMessage());
                    }
                }
            }

            if (root.has("rootAnchors")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("rootAnchors").entrySet()) {
                    try {
                        long rootKey = Long.parseLong(e.getKey());
                        Set<Long> newChunks = new HashSet<>();
                        Set<Long> preChunks = new HashSet<>();
                        JsonElement val = e.getValue();
                        if (val.isJsonObject()) {
                            // Current format: {"new": [...], "pre": [...]}
                            JsonObject radObj = val.getAsJsonObject();
                            if (radObj.has("new")) for (JsonElement c : radObj.getAsJsonArray("new")) newChunks.add(c.getAsLong());
                            if (radObj.has("pre")) for (JsonElement c : radObj.getAsJsonArray("pre")) preChunks.add(c.getAsLong());
                        } else {
                            // Legacy flat array - treat all as new claims
                            for (JsonElement c : val.getAsJsonArray()) newChunks.add(c.getAsLong());
                        }
                        fresh.rootAnchors.put(rootKey, new RootAnchorData(newChunks, preChunks));
                    } catch (Exception ex) {
                        CreRaces.LOGGER.warn("Skipping malformed rootAnchor entry '{}': {}", e.getKey(), ex.getMessage());
                    }
                }
            }

            INSTANCE = fresh;
            CreRaces.LOGGER.info("Loaded territory data: {} claims, {} diplomacy records, {} root anchors",
                    fresh.claims.size(), fresh.diplomacy.size(), fresh.rootAnchors.size());
        } catch (Exception ex) {
            CreRaces.LOGGER.error("Failed to load territory data: {}", ex.getMessage());
            INSTANCE = fresh;
        }
    }

    // Serialization helpers

    private static JsonObject serializeClaim(ClaimData c) {
        JsonObject o = new JsonObject();
        o.addProperty("race", c.getRaceId().toString());
        o.addProperty("persistent", c.isPersistent());
        if (c.getOwnerUUID() != null) o.addProperty("owner", c.getOwnerUUID().toString());
        if (c.getPricePaid() > 0) o.addProperty("price", c.getPricePaid());
        return o;
    }

    private static ClaimData deserializeClaim(long key, JsonObject o) {
        ResourceLocation race = new ResourceLocation(o.get("race").getAsString());
        boolean persistent = o.has("persistent") && o.get("persistent").getAsBoolean();
        UUID owner = o.has("owner") ? UUID.fromString(o.get("owner").getAsString()) : null;
        int price = o.has("price") ? o.get("price").getAsInt() : 0;
        return new ClaimData(key, race, persistent, owner, price);
    }

    private static JsonObject serializeClan(ClanData c) {
        JsonObject o = new JsonObject();
        JsonObject relations = new JsonObject();
        for (Map.Entry<ResourceLocation, DiplomacyStatus> e : c.getRelations().entrySet()) {
            relations.addProperty(e.getKey().toString(), e.getValue().name());
        }
        o.add("relations", relations);
        return o;
    }

    private static ClanData deserializeClan(ResourceLocation raceId, JsonObject o) {
        ClanData c = new ClanData(raceId);
        if (o.has("relations")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("relations").entrySet()) {
                try {
                    c.setRelation(new ResourceLocation(e.getKey()),
                            DiplomacyStatus.valueOf(e.getValue().getAsString()));
                } catch (Exception ex) {
                    CreRaces.LOGGER.warn("Skipping malformed diplomacy entry '{}'", e.getKey());
                }
            }
        }
        return c;
    }
}
