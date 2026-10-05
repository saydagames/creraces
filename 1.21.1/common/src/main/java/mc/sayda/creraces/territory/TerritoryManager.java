package mc.sayda.creraces.territory;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

/**
 * Chunk claims, root block anchors and race diplomacy for the whole server, stored as SavedData
 * on the overworld.
 */
public class TerritoryManager extends SavedData {

    private static final String DATA_ID = "creraces_territory";

    private static volatile TerritoryManager INSTANCE;

    public static TerritoryManager get() {
        if (INSTANCE == null) INSTANCE = new TerritoryManager();
        return INSTANCE;
    }

    private final Map<Long, ClaimData>             claims    = new HashMap<>();
    private final Map<ResourceLocation, ClanData>  diplomacy = new HashMap<>();

    // Root block position (BlockPos.asLong) to the chunks it anchored, split by whether it claimed them itself.
    private final Map<Long, RootAnchorData> rootAnchors = new HashMap<>();

    private static final class RootAnchorData {
        Set<Long> newClaims;
        Set<Long> preExisting;
        RootAnchorData(Set<Long> newClaims, Set<Long> preExisting) {
            this.newClaims = newClaims;
            this.preExisting = preExisting;
        }
    }

    // Last chunk each player stood in, for entry messages. Not saved.
    private final Map<UUID, Long> lastChunkKeys = new HashMap<>();

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

