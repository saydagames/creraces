package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.screen.RaceTeamScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/** S2C: opens the race team screen. */
public class OpenTeamScreenPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "open_team_screen");

    public OpenTeamScreenPacket() {
    }

    public OpenTeamScreenPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> RaceTeamScreen.open()));
    }
}
