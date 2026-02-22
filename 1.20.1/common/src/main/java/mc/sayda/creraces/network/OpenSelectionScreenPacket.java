package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.screen.RaceSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to open the race selection screen.
 */
public class OpenSelectionScreenPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "open_selection_screen");

    public OpenSelectionScreenPacket() {
    }

    public OpenSelectionScreenPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                mc.sayda.creraces.client.ClientAccess.setScreen(new RaceSelectionScreen());
            });
        });
    }
}
