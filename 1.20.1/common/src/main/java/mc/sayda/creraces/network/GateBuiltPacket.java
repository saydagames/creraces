package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.waypoint.WaypointStore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * S2C: tells the kitsune who just finished a gate to record it as a waypoint immediately,
 * instead of waiting for them to walk through it. Silent - no chat message, no editor - matching
 * how transiting an already-known gate discovers it.
 */
public class GateBuiltPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "gate_built");

    private static final int DIMENSION_MAX_LEN = 64;

    private final String dimension;
    private final BlockPos pos;

    public GateBuiltPacket(String dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos;
    }

    public GateBuiltPacket(FriendlyByteBuf buf) {
        this.dimension = buf.readUtf(DIMENSION_MAX_LEN);
        this.pos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimension, DIMENSION_MAX_LEN);
        buf.writeBlockPos(pos);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.queue(() -> EnvExecutor.runInEnv(Env.CLIENT,
                () -> () -> WaypointStore.get().discoverIfNew(dimension, pos)));
    }
}
