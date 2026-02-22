package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.screen.DebugScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to open the Debug Screen.
 */
public class OpenDebugScreenPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "open_debug_screen");

    public OpenDebugScreenPacket() {
    }

    public OpenDebugScreenPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                mc.sayda.creraces.client.ClientAccess.setScreen(new DebugScreen());
            });
        });
    }
}
