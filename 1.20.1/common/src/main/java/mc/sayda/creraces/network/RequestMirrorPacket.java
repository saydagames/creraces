package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Supplier;

public class RequestMirrorPacket {
    public static final ResourceLocation ID = new ResourceLocation("creraces", "request_mirror");

    public RequestMirrorPacket() {
    }

    public RequestMirrorPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            BoundaryHandler.sendOpenMirror(player);
        });
    }
}
