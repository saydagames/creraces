package mc.sayda.creraces.network;

import com.mojang.logging.LogUtils;
import mc.sayda.creraces.CreRaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Packet sent from the client to the server to request an initial data sync.
 * This bypasses the PLAYER_JOIN race condition on dedicated servers.
 */
public class RequestSyncPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "request_sync");
    private static final Logger LOGGER = LogUtils.getLogger();

    public RequestSyncPacket() {
    }

    public RequestSyncPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            net.minecraft.world.entity.player.Player player = context.getPlayer();
            if (player instanceof ServerPlayer sp) {
                LOGGER.debug("CreRaces: Received sync request from {}", sp.getScoreboardName());
                mc.sayda.creraces.IncidentResolver.onClientRequestedSync(sp);
            }
        });
    }
}
