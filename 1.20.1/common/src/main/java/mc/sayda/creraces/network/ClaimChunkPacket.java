package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.function.Supplier;

/**
 * C2S: player requests a chunk claim or unclaim from the territory map.
 */
@SuppressWarnings("null")
public class ClaimChunkPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "claim_chunk");

    public enum ClaimAction { CLAIM, UNCLAIM }

    private final int chunkX;
    private final int chunkZ;
    private final ClaimAction claimAction;

    public ClaimChunkPacket(int chunkX, int chunkZ, ClaimAction claimAction) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.claimAction = claimAction;
    }

    public ClaimChunkPacket(FriendlyByteBuf buf) {
        this.chunkX = buf.readInt();
        this.chunkZ = buf.readInt();
        this.claimAction = buf.readEnum(ClaimAction.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(chunkX);
        buf.writeInt(chunkZ);
        buf.writeEnum(claimAction);
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            ResourceLocation raceId = DataUtils.getVariables(player)
                    .map(IPlayerVariables::getRace)
                    .orElse(null);
            if (raceId == null || raceId.getPath().equals("none")) return;

            TerritoryManager tm = TerritoryManager.get();
            ChunkPos chunk = new ChunkPos(chunkX, chunkZ);

            // Prevent remote claiming: player must be within a reasonable radius.
            int playerCX = player.chunkPosition().x;
            int playerCZ = player.chunkPosition().z;
            int maxDist = mc.sayda.creraces.config.CreRacesConfig.TERRITORY_MAP_CLAIM_MAX_DISTANCE.get();
            if (maxDist >= 0 && (Math.abs(chunkX - playerCX) > maxDist || Math.abs(chunkZ - playerCZ) > maxDist)) {
                BoundaryHandler.sendClaimResponse(player, new ClaimResponsePacket(TerritoryManager.ClaimResultType.OUT_OF_RANGE));
                return;
            }

            TerritoryManager.ClaimResultType result = TerritoryManager.ClaimResultType.ENEMY_TERRITORY;

            switch (claimAction) {
                case CLAIM -> {
                    mc.sayda.creraces.race.Race raceForLeaderCheck = mc.sayda.creraces.race.RaceRegistry.get(raceId);
                    if (raceForLeaderCheck != null && raceForLeaderCheck.enableTerritory()
                            && !mc.sayda.creraces.territory.FactionLeaderManager.isLeader(player)) {
                        BoundaryHandler.sendClaimResponse(player,
                                new ClaimResponsePacket(TerritoryManager.ClaimResultType.NOT_LEADER));
                        return;
                    }
                    int costPerChunk = mc.sayda.creraces.config.CreRacesConfig.TERRITORY_CLAIM_COST_PER_CHUNK.get();
                    mc.sayda.creraces.capability.IPlayerVariables vars$ =
                            DataUtils.getVariables(player).orElse(null);
                    if (costPerChunk > 0 && (vars$ == null || vars$.getCoins() < costPerChunk)) {
                        BoundaryHandler.sendClaimResponse(player,
                                new ClaimResponsePacket(TerritoryManager.ClaimResultType.INSUFFICIENT_COINS));
                        return;
                    }
                    mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(raceId);
                    if (race != null && !race.claimValidBiomes().isEmpty()) {
                        net.minecraft.core.BlockPos checkPos = chunk.getMiddleBlockPosition(64);
                        if (!isBiomeValid(player.serverLevel(), checkPos, race.claimValidBiomes())) {
                            BoundaryHandler.sendClaimResponse(player,
                                    new ClaimResponsePacket(TerritoryManager.ClaimResultType.INVALID_BIOME));
                            return;
                        }
                    }
                    var cr = tm.claimChunk(raceId, chunk, player.getUUID());
                    result = cr.type;
                    if (result == TerritoryManager.ClaimResultType.SUCCESS && costPerChunk > 0) {
                        final int cost = costPerChunk;
                        DataUtils.getVariables(player).ifPresent(v -> {
                            v.setCoins(Math.max(0, v.getCoins() - cost));
                            v.sync(player);
                        });
                    }
                }
                case UNCLAIM -> {
                    mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(raceId);
                    if (race != null && race.enableTerritory()
                            && !mc.sayda.creraces.territory.FactionLeaderManager.isLeader(player)) {
                        BoundaryHandler.sendClaimResponse(player,
                                new ClaimResponsePacket(TerritoryManager.ClaimResultType.NOT_LEADER));
                        return;
                    }
                    mc.sayda.creraces.territory.ClaimData existing = tm.getClaimAt(chunk);
                    if (existing == null || !existing.getRaceId().equals(raceId)) {
                        result = TerritoryManager.ClaimResultType.ENEMY_TERRITORY;
                    } else if (existing.isPersistent()) {
                        result = TerritoryManager.ClaimResultType.ANCHOR_CHUNK;
                    } else {
                        boolean unclaimed = tm.unclaimChunk(raceId, chunk);
                        result = unclaimed ? TerritoryManager.ClaimResultType.UNCLAIM_SUCCESS
                                           : TerritoryManager.ClaimResultType.ENEMY_TERRITORY;
                        if (unclaimed) {
                            int costPerChunk = mc.sayda.creraces.config.CreRacesConfig.TERRITORY_CLAIM_COST_PER_CHUNK.get();
                            if (costPerChunk > 0) {
                                DataUtils.getVariables(player).ifPresent(v -> {
                                    v.setCoins(v.getCoins() + costPerChunk);
                                    v.sync(player);
                                });
                            }
                        }
                    }
                }
            }

            BoundaryHandler.sendClaimResponse(player, new ClaimResponsePacket(result));
        });
    }

    private static boolean isBiomeValid(net.minecraft.server.level.ServerLevel level,
            net.minecraft.core.BlockPos pos, java.util.List<String> validBiomes) {
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome = level.getBiome(pos);
        for (String entry : validBiomes) {
            if (entry.startsWith("#")) {
                try {
                    net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome> tag =
                            net.minecraft.tags.TagKey.create(
                                    net.minecraft.core.registries.Registries.BIOME,
                                    new ResourceLocation(entry.substring(1)));
                    if (biome.is(tag)) return true;
                } catch (Exception ignored) {}
            } else {
                if (biome.unwrapKey().map(k -> k.location().toString().equals(entry)).orElse(false))
                    return true;
            }
        }
        return false;
    }
}
