package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.screen.TerritoryMapScreen;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/** S2C: result of a chunk claim/unclaim request from the territory map. */
@SuppressWarnings("null")
public class ClaimResponsePacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "claim_response");

    public final TerritoryManager.ClaimResultType result;

    public ClaimResponsePacket(TerritoryManager.ClaimResultType result) {
        this.result = result;
    }

    public ClaimResponsePacket(FriendlyByteBuf buf) {
        this.result = buf.readEnum(TerritoryManager.ClaimResultType.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(result);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> TerritoryMapScreen.onClaimResponse(result)));
    }
}