    public Map<Long, ClaimData>            getClaims()    { return Collections.unmodifiableMap(claims); }
    public Map<ResourceLocation, ClanData> getDiplomacy() { return Collections.unmodifiableMap(diplomacy); }
    public ClaimData getClaimAt(long chunkKey)            { return claims.get(chunkKey); }
    public ClaimData getClaimAt(ChunkPos pos)             { return claims.get(pos.toLong()); }

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
            Predicate<ChunkPos> include) {
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
                        claims.put(key, new ClaimData(raceId, false, claimerUUID));
                        newKeys.add(key);
                    }
                    continue;
                }
                claims.put(key, new ClaimData(raceId, false, claimerUUID));
                newKeys.add(key);
            }
        }

        if (newKeys.isEmpty()) return ClaimResult.insideOwn(preExistingKeys);
        setDirty();
        return ClaimResult.success(newKeys.size(), newKeys, preExistingKeys);
    }

    /** Claims exactly one chunk for raceId; used by the territory map (no island expansion). */
    public ClaimResult claimChunk(ResourceLocation raceId, ChunkPos chunk, UUID claimerUUID, int pricePaid) {
        long key = chunk.toLong();
        ClaimData existing = claims.get(key);
        if (existing != null) {
            if (existing.getRaceId().equals(raceId)) return ClaimResult.insideOwn(Set.of(key));
            return ClaimResult.enemyTerritory();
        }
        claims.put(key, new ClaimData(raceId, false, claimerUUID, pricePaid));
        setDirty();
        return ClaimResult.success(1, Set.of(key), Collections.emptySet());
    }

    public boolean unclaimChunk(ResourceLocation raceId, ChunkPos chunk) {
        long key = chunk.toLong();
        ClaimData cd = claims.get(key);
        if (cd == null || !cd.getRaceId().equals(raceId)) return false;
        if (cd.isPersistent()) return false;
        claims.remove(key);
        setDirty();
        return true;
    }

    /** Removes a chunk only if the given player is its owner, regardless of persistence. */
    public boolean unclaimOwnChunk(UUID callerUUID, ChunkPos chunk) {
        long key = chunk.toLong();
        ClaimData cd = claims.get(key);
        if (cd == null) return false;
        if (!callerUUID.equals(cd.getOwnerUUID())) return false;
        claims.remove(key);
        setDirty();
        return true;
    }

    /** Admin: force-remove a single chunk regardless of persistence. */
    public boolean forceUnclaimChunk(ChunkPos chunk) {
        boolean removed = claims.remove(chunk.toLong()) != null;
        if (removed) setDirty();
        return removed;
    }

    /**
     * Called when a root block is placed. Marks all provided chunk keys as persistent and records
     * new vs. pre-existing chunks so removal can handle them differently.
     */
    public void placeRootBlock(BlockPos rootPos, Set<Long> newClaims, Set<Long> preExisting) {
        Set<Long> anchoredNew = new HashSet<>();
        for (long key : newClaims) {
            ClaimData cd = claims.get(key);
            if (cd != null) {
                claims.put(key, new ClaimData(cd.getRaceId(), true, cd.getOwnerUUID(), cd.getPricePaid()));
                anchoredNew.add(key);
            }
        }
        Set<Long> anchoredPre = new HashSet<>();
        for (long key : preExisting) {
            ClaimData cd = claims.get(key);
            if (cd != null) {
                claims.put(key, new ClaimData(cd.getRaceId(), true, cd.getOwnerUUID(), cd.getPricePaid()));
                anchoredPre.add(key);
            }
        }
        if (!anchoredNew.isEmpty() || !anchoredPre.isEmpty()) {
            rootAnchors.put(rootPos.asLong(), new RootAnchorData(anchoredNew, anchoredPre));
            setDirty();
        }
    }

    /**
     * Called when a root block is removed. Chunks it claimed are unclaimed and chunks it pinned lose
     * their persistence, except where another root still covers them.
     */
    public void removeRootBlock(BlockPos rootPos) {
        RootAnchorData data = rootAnchors.remove(rootPos.asLong());
        if (data == null) return;
        Set<Long> stillPinned = pinnedChunks();
        for (long key : data.newClaims) {
            if (!stillPinned.contains(key)) claims.remove(key);
        }
        for (long key : data.preExisting) {
            if (!stillPinned.contains(key)) unpin(key);
        }
        setDirty();
    }

    /** Admin: remove all claims for a race. */
    public void unclaimAllForRace(ResourceLocation raceId) {
        boolean changed = claims.entrySet().removeIf(e -> e.getValue().getRaceId().equals(raceId));
        if (changed) setDirty();
    }

    /**
     * Called when a player's race is reset or changed: removes every claim they own and the root
     * anchors they placed. Other players' chunks those anchors pinned stay claimed but lose their
     * persistence, unless another root still covers them.
     */
    public void unclaimAllForPlayer(UUID playerUUID) {
        Set<Long> owned = new HashSet<>();
        claims.forEach((key, claim) -> {
            if (playerUUID.equals(claim.getOwnerUUID())) owned.add(key);
        });
        if (owned.isEmpty()) return;
        owned.forEach(claims::remove);

        Set<Long> unpinned = new HashSet<>();
        rootAnchors.values().removeIf(anchor -> {
            // A root belongs to whoever it newly claimed chunks for, the same test the node limit uses.
            if (!Collections.disjoint(anchor.newClaims, owned)) {
                unpinned.addAll(anchor.newClaims);
                unpinned.addAll(anchor.preExisting);
                return true;
            }
            anchor.newClaims.removeAll(owned);
            anchor.preExisting.removeAll(owned);
            return anchor.newClaims.isEmpty() && anchor.preExisting.isEmpty();
        });
        unpinned.removeAll(pinnedChunks());
        unpinned.forEach(this::unpin);
        setDirty();
    }

    /** Every chunk some remaining root anchor covers. */
    private Set<Long> pinnedChunks() {
        Set<Long> pinned = new HashSet<>();
        for (RootAnchorData anchor : rootAnchors.values()) {
            pinned.addAll(anchor.newClaims);
            pinned.addAll(anchor.preExisting);
        }
        return pinned;
    }

    /** Keeps the chunk claimed but lets it be unclaimed voluntarily again. */
    private void unpin(long key) {
        ClaimData cd = claims.get(key);
        if (cd != null) claims.put(key, new ClaimData(cd.getRaceId(), false, cd.getOwnerUUID(), cd.getPricePaid()));
    }

    public ClanData getOrCreateClan(ResourceLocation raceId) {
        ClanData existing = diplomacy.get(raceId);
        if (existing != null) return existing;
        ClanData created = new ClanData(raceId);
        diplomacy.put(raceId, created);
        setDirty();
        return created;
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
        setDirty();
    }

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

            ResourceLocation newRace = raceAt(currentKey);
            ResourceLocation oldRace = raceAt(lastKey);

            if (Objects.equals(newRace, oldRace)) continue;

            ResourceLocation myRace = DataUtils.getVariables(player)
                    .map(IPlayerVariables::getRace)
                    .orElse(null);

            MutableComponent msg = null;

            if (newRace != null) {
                String name = raceName(newRace);
                if (newRace.equals(myRace)) {
                    msg = Component.translatable("msg.creraces.territory.entered_own", name);
                } else {
                    DiplomacyStatus rel = myRace != null ? getDiplomacy(myRace, newRace) : DiplomacyStatus.NEUTRAL;
                    String key = switch (rel) {
                        case ALLY    -> "msg.creraces.territory.entered_ally";
                        case ENEMY   -> "msg.creraces.territory.entered_enemy";
                        case NEUTRAL -> "msg.creraces.territory.entered_neutral";
                    };
                    msg = Component.translatable(key, name);
                }
            } else if (oldRace != null && oldRace.equals(myRace)) {
                msg = Component.translatable("msg.creraces.territory.left", raceName(oldRace));
            }

            if (msg != null && CreRacesConfig.TERRITORY_ENTRY_MESSAGES.get())
                player.displayClientMessage(msg, true);
        }
    }

    @Nullable
    private ResourceLocation raceAt(long chunkKey) {
        ClaimData claim = claims.get(chunkKey);
        return claim != null ? claim.getRaceId() : null;
    }

    private static String raceName(ResourceLocation raceId) {
        Race race = RaceRegistry.get(raceId);
        return race != null ? race.name().getString() : raceId.getPath();
    }

    // Persistence: vanilla SavedData stored in <world>/data/creraces_territory.dat.

    public static void load(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        // No datafixer: this is mod data, not level.dat. NeoForge and Fabric API both accept null here.
        SavedData.Factory<TerritoryManager> factory = new SavedData.Factory<>(
                TerritoryManager::new,
                (tag, registries) -> fromTag(tag),
                null);
        INSTANCE = overworld.getDataStorage().computeIfAbsent(factory, DATA_ID);
        CreRaces.LOGGER.info("Loaded territory data: {} claims, {} diplomacy records, {} root anchors",
                INSTANCE.claims.size(), INSTANCE.diplomacy.size(), INSTANCE.rootAnchors.size());
    }

    /** Marks the data dirty and flushes it immediately rather than waiting for the next autosave. */
    public static void save(MinecraftServer server) {
        if (INSTANCE != null) {
            INSTANCE.setDirty();
            server.overworld().getDataStorage().save();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag claimsTag = new CompoundTag();
        for (Map.Entry<Long, ClaimData> e : claims.entrySet()) {
            claimsTag.put(String.valueOf(e.getKey()), serializeClaim(e.getValue()));
        }
        tag.put("claims", claimsTag);

        CompoundTag diplomacyTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, ClanData> e : diplomacy.entrySet()) {
            diplomacyTag.put(e.getKey().toString(), serializeClan(e.getValue()));
        }
        tag.put("diplomacy", diplomacyTag);

        CompoundTag anchorsTag = new CompoundTag();
        for (Map.Entry<Long, RootAnchorData> e : rootAnchors.entrySet()) {
            RootAnchorData rad = e.getValue();
            CompoundTag radTag = new CompoundTag();
            radTag.put("new", new LongArrayTag(rad.newClaims.stream().mapToLong(Long::longValue).toArray()));
            radTag.put("pre", new LongArrayTag(rad.preExisting.stream().mapToLong(Long::longValue).toArray()));
            anchorsTag.put(String.valueOf(e.getKey()), radTag);
        }
        tag.put("rootAnchors", anchorsTag);
        return tag;
    }

    private static TerritoryManager fromTag(CompoundTag tag) {
        TerritoryManager tm = new TerritoryManager();

        CompoundTag claimsTag = tag.getCompound("claims");
        for (String key : claimsTag.getAllKeys()) {
            try {
                long chunkKey = Long.parseLong(key);
                tm.claims.put(chunkKey, deserializeClaim(claimsTag.getCompound(key)));
            } catch (Exception ex) {
                CreRaces.LOGGER.warn("Skipping malformed claim '{}': {}", key, ex.getMessage());
            }
        }

        CompoundTag diplomacyTag = tag.getCompound("diplomacy");
        for (String key : diplomacyTag.getAllKeys()) {
            try {
                ResourceLocation raceId = ResourceLocation.parse(key);
                tm.diplomacy.put(raceId, deserializeClan(raceId, diplomacyTag.getCompound(key)));
            } catch (Exception ex) {
                CreRaces.LOGGER.warn("Skipping malformed diplomacy '{}': {}", key, ex.getMessage());
            }
        }

        CompoundTag anchorsTag = tag.getCompound("rootAnchors");
        for (String key : anchorsTag.getAllKeys()) {
            try {
                long rootKey = Long.parseLong(key);
                CompoundTag radTag = anchorsTag.getCompound(key);
                Set<Long> newChunks = new HashSet<>();
                for (long k : radTag.getLongArray("new")) newChunks.add(k);
                Set<Long> preChunks = new HashSet<>();
                for (long k : radTag.getLongArray("pre")) preChunks.add(k);
                tm.rootAnchors.put(rootKey, new RootAnchorData(newChunks, preChunks));
            } catch (Exception ex) {
                CreRaces.LOGGER.warn("Skipping malformed rootAnchor entry '{}': {}", key, ex.getMessage());
            }
        }

        return tm;
    }

    private static CompoundTag serializeClaim(ClaimData c) {
        CompoundTag tag = new CompoundTag();
        tag.putString("race", c.getRaceId().toString());
        tag.putBoolean("persistent", c.isPersistent());
        if (c.getOwnerUUID() != null) tag.putString("owner", c.getOwnerUUID().toString());
        if (c.getPricePaid() > 0) tag.putInt("price", c.getPricePaid());
        return tag;
    }

    private static ClaimData deserializeClaim(CompoundTag tag) {
        ResourceLocation race = ResourceLocation.parse(tag.getString("race"));
        boolean persistent = tag.getBoolean("persistent");
        UUID owner = tag.contains("owner") ? UUID.fromString(tag.getString("owner")) : null;
        int price = tag.contains("price") ? tag.getInt("price") : 0;
        return new ClaimData(race, persistent, owner, price);
    }

    private static CompoundTag serializeClan(ClanData c) {
        CompoundTag tag = new CompoundTag();
        CompoundTag relations = new CompoundTag();
        for (Map.Entry<ResourceLocation, DiplomacyStatus> e : c.getRelations().entrySet()) {
            relations.putString(e.getKey().toString(), e.getValue().name());
        }
        tag.put("relations", relations);
        return tag;
    }

    private static ClanData deserializeClan(ResourceLocation raceId, CompoundTag tag) {
        ClanData c = new ClanData(raceId);
        CompoundTag relations = tag.getCompound("relations");
        for (String key : relations.getAllKeys()) {
            try {
                c.setRelation(ResourceLocation.parse(key), DiplomacyStatus.valueOf(relations.getString(key)));
            } catch (Exception ex) {
                CreRaces.LOGGER.warn("Skipping malformed diplomacy entry '{}'", key);
            }
        }
        return c;
    }
}
