package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.ClientAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.Supplier;

/** S2C: every race definition, as JSON, so the client can mirror the server's RaceRegistry. */
public class SyncRacesPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "sync_races");

    private final Map<ResourceLocation, String> raceData;

    public SyncRacesPacket(Map<ResourceLocation, String> raceData) {
        this.raceData = raceData;
    }

    public SyncRacesPacket(FriendlyByteBuf buf) {
        this.raceData = JsonDefinitions.read(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        JsonDefinitions.write(buf, raceData);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientAccess.handleRaceSync(this.raceData)));
    }
}
