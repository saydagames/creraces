package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.waypoint.WaypointStore;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Supplier;

/**
 * S2C: relays shared gates to the recipient as one offer. Parked in WaypointStore's pending-offer
 * list and surfaced as a chat notice; the recipient accepts or declines the whole offer from their
 * waypoint editor.
 */
public class WaypointOfferPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "waypoint_offer");

    private static final int SENDER_NAME_MAX_LEN = 64;

    private final String senderName;
    private final List<SharedGate> gates;

    public WaypointOfferPacket(String senderName, List<SharedGate> gates) {
        this.senderName = senderName;
        this.gates = List.copyOf(gates);
    }

    public WaypointOfferPacket(FriendlyByteBuf buf) {
        this.senderName = buf.readUtf(SENDER_NAME_MAX_LEN);
        this.gates = SharedGate.readList(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(senderName, SENDER_NAME_MAX_LEN);
        SharedGate.writeList(buf, gates);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            WaypointStore.get().addPendingOffer(senderName, gates);
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                Component message = gates.size() == 1
                        ? Component.translatable("msg.creraces.waypoint.offer_received", senderName,
                                gates.get(0).name())
                        : Component.translatable("msg.creraces.waypoint.offer_received_many", senderName,
                                gates.size());
                minecraft.player.displayClientMessage(message.copy().withStyle(ChatFormatting.GOLD), false);
            }
        }));
    }
}
