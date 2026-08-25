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
                    mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(raceId);
                    if (race != null && race.enableTerritory()
                            && !mc.sayda.creraces.territory.FactionLeaderManager.isLeader(player)) {
                        BoundaryHandler.sendClaimResponse(player,
                                new ClaimResponsePacket(TerritoryManager.ClaimResultType.NOT_LEADER));
                        return;
                    }
                    int costPerChunk = mc.sayda.creraces.config.CreRacesConfig.TERRITORY_CLAIM_COST_PER_CHUNK.get();
                    IPlayerVariables vars =
                            DataUtils.getVariables(player).orElse(null);
                    if (costPerChunk > 0 && (vars == null || vars.getCoins() < costPerChunk)) {
                        BoundaryHandler.sendClaimResponse(player,
                                new ClaimResponsePacket(TerritoryManager.ClaimResultType.INSUFFICIENT_COINS));
                        return;
                    }
                    if (race != null && !race.claimValidBiomes().isEmpty()) {
                        int sampleBX = chunk.x * 16 + 8;
                        int sampleBZ = chunk.z * 16 + 8;
                        int sampleY = player.serverLevel().getHeight(
                                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                sampleBX, sampleBZ);
                        if (!mc.sayda.creraces.engine.BiomeChecker.matchesChunk(
                                player.serverLevel(), chunk, sampleY, race.claimValidBiomes(), race.claimBiomeThreshold())) {
                            BoundaryHandler.sendClaimResponse(player,
                                    new ClaimResponsePacket(TerritoryManager.ClaimResultType.INVALID_BIOME));
                            return;
                        }
                    }
                    var cr = tm.claimChunk(raceId, chunk, player.getUUID(), costPerChunk);
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
                        final int refund = existing.getPricePaid();
                        boolean unclaimed = tm.unclaimChunk(raceId, chunk);
                        result = unclaimed ? TerritoryManager.ClaimResultType.UNCLAIM_SUCCESS
                                           : TerritoryManager.ClaimResultType.ENEMY_TERRITORY;
                        if (unclaimed && refund > 0) {
                            DataUtils.getVariables(player).ifPresent(v -> {
                                v.setCoins(v.getCoins() + refund);
                                v.sync(player);
                            });
                        }
                    }
                }
            }

            BoundaryHandler.sendClaimResponse(player, new ClaimResponsePacket(result));
        });
    }

}
