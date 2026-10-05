package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.ClientAccess;
import mc.sayda.creraces.client.screen.HUDEditorScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/** S2C: opens the HUD editor screen. */
public class OpenHUDEditorPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "open_hud_editor");

    public OpenHUDEditorPacket() {}
    public OpenHUDEditorPacket(FriendlyByteBuf buf) {}
    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientAccess.setScreen(new HUDEditorScreen())));
    }
}
