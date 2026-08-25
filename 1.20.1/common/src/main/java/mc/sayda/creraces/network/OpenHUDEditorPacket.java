package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.screen.HUDEditorScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to open the HUD editor screen.
 */
public class OpenHUDEditorPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "open_hud_editor");

    public OpenHUDEditorPacket() {}
    public OpenHUDEditorPacket(FriendlyByteBuf buf) {}
    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                mc.sayda.creraces.client.ClientAccess.setScreen(new HUDEditorScreen());
            });
        });
    }
}
