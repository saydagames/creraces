package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.screen.TerritoryMapScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** S2C: sends a chunk grid around the player for the territory map screen. */
@SuppressWarnings("null")
public class TerritoryDataPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "territory_data");

    private static final int MAX_CHUNKS = 16384;
    private static final int MAX_BIOME_CHUNKS = 65536;
    private static final int FACTION_NAME_MAX_LEN = 64;
    private static final int OWNER_NAME_MAX_LEN = 48;

    /** Relationship of this chunk to the receiving player. */
    public enum Relation { UNCLAIMED, OWN, ALLIED, ENEMY }

    public static final class ChunkInfo {
        public final int chunkX;
        public final int chunkZ;
        public final Relation relation;
        public final boolean dormant;
        public final String factionName;
        public final String ownerName;

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

    public TerritoryDataPacket(List<ChunkInfo> chunks, List<Long> biomeClaimableChunks) {
        this.chunks = chunks;
        this.biomeClaimableChunks = biomeClaimableChunks;
    }

    public TerritoryDataPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_CHUNKS) throw new IllegalStateException("Oversized territory packet: " + size);
        chunks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int cx = buf.readInt();
            int cz = buf.readInt();
            Relation rel = buf.readEnum(Relation.class);
            boolean dormant = buf.readBoolean();
            String name = buf.readUtf(FACTION_NAME_MAX_LEN);
            String ownerName = buf.readUtf(OWNER_NAME_MAX_LEN);
            chunks.add(new ChunkInfo(cx, cz, rel, dormant, name, ownerName));
        }
        int bSize = buf.readVarInt();
        if (bSize < 0 || bSize > MAX_BIOME_CHUNKS) throw new IllegalStateException("Oversized biome chunk list: " + bSize);
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
            buf.writeUtf(truncate(c.factionName, FACTION_NAME_MAX_LEN));
            buf.writeUtf(truncate(c.ownerName, OWNER_NAME_MAX_LEN));
        }
        buf.writeVarInt(biomeClaimableChunks.size());
        for (long key : biomeClaimableChunks) buf.writeLong(key);
    }

    private static String truncate(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> TerritoryMapScreen.updateChunks(this)));
    }
}
