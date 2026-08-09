package mc.sayda.creraces.network;

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
    public static final ResourceLocation ID = new ResourceLocation("creraces", "request_territory_data");

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

    /** Builds a TerritoryDataPacket covering a 65×65 chunk radius around the player. */
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
        int radius = 32;

        // Pre-build owner name map
        Map<UUID, String> ownerNameCache = new HashMap<>();
        for (ClaimData cd : tm.getClaims().values()) {
            UUID ownerUUID = cd.getOwnerUUID();
            if (ownerUUID == null || ownerNameCache.containsKey(ownerUUID)) continue;
            ServerPlayer ownerOnline = player.getServer().getPlayerList().getPlayer(ownerUUID);
            if (ownerOnline != null) {
                ownerNameCache.put(ownerUUID, ownerOnline.getName().getString());
            } else {
                var optProfile = player.getServer().getProfileCache().get(ownerUUID);
                ownerNameCache.put(ownerUUID, optProfile.isPresent() ? optProfile.get().getName() : "");
            }
        }

        List<TerritoryDataPacket.ChunkInfo> list = new ArrayList<>();
        for (Map.Entry<Long, ClaimData> entry : tm.getClaims().entrySet()) {
            long key = entry.getKey();
            int cx = (int) key;
            int cz = (int) (key >> 32);
            if (Math.abs(cx - pCX) > radius || Math.abs(cz - pCZ) > radius) continue;

            ClaimData claim = entry.getValue();
            ResourceLocation claimRace = claim.getRaceId();

            Race race = RaceRegistry.get(claimRace);
            String raceName = race != null ? race.name().getString() : claimRace.getPath();
            String ownerName = claim.getOwnerUUID() != null
                    ? ownerNameCache.getOrDefault(claim.getOwnerUUID(), "") : "";

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

                net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biomeHolder =
                        level.getBiome(center);
                if (matchesBiome(biomeHolder, validBiomes)) result.add(key);
            }
        }
        return result;
    }

    @SuppressWarnings("null")
    private static boolean matchesBiome(
            net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> holder,
            List<String> validBiomes) {
        for (String entry : validBiomes) {
            if (entry.startsWith("#")) {
                try {
                    net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome> tag =
                            net.minecraft.tags.TagKey.create(
                                    net.minecraft.core.registries.Registries.BIOME,
                                    new ResourceLocation(entry.substring(1)));
                    if (holder.is(tag)) return true;
                } catch (Exception ignored) {}
            } else {
                if (holder.unwrapKey().map(k -> k.location().toString().equals(entry)).orElse(false))
                    return true;
            }
        }
        return false;
    }
}
