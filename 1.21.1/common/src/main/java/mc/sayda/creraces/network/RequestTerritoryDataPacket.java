package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.BiomeChecker;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.territory.ClaimData;
import mc.sayda.creraces.territory.DiplomacyStatus;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * C2S: client requests a territory data snapshot centered on their position.
 * Server responds with TerritoryDataPacket and TerrainSamplePacket.
 */
public class RequestTerritoryDataPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "request_territory_data");

    public RequestTerritoryDataPacket() {}
    public RequestTerritoryDataPacket(FriendlyByteBuf buf) {}
    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
                sendSnapshot(player);
            }
        });
    }

    /** Sends the claim grid and the terrain colours the territory map draws underneath it. */
    public static void sendSnapshot(ServerPlayer player) {
        BoundaryHandler.sendTerritoryData(player, buildFor(player));
        BoundaryHandler.sendTerrainSample(player, TerrainSamplePacket.buildFor(player));
    }

    /** Builds a TerritoryDataPacket covering the same chunk area as TerrainSamplePacket (RADIUS around the player). */
    public static TerritoryDataPacket buildFor(ServerPlayer player) {
        TerritoryManager tm = TerritoryManager.get();

        ResourceLocation myRace = DataUtils.getVariables(player)
                .map(IPlayerVariables::getRace)
                .orElse(null);
        if (myRace == null || myRace.getPath().equals("none")) {
            return new TerritoryDataPacket(List.of(), List.of());
        }

        int pCX = player.chunkPosition().x;
        int pCZ = player.chunkPosition().z;
        int radius = TerrainSamplePacket.RADIUS;

        Map<UUID, String> ownerNames = new HashMap<>();
        List<TerritoryDataPacket.ChunkInfo> list = new ArrayList<>();
        for (Map.Entry<Long, ClaimData> entry : tm.getClaims().entrySet()) {
            long key = entry.getKey();
            int cx = (int) key;
            int cz = (int) (key >> 32);
            if (Math.abs(cx - pCX) > radius || Math.abs(cz - pCZ) > radius) continue;

            ClaimData claim = entry.getValue();
            ResourceLocation claimRace = claim.getRaceId();

            UUID ownerUUID = claim.getOwnerUUID();
            String ownerName = ownerUUID != null
                    ? ownerNames.computeIfAbsent(ownerUUID, id -> lookUpName(player.getServer(), id))
                    : "";

            TerritoryDataPacket.Relation rel;
            if (claimRace.equals(myRace)) {
                rel = TerritoryDataPacket.Relation.OWN;
            } else if (tm.getDiplomacy(myRace, claimRace) == DiplomacyStatus.ALLY) {
                rel = TerritoryDataPacket.Relation.ALLIED;
            } else {
                rel = TerritoryDataPacket.Relation.ENEMY;
            }

            list.add(new TerritoryDataPacket.ChunkInfo(cx, cz, rel, claim.isPersistent(),
                    factionDisplayName(claimRace), ownerName));
        }

        List<Long> biomeClaimable = buildBiomeClaimable(player, myRace, tm, pCX, pCZ, radius);
        return new TerritoryDataPacket(list, biomeClaimable);
    }

    /** Claims are labelled with the owning race's id path, capitalised. */
    private static String factionDisplayName(ResourceLocation raceId) {
        String path = raceId.getPath();
        if (path.isEmpty()) {
            return path;
        }
        return Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    private static String lookUpName(MinecraftServer server, UUID playerId) {
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        if (online != null) {
            return online.getName().getString();
        }
        var profile = server.getProfileCache().get(playerId);
        return profile.isPresent() ? profile.get().getName() : "";
    }

    private static List<Long> buildBiomeClaimable(ServerPlayer player, ResourceLocation raceId, TerritoryManager tm,
            int pCX, int pCZ, int radius) {
        Race race = RaceRegistry.get(raceId);
        if (race == null || !race.biomePreview() || race.claimValidBiomes().isEmpty()) return List.of();

        List<String> validBiomes = race.claimValidBiomes();
        float threshold = race.claimBiomeThreshold();
        List<Long> result = new ArrayList<>();
        ServerLevel level = player.serverLevel();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = pCX + dx;
                int cz = pCZ + dz;
                ChunkPos chunk = new ChunkPos(cx, cz);
                if (tm.getClaimAt(chunk) != null) continue;

                BlockPos center = new BlockPos(cx * 16 + 8, 64, cz * 16 + 8);
                if (!level.isLoaded(center)) continue;
                int sampleY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx * 16 + 8, cz * 16 + 8);
                if (BiomeChecker.matchesChunk(level, chunk, sampleY, validBiomes, threshold)) {
                    result.add(ChunkPos.asLong(cx, cz));
                }
            }
        }
        return result;
    }
}
