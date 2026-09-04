package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Supplier;

/**
 * S2C: result of a chunk claim/unclaim request from the territory map.
 */
@SuppressWarnings("null")
public class ClaimResponsePacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "claim_response");

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

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                mc.sayda.creraces.client.screen.TerritoryMapScreen.onClaimResponse(result);
            });
        });
    }
}
