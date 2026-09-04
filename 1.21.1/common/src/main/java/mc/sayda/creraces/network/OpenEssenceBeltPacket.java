package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.item.EssenceBeltItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to open the essence belt menu.
 */
public class OpenEssenceBeltPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "open_essence_belt");

    public OpenEssenceBeltPacket() {}
    public OpenEssenceBeltPacket(FriendlyByteBuf buf) {}
    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkManager.PacketContext> context) {
        context.get().queue(() -> {
            Player player = context.get().getPlayer();
            if (!(player instanceof ServerPlayer sp)) return;
            EssenceBeltItem.openBeltMenu(sp);
        });
    }
}
