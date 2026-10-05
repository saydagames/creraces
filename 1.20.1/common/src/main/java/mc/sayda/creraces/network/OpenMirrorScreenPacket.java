package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.ClientAccess;
import mc.sayda.creraces.client.screen.DynamicMirrorScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * S2C: opens the mirror screen. It holds no slots, so it is a plain client screen rather than a
 * container menu, opened the same way as the other Open*ScreenPacket types.
 */
public class OpenMirrorScreenPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "open_mirror_screen");

    public OpenMirrorScreenPacket() {
    }

    public OpenMirrorScreenPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientAccess.setScreen(new DynamicMirrorScreen())));
    }
}
