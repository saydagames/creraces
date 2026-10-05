package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.BiomeChecker;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.territory.ClaimData;
import mc.sayda.creraces.territory.FactionLeaderManager;
import mc.sayda.creraces.territory.TerritoryManager;
import mc.sayda.creraces.territory.TerritoryManager.ClaimResultType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.function.Supplier;

/** C2S: player requests a chunk claim or unclaim from the territory map. */
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

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            IPlayerVariables vars = DataUtils.getVariables(player).orElse(null);
            ResourceLocation raceId = vars != null ? vars.getRace() : null;
            if (raceId == null || raceId.getPath().equals("none")) return;

            // Prevent remote claiming: player must be within a reasonable radius.
            int maxDist = CreRacesConfig.TERRITORY_MAP_CLAIM_MAX_DISTANCE.get();
            if (maxDist >= 0 && (Math.abs(chunkX - player.chunkPosition().x) > maxDist
                    || Math.abs(chunkZ - player.chunkPosition().z) > maxDist)) {
                BoundaryHandler.sendClaimResponse(player, new ClaimResponsePacket(ClaimResultType.OUT_OF_RANGE));
                return;
            }

            Race race = RaceRegistry.get(raceId);
            if (race != null && race.enableTerritory() && !FactionLeaderManager.isLeader(player)) {
                BoundaryHandler.sendClaimResponse(player, new ClaimResponsePacket(ClaimResultType.NOT_LEADER));
                return;
            }

            ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
            ClaimResultType result = switch (claimAction) {
                case CLAIM -> claim(player, vars, raceId, race, chunk);
                case UNCLAIM -> unclaim(player, vars, raceId, chunk);
            };
            BoundaryHandler.sendClaimResponse(player, new ClaimResponsePacket(result));
        });
    }

    private static ClaimResultType claim(ServerPlayer player, IPlayerVariables vars, ResourceLocation raceId,
            Race race, ChunkPos chunk) {
        int costPerChunk = CreRacesConfig.TERRITORY_CLAIM_COST_PER_CHUNK.get();
        if (costPerChunk > 0 && vars.getCoins() < costPerChunk) {
            return ClaimResultType.INSUFFICIENT_COINS;
        }
        if (race != null && !race.claimValidBiomes().isEmpty()) {
            ServerLevel level = player.serverLevel();
            int sampleY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    chunk.x * 16 + 8, chunk.z * 16 + 8);
            if (!BiomeChecker.matchesChunk(level, chunk, sampleY, race.claimValidBiomes(), race.claimBiomeThreshold())) {
                return ClaimResultType.INVALID_BIOME;
            }
        }

        ClaimResultType result = TerritoryManager.get().claimChunk(raceId, chunk, player.getUUID(), costPerChunk).type;
        if (result == ClaimResultType.SUCCESS && costPerChunk > 0) {
            vars.setCoins(Math.max(0, vars.getCoins() - costPerChunk));
            vars.sync(player);
        }
        return result;
    }

    private static ClaimResultType unclaim(ServerPlayer player, IPlayerVariables vars, ResourceLocation raceId,
            ChunkPos chunk) {
        TerritoryManager tm = TerritoryManager.get();
        ClaimData existing = tm.getClaimAt(chunk);
        if (existing == null || !existing.getRaceId().equals(raceId)) {
            return ClaimResultType.ENEMY_TERRITORY;
        }
        if (existing.isPersistent()) {
            return ClaimResultType.ANCHOR_CHUNK;
        }

        int refund = existing.getPricePaid();
        if (!tm.unclaimChunk(raceId, chunk)) {
            return ClaimResultType.ENEMY_TERRITORY;
        }
        if (refund > 0) {
            vars.setCoins(vars.getCoins() + refund);
            vars.sync(player);
        }
        return ClaimResultType.UNCLAIM_SUCCESS;
    }
}
