package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.territory.ClaimData;
import mc.sayda.creraces.territory.DiplomacyStatus;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.*;
import java.util.function.Supplier;

/**
 * C2S: client requests a territory data snapshot centered on their position.
 * Server responds with TerritoryDataPacket.
 */
public class RequestTerritoryDataPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "request_territory_data");

    public RequestTerritoryDataPacket() {}
    public RequestTerritoryDataPacket(FriendlyByteBuf buf) {}
    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            BoundaryHandler.sendTerritoryData(player, buildFor(player));
            BoundaryHandler.sendTerrainSample(player, TerrainSamplePacket.buildFor(player));
        });
    }

    /** Builds a TerritoryDataPacket covering a 129×129 chunk area around the player (matches TerrainSamplePacket.RADIUS). */
    public static TerritoryDataPacket buildFor(ServerPlayer player) {
        TerritoryManager tm = TerritoryManager.get();

        ResourceLocation myRace = DataUtils.getVariables(player)
                .map(v -> v.getRace())
                .orElse(null);

        if (myRace == null || myRace.getPath().equals("none")) {
            return new TerritoryDataPacket(java.util.List.of(), java.util.List.of());
        }

        int pCX = player.chunkPosition().x;
        int pCZ = player.chunkPosition().z;
        int radius = TerrainSamplePacket.RADIUS;

        Map<UUID, String> ownerNameCache = new HashMap<>();
        List<TerritoryDataPacket.ChunkInfo> list = new ArrayList<>();
        for (Map.Entry<Long, ClaimData> entry : tm.getClaims().entrySet()) {
            long key = entry.getKey();
            int cx = (int) key;
            int cz = (int) (key >> 32);
            if (Math.abs(cx - pCX) > radius || Math.abs(cz - pCZ) > radius) continue;

            ClaimData claim = entry.getValue();
            ResourceLocation claimRace = claim.getRaceId();

            Race race = RaceRegistry.get(claimRace);
            String fg = race != null ? race.factionGroup() : null;
            String groupKey = (fg != null && !fg.isEmpty()) ? fg
                    : (race != null ? race.id().getPath() : claimRace.getPath());
            String raceName = (groupKey == null || groupKey.isEmpty()) ? claimRace.getPath()
                    : Character.toUpperCase(groupKey.charAt(0)) + groupKey.substring(1);

            UUID ownerUUID = claim.getOwnerUUID();
            if (ownerUUID != null && !ownerNameCache.containsKey(ownerUUID)) {
                ServerPlayer ownerOnline = player.getServer().getPlayerList().getPlayer(ownerUUID);
                if (ownerOnline != null) {
                    ownerNameCache.put(ownerUUID, ownerOnline.getName().getString());
                } else {
                    var optProfile = player.getServer().getProfileCache().get(ownerUUID);
                    ownerNameCache.put(ownerUUID, optProfile.isPresent() ? optProfile.get().getName() : "");
                }
            }
            String ownerName = ownerUUID != null ? ownerNameCache.getOrDefault(ownerUUID, "") : "";

            TerritoryDataPacket.Relation rel;
            if (myRace != null && claimRace.equals(myRace)) {
                rel = TerritoryDataPacket.Relation.OWN;
            } else if (myRace != null && tm.getDiplomacy(myRace, claimRace) == DiplomacyStatus.ALLY) {
                rel = TerritoryDataPacket.Relation.ALLIED;
            } else {
                rel = TerritoryDataPacket.Relation.ENEMY;
            }

            list.add(new TerritoryDataPacket.ChunkInfo(cx, cz, rel, claim.isPersistent(), raceName, ownerName));
        }

        List<Long> biomeClaimable = buildBiomeClaimable(player, tm, pCX, pCZ, radius);
        return new TerritoryDataPacket(list, biomeClaimable);
    }

    private static List<Long> buildBiomeClaimable(ServerPlayer player, TerritoryManager tm,
            int pCX, int pCZ, int radius) {
        ResourceLocation raceId = DataUtils.getVariables(player).map(v -> v.getRace()).orElse(null);
        if (raceId == null) return List.of();
        Race race = RaceRegistry.get(raceId);
        if (race == null || !race.biomePreview() || race.claimValidBiomes().isEmpty()) return List.of();

        List<String> validBiomes = race.claimValidBiomes();
        float threshold = race.claimBiomeThreshold();
        List<Long> result = new ArrayList<>();
        net.minecraft.server.level.ServerLevel level = player.serverLevel();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = pCX + dx;
                int cz = pCZ + dz;
                long key = ChunkPos.asLong(cx, cz);
                if (tm.getClaimAt(new ChunkPos(cx, cz)) != null) continue;

                BlockPos center = new BlockPos(cx * 16 + 8, 64, cz * 16 + 8);
                if (!level.isLoaded(center)) continue;
                int sampleY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        cx * 16 + 8, cz * 16 + 8);
                if (mc.sayda.creraces.engine.BiomeChecker.matchesChunk(level, new ChunkPos(cx, cz), sampleY, validBiomes, threshold))
                    result.add(key);
            }
        }
        return result;
    }
}
