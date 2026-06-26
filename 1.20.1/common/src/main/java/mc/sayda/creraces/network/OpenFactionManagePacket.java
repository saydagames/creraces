package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Supplier;

/**
 * S2C: opens the FactionManagementScreen on the client.
 * Faction roster data is delivered separately via FactionUpdatePacket.
 */
public class OpenFactionManagePacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "open_faction_manage");

    public OpenFactionManagePacket() {}

    public OpenFactionManagePacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                mc.sayda.creraces.client.screen.FactionManagementScreen.open();
            });
        });
    }
}
