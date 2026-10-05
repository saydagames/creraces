package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.IncidentResolver;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/**
 * C2S: the client asks for its initial data sync once it is ready for it. Syncing from
 * PLAYER_JOIN alone races the client's setup on dedicated servers.
 */
public class RequestSyncPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "request_sync");

    public RequestSyncPacket() {
    }

    public RequestSyncPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer sp) {
                IncidentResolver.onClientRequestedSync(sp);
            }
        });
    }
}
