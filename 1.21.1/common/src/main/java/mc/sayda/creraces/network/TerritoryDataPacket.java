package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S2C: sends a chunk grid around the player for the territory map screen.
 */
@SuppressWarnings("null")
public class TerritoryDataPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "territory_data");

    /** Relationship of this chunk to the receiving player. */
    public enum Relation { UNCLAIMED, OWN, ALLIED, ENEMY }

    public static final class ChunkInfo {
        public final int chunkX;
        public final int chunkZ;
        public final Relation relation;
        public final boolean dormant;
        public final String factionName;
        public final String ownerName;

        public ChunkInfo(int cx, int cz, Relation relation, boolean dormant, String factionName) {
            this(cx, cz, relation, dormant, factionName, "");
        }

        public ChunkInfo(int cx, int cz, Relation relation, boolean dormant, String factionName, String ownerName) {
            this.chunkX = cx;
            this.chunkZ = cz;
            this.relation = relation;
            this.dormant = dormant;
            this.factionName = factionName != null ? factionName : "";
            this.ownerName = ownerName != null ? ownerName : "";
        }
    }

    public final List<ChunkInfo> chunks;
    /** Unclaimed chunks in the player's race valid biomes (biome-preview mode). Empty = use adjacency fallback. */
    public final List<Long> biomeClaimableChunks;

    public TerritoryDataPacket(List<ChunkInfo> chunks) {
        this(chunks, new ArrayList<>());
    }

    public TerritoryDataPacket(List<ChunkInfo> chunks, List<Long> biomeClaimableChunks) {
        this.chunks = chunks;
        this.biomeClaimableChunks = biomeClaimableChunks;
    }

    public TerritoryDataPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > 16384) throw new IllegalStateException("Oversized territory packet: " + size);
        chunks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int cx = buf.readInt();
            int cz = buf.readInt();
            Relation rel = buf.readEnum(Relation.class);
            boolean dormant = buf.readBoolean();
            String name = buf.readUtf(64);
            String ownerName = buf.readUtf(48);
            chunks.add(new ChunkInfo(cx, cz, rel, dormant, name, ownerName));
        }
        int bSize = buf.readVarInt();
        if (bSize < 0 || bSize > 65536) throw new IllegalStateException("Oversized biome chunk list: " + bSize);
        biomeClaimableChunks = new ArrayList<>(bSize);
        for (int i = 0; i < bSize; i++) biomeClaimableChunks.add(buf.readLong());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(chunks.size());
        for (ChunkInfo c : chunks) {
            buf.writeInt(c.chunkX);
            buf.writeInt(c.chunkZ);
            buf.writeEnum(c.relation);
            buf.writeBoolean(c.dormant);
            String fn = c.factionName != null ? c.factionName : "";
            buf.writeUtf(fn.length() > 64 ? fn.substring(0, 64) : fn);
            String on = c.ownerName != null ? c.ownerName : "";
            buf.writeUtf(on.length() > 48 ? on.substring(0, 48) : on);
        }
        buf.writeVarInt(biomeClaimableChunks.size());
        for (long key : biomeClaimableChunks) buf.writeLong(key);
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                mc.sayda.creraces.client.screen.TerritoryMapScreen.updateChunks(this);
            });
        });
    }
}
