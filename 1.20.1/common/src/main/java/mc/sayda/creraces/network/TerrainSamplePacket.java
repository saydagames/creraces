package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.screen.TerritoryMapScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Supplier;

/**
 * S2C: per-chunk terrain colours centered on the player, for the territory map background.
 * Each chunk carries SUB x SUB samples stored as packed MapColor bytes (the same encoding
 * MapItemSavedData uses); 0 means no data, either an unloaded chunk or no surface found.
 */
@SuppressWarnings("null")
public class TerrainSamplePacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "terrain_sample");

    /** Sub-samples per chunk axis. CELL(10) / SUB(5) = 2px per sample (clean integer). */
    public static final int SUB  = 5;
    public static final int SUB2 = SUB * SUB; // 25 bytes per chunk

    public static final int RADIUS = 64; // chunks; gives 129x129x25 = ~406KB (one-shot)

    public final int originCX, originCZ;
    public final int width, height;
    /**
     * Layout: [(rz * width + rx) * SUB2 + sy * SUB + sx]
     * 0 = no data, so the client leaves its parchment fallback for that sub-cell.
     */
    public final byte[] colors;

    public TerrainSamplePacket(int originCX, int originCZ, int width, int height, byte[] colors) {
        this.originCX = originCX;
        this.originCZ = originCZ;
        this.width    = width;
        this.height   = height;
        this.colors   = colors;
    }

    public TerrainSamplePacket(FriendlyByteBuf buf) {
        this.originCX = buf.readInt();
        this.originCZ = buf.readInt();
        this.width    = buf.readVarInt();
        this.height   = buf.readVarInt();
        int len = buf.readVarInt();
        int maxLen = (RADIUS * 2 + 1) * (RADIUS * 2 + 1) * SUB2;
        if (len < 0 || len > maxLen) throw new IllegalStateException("Oversized terrain sample packet: " + len);
        this.colors = new byte[len];
        buf.readBytes(this.colors);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(originCX);
        buf.writeInt(originCZ);
        buf.writeVarInt(width);
        buf.writeVarInt(height);
        buf.writeVarInt(colors.length);
        buf.writeBytes(colors);
    }

    public static TerrainSamplePacket buildFor(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        int pcx = player.chunkPosition().x;
        int pcz = player.chunkPosition().z;
        int w = RADIUS * 2 + 1;
        byte[] colors = new byte[w * w * SUB2];

        for (int rz = 0; rz < w; rz++) {
            for (int rx = 0; rx < w; rx++) {
                int cx = pcx - RADIUS + rx;
                int cz = pcz - RADIUS + rz;

                // Skip unloaded chunks; never trigger a chunk load
                if (level.getChunkSource().getChunkNow(cx, cz) == null) continue;

                int baseIdx = (rz * w + rx) * SUB2;
                for (int sy = 0; sy < SUB; sy++) {
                    for (int sx = 0; sx < SUB; sx++) {
                        // Block position within chunk: (sx*16+8)/5 gives 1,4,8,11,14
                        int bx = cx * 16 + (sx * 16 + 8) / SUB;
                        int bz = cz * 16 + (sy * 16 + 8) / SUB;
                        colors[baseIdx + sy * SUB + sx] = sampleBlock(level, bx, bz);
                    }
                }
            }
        }
        return new TerrainSamplePacket(pcx - RADIUS, pcz - RADIUS, w, w, colors);
    }

    private static byte sampleBlock(ServerLevel level, int bx, int bz) {
        try {
            int byC = level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz);
            int byN = level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz - 1);

            // Walk down from the surface to the first block with a map colour
            MapColor color = MapColor.NONE;
            for (int dy = 0; dy <= 5; dy++) {
                int y = byC - 1 - dy;
                if (y < level.getMinBuildHeight()) break;
                BlockPos pos = new BlockPos(bx, y, bz);
                color = level.getBlockState(pos).getMapColor(level, pos);
                if (color != MapColor.NONE) break;
            }
            if (color == MapColor.NONE) return 0;

            // Height-comparison shading: same logic as MapItemSavedData
            MapColor.Brightness brightness;
            if (byC > byN)      brightness = MapColor.Brightness.HIGH;
            else if (byC < byN) brightness = MapColor.Brightness.LOW;
            else                brightness = MapColor.Brightness.NORMAL;

            return color.getPackedId(brightness);
        } catch (Exception e) {
            // One unreadable column only costs a blank map pixel; don't fail the whole snapshot
            return 0;
        }
    }

    /** Packed byte to fully-opaque ARGB. Returns 0 for byte value 0 (no data). */
    public static int packedToArgb(byte packed) {
        return MapColor.getColorFromPackedId(packed & 0xFF);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> TerritoryMapScreen.updateTerrain(this)));
    }
}
