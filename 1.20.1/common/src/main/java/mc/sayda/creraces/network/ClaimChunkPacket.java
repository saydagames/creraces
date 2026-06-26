package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.territory.FactionData;
import mc.sayda.creraces.territory.FactionRank;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * C2S: player requests a chunk claim, unclaim, or transfer from the territory map.
 */
@SuppressWarnings("null")
public class ClaimChunkPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "claim_chunk");

    public enum ClaimAction { CLAIM, UNCLAIM, REQUEST_TRANSFER }

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
            TerritoryManager tm = TerritoryManager.get();
            UUID playerId = player.getUUID();
            if (!tm.hasFaction(playerId)) return;

            UUID factionId = tm.getFactionId(playerId);
            FactionData faction = tm.getFaction(factionId);
            if (faction == null) return;
            FactionRank rank = faction.getRank(playerId);
            if (rank == null) return;

            ChunkPos chunk = new ChunkPos(chunkX, chunkZ);

            // Prevent remote claiming: player must be within a reasonable radius of the
            // chunk they're targeting via the map UI.
            int playerCX = player.chunkPosition().x;
            int playerCZ = player.chunkPosition().z;
            int maxDist = mc.sayda.creraces.config.CreRacesConfig.TERRITORY_MAP_CLAIM_MAX_DISTANCE.get();
            if (maxDist >= 0 && (Math.abs(chunkX - playerCX) > maxDist || Math.abs(chunkZ - playerCZ) > maxDist)) {
                BoundaryHandler.sendClaimResponse(player, new ClaimResponsePacket(TerritoryManager.ClaimResultType.INVALID_RANK));
                return;
            }

            TerritoryManager.ClaimResultType result;

            switch (claimAction) {
                case CLAIM -> {
                    mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(faction.getRaceId());
                    if (race != null && !race.claimValidBiomes().isEmpty()) {
                        net.minecraft.core.BlockPos checkPos = chunk.getMiddleBlockPosition(64);
                        // Always use the overworld for biome validation regardless of which
                        // dimension the requesting player is currently in.
                        net.minecraft.server.level.ServerLevel biomeLevel =
                                player.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
                        if (biomeLevel == null) biomeLevel = player.serverLevel();
                        if (!isBiomeValid(biomeLevel, checkPos, race.claimValidBiomes())) {
                            result = TerritoryManager.ClaimResultType.INVALID_BIOME;
                            break;
                        }
                    }
                    var cr = tm.claimAdjacentChunk(factionId, chunk, rank, player.getUUID());
                    result = cr.type;
                }
                case UNCLAIM -> result = tm.unclaimChunk(factionId, chunk, rank)
                        ? TerritoryManager.ClaimResultType.SUCCESS
                        : TerritoryManager.ClaimResultType.ENEMY_TERRITORY;
                case REQUEST_TRANSFER -> {
                    // Notify the owning faction's officers of the transfer request
                    var claim = tm.getClaimAt(chunk);
                    if (claim != null) {
                        notifyTransferRequest(player, claim.getFactionId());
                    }
                    result = TerritoryManager.ClaimResultType.PARTIAL;
                }
                default -> result = TerritoryManager.ClaimResultType.ENEMY_TERRITORY;
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

    private static void notifyTransferRequest(ServerPlayer requester, UUID targetFactionId) {
        FactionData targetFaction = TerritoryManager.get().getFaction(targetFactionId);
        if (targetFaction == null) return;
        var pkt = new JoinRequestNotifyPacket(requester.getUUID(),
                requester.getName().getString(),
                mc.sayda.creraces.capability.DataUtils.getVariables(requester)
                        .map(mc.sayda.creraces.capability.IPlayerVariables::getRace)
                        .orElse(new ResourceLocation("creraces", "none")));
        for (java.util.Map.Entry<UUID, FactionRank> e : targetFaction.getMembers().entrySet()) {
            if (!e.getValue().isAtLeast(FactionRank.OFFICER)) continue;
            ServerPlayer officer = requester.getServer().getPlayerList().getPlayer(e.getKey());
            if (officer != null) BoundaryHandler.sendJoinRequestNotify(officer, pkt);
        }
    }
}
