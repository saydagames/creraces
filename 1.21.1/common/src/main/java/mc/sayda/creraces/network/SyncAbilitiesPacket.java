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

/** S2C: every ability definition, as JSON, so the client can mirror the server's AbilityRegistry. */
public class SyncAbilitiesPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "sync_abilities");

    private final Map<ResourceLocation, String> abilityData;

    public SyncAbilitiesPacket(Map<ResourceLocation, String> abilityData) {
        this.abilityData = abilityData;
    }

    public SyncAbilitiesPacket(FriendlyByteBuf buf) {
        this.abilityData = JsonDefinitions.read(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        JsonDefinitions.write(buf, abilityData);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientAccess.handleAbilitySync(this.abilityData)));
    }
}
