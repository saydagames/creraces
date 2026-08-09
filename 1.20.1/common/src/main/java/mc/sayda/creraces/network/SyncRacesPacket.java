package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.race.RaceManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Syncs the entire RaceRegistry (in JSON form) from server to client.
 */
public class SyncRacesPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "sync_races");
    private final Map<ResourceLocation, String> raceData;

    public SyncRacesPacket(Map<ResourceLocation, String> raceData) {
        this.raceData = raceData;
    }

    public SyncRacesPacket(FriendlyByteBuf buf) {
        this.raceData = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            this.raceData.put(buf.readResourceLocation(), buf.readUtf(262144));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(raceData.size());
        raceData.forEach((id, json) -> {
            buf.writeResourceLocation(id);
            buf.writeUtf(json, 262144);
        });
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                mc.sayda.creraces.client.ClientAccess.handleRaceSync(this.raceData);
            });
        });
    }
}
