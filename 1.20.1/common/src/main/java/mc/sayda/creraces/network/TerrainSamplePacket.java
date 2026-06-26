package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Supplier;

/**
 * S2C: per-chunk terrain color snapshot centered on the player.
 *
 * Resolution: SUB×SUB sub-samples per chunk, each stored as a packed MapColor
 * byte (same encoding as MapItemSavedData). 0 = no data (chunk unloaded).
 *
 * Client renders each sub-sample as a (CELL/SUB)×(CELL/SUB) pixel block, giving
 * proper terrain detail inside each chunk cell.
 */
@SuppressWarnings("null")
public class TerrainSamplePacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "terrain_sample");

    /** Sub-samples per chunk axis. CELL(10) / SUB(5) = 2px per sample — clean integer. */
    public static final int SUB  = 5;
    public static final int SUB2 = SUB * SUB; // 25 bytes per chunk

    private static final int RADIUS = 64; // chunks — gives 129×129×25 ≈ 406KB (one-shot)

    public final int originCX, originCZ;
    public final int width, height;
    /**
     * Layout: [(rz * width + rx) * SUB2 + sy * SUB + sx]
     * 0 = no data → client leaves parchment fallback for that sub-cell.
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

    // ── Server-side factory ───────────────────────────────────────────────────

    public static TerrainSamplePacket buildFor(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        int pcx = player.chunkPosition().x;
        int pcz = player.chunkPosition().z;
        int w = RADIUS * 2 + 1; // 129
        byte[] colors = new byte[w * w * SUB2];

        for (int rz = 0; rz < w; rz++) {
            for (int rx = 0; rx < w; rx++) {
                int cx = pcx - RADIUS + rx;
                int cz = pcz - RADIUS + rz;

                // Skip unloaded chunks — never trigger a chunk load
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

            // Walk down from surface to find a non-NONE MapColor
            MapColor color = MapColor.NONE;
            for (int dy = 0; dy <= 5; dy++) {
                int y = byC - 1 - dy;
                if (y < level.getMinBuildHeight()) break;
                BlockPos pos = new BlockPos(bx, y, bz);
                color = level.getBlockState(pos).getMapColor(level, pos);
                if (color != MapColor.NONE) break;
            }
            if (color == MapColor.NONE) return 0;

            // Height-comparison shading — same logic as MapItemSavedData
            MapColor.Brightness brightness;
            if (byC > byN)      brightness = MapColor.Brightness.HIGH;
            else if (byC < byN) brightness = MapColor.Brightness.LOW;
            else                brightness = MapColor.Brightness.NORMAL;

            return color.getPackedId(brightness);
        } catch (Exception e) {
            return 0;
        }
    }

    // ── Client-side color conversion ──────────────────────────────────────────

    /** Packed byte → fully-opaque ARGB. Returns 0 for byte value 0 (no data). */
    public static int packedToArgb(byte packed) {
        return MapColor.getColorFromPackedId(packed & 0xFF);
    }

    // ── Packet handler ────────────────────────────────────────────────────────

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() ->
                dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () ->
                        mc.sayda.creraces.client.screen.TerritoryMapScreen.updateTerrain(this)));
    }
}
