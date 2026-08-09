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

    private static TerritoryManager INSTANCE;

    public static TerritoryManager get() {
        if (INSTANCE == null) INSTANCE = new TerritoryManager();
        return INSTANCE;
    }

    // ── Data ───────────────────────────────────────────────────────────────────
    private final Map<Long, ClaimData>             claims    = new HashMap<>();
    private final Map<ResourceLocation, ClanData>  diplomacy = new HashMap<>();

    // Maps BlockPos.asLong() of a root block → chunk keys it anchored
    private final Map<Long, Set<Long>> rootAnchors = new HashMap<>();

    // transient, not saved
    private final Map<UUID, Long> lastChunkKeys = new HashMap<>();

    // ── ClaimResult ────────────────────────────────────────────────────────────
    public enum ClaimResultType {
        SUCCESS, UNCLAIM_SUCCESS, PARTIAL, INVALID_BIOME, ENEMY_TERRITORY, INSIDE_OWN_TERRITORY, INSUFFICIENT_COINS, ANCHOR_CHUNK, OUT_OF_RANGE, NOT_LEADER
    }

    public static final class ClaimResult {
        public final ClaimResultType type;
        public final int claimed;
        public final int alreadyOwned;
        /** Chunk keys that were newly created by this operation — used to anchor only what the tree itself claimed. */
        public final Set<Long> newClaims;

        private ClaimResult(ClaimResultType type, int claimed, int alreadyOwned, Set<Long> newClaims) {
            this.type = type;
            this.claimed = claimed;
            this.alreadyOwned = alreadyOwned;
            this.newClaims = newClaims;
        }

        public static ClaimResult success(int n, Set<Long> keys)   { return new ClaimResult(ClaimResultType.SUCCESS, n, 0, keys); }
        public static ClaimResult partial(int ok, int skipped)     { return new ClaimResult(ClaimResultType.PARTIAL, ok, skipped, Collections.emptySet()); }
        public static ClaimResult invalidBiome()                   { return new ClaimResult(ClaimResultType.INVALID_BIOME, 0, 0, Collections.emptySet()); }
        public static ClaimResult enemyTerritory()                 { return new ClaimResult(ClaimResultType.ENEMY_TERRITORY, 0, 0, Collections.emptySet()); }
        public static ClaimResult insideOwn()                      { return new ClaimResult(ClaimResultType.INSIDE_OWN_TERRITORY, 0, 0, Collections.emptySet()); }
        public static ClaimResult insufficientCoins()              { return new ClaimResult(ClaimResultType.INSUFFICIENT_COINS, 0, 0, Collections.emptySet()); }
        public static ClaimResult anchorChunk()                    { return new ClaimResult(ClaimResultType.ANCHOR_CHUNK, 0, 0, Collections.emptySet()); }
    }

    // ── Accessors ──────────────────────────────────────────────────────────────
    public Map<Long, ClaimData>            getClaims()    { return Collections.unmodifiableMap(claims); }
    public Map<ResourceLocation, ClanData> getDiplomacy() { return Collections.unmodifiableMap(diplomacy); }
    public ClaimData getClaimAt(long chunkKey)            { return claims.get(chunkKey); }
    public ClaimData getClaimAt(ChunkPos pos)             { return claims.get(pos.toLong()); }

    // ── Claim Operations ───────────────────────────────────────────────────────

    /**
     * Claims a (2*radius+1)² island centered on the given chunk for raceId.
     * Only chunks that pass {@code include} are claimed; the rest are silently skipped.
     * Proceeds even when the center chunk is already owned by this race — only newly-unclaimed
     * chunks in the radius are added and returned in {@link ClaimResult#newClaims}.
     * Returns INSIDE_OWN_TERRITORY when the whole radius was already owned (nothing new claimed).
     * Returns ENEMY_TERRITORY if any included chunk belongs to another race.
     */
    public ClaimResult claimIsland(ResourceLocation raceId, ChunkPos center, UUID claimerUUID,
            java.util.function.Predicate<ChunkPos> include) {
        long centerKey = center.toLong();
        ClaimData existingCenter = claims.get(centerKey);
        if (existingCenter != null && !existingCenter.getRaceId().equals(raceId)) {
            return ClaimResult.enemyTerritory();
        }

        int radius = CreRacesConfig.TERRITORY_DEFAULT_CLAIM_RADIUS.get();
        // First pass: ensure no enemy chunks anywhere in the radius
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos cp = new ChunkPos(center.x + dx, center.z + dz);
                if (!include.test(cp)) continue;
                ClaimData cd = claims.get(cp.toLong());
                if (cd != null && !cd.getRaceId().equals(raceId)) return ClaimResult.enemyTerritory();
            }
        }

        // Second pass: claim only the unclaimed chunks and track which are new
        Set<Long> newKeys = new HashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos cp = new ChunkPos(center.x + dx, center.z + dz);
                if (!include.test(cp)) continue;
                long key = cp.toLong();
                if (claims.containsKey(key)) continue;
                claims.put(key, new ClaimData(key, raceId, false, claimerUUID));
                newKeys.add(key);
            }
        }

        if (newKeys.isEmpty()) return ClaimResult.insideOwn();
        return ClaimResult.success(newKeys.size(), newKeys);
    }

    /** Convenience overload: claims all chunks in the island with no biome filter. */
    public ClaimResult claimIsland(ResourceLocation raceId, ChunkPos center, UUID claimerUUID) {
        return claimIsland(raceId, center, claimerUUID, cp -> true);
    }

    /** Claims exactly one chunk for raceId; used by the territory map (no island expansion). */
    public ClaimResult claimChunk(ResourceLocation raceId, ChunkPos chunk, UUID claimerUUID) {
        long key = chunk.toLong();
        ClaimData existing = claims.get(key);
        if (existing != null) {
            if (existing.getRaceId().equals(raceId)) return ClaimResult.insideOwn();
            return ClaimResult.enemyTerritory();
        }
        claims.put(key, new ClaimData(key, raceId, false, claimerUUID));
        return ClaimResult.success(1, Set.of(key));
    }

    public boolean unclaimChunk(ResourceLocation raceId, ChunkPos chunk) {
        long key = chunk.toLong();
        ClaimData cd = claims.get(key);
        if (cd == null || !cd.getRaceId().equals(raceId)) return false;
        if (cd.isPersistent()) return false;
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
        claims.put(key, new ClaimData(key, cd.getRaceId(), persistent, cd.getOwnerUUID()));
    }

    /**
     * Called when a root block is placed. Marks all provided chunk keys as persistent
     * and records which chunks this root block owns so they can be unclaimed precisely on removal.
     */
    public void placeRootBlock(net.minecraft.core.BlockPos rootPos, Set<Long> islandChunkKeys) {
        Set<Long> anchored = new HashSet<>();
        for (long key : islandChunkKeys) {
            ClaimData cd = claims.get(key);
            if (cd != null) {
                claims.put(key, new ClaimData(key, cd.getRaceId(), true, cd.getOwnerUUID()));
                anchored.add(key);
            }
        }
        if (!anchored.isEmpty()) rootAnchors.put(rootPos.asLong(), anchored);
    }

    /**
     * Called when a root block is removed. Fully unclaims only the chunks this root block
     * originally anchored, leaving any overlapping territory from other trees intact.
     */
    public void removeRootBlock(net.minecraft.core.BlockPos rootPos) {
        Set<Long> anchored = rootAnchors.remove(rootPos.asLong());
        if (anchored == null) return;
        anchored.forEach(claims::remove);
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
            e.getValue().removeAll(removed);
            return e.getValue().isEmpty();
        });
    }

    // ── Diplomacy ──────────────────────────────────────────────────────────────

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
        getOrCreateClan(raceA).setRelation(raceB, status);
        ClanData clan = diplomacy.get(raceA);
        if (clan.getRelations().isEmpty()) diplomacy.remove(raceA);
    }

    // ── Tick / Notifications ───────────────────────────────────────────────────

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

            if (msg != null) player.displayClientMessage(msg, true);
        }
    }

    private static String raceName(ResourceLocation raceId) {
        mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(raceId);
        return race != null ? race.name().getString() : raceId.getPath();
    }

    // ── Persistence ────────────────────────────────────────────────────────────

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
        for (Map.Entry<Long, Set<Long>> e : tm.rootAnchors.entrySet()) {
            JsonArray arr = new JsonArray();
            for (long k : e.getValue()) arr.add(k);
            anchorsJson.add(String.valueOf(e.getKey()), arr);
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
                        Set<Long> chunks = new HashSet<>();
                        for (JsonElement c : e.getValue().getAsJsonArray()) chunks.add(c.getAsLong());
                        fresh.rootAnchors.put(rootKey, chunks);
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

    // ── Serialization helpers ──────────────────────────────────────────────────

    private static JsonObject serializeClaim(ClaimData c) {
        JsonObject o = new JsonObject();
        o.addProperty("race", c.getRaceId().toString());
        o.addProperty("persistent", c.isPersistent());
        if (c.getOwnerUUID() != null) o.addProperty("owner", c.getOwnerUUID().toString());
        return o;
    }

    private static ClaimData deserializeClaim(long key, JsonObject o) {
        ResourceLocation race = new ResourceLocation(o.get("race").getAsString());
        boolean persistent = o.has("persistent") && o.get("persistent").getAsBoolean();
        UUID owner = o.has("owner") ? UUID.fromString(o.get("owner").getAsString()) : null;
        return new ClaimData(key, race, persistent, owner);
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
