package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Packet to clear remote documentation cache on the client.
 */
public class ClearRemoteCachePacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "clear_remote_cache");

    public ClearRemoteCachePacket() {
    }

    public ClearRemoteCachePacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                mc.sayda.creraces.util.DocCache.clear();
                mc.sayda.creraces.util.RemoteDocFetcher.clearCache();
            });
        });
    }
}
