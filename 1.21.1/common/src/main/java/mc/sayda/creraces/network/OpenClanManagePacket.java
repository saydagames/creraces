package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Supplier;

/**
 * S2C: opens the ClanManagementScreen on the client.
 */
public class OpenClanManagePacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "open_clan_manage");

    public OpenClanManagePacket() {}

    public OpenClanManagePacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                mc.sayda.creraces.client.screen.ClanManagementScreen.open();
            });
        });
    }
}
