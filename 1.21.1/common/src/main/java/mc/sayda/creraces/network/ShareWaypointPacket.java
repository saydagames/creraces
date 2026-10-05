package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.util.RaceUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * C2S: a kitsune shares one or more gate waypoints with another kitsune. The server relays them
 * as a single WaypointOfferPacket; it never stores them itself, since the whole system stays
 * client-side except for this one hop.
 */
public class ShareWaypointPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "share_waypoint");

    private final UUID targetUuid;
    private final List<SharedGate> gates;

    public ShareWaypointPacket(UUID targetUuid, List<SharedGate> gates) {
        this.targetUuid = targetUuid;
        this.gates = List.copyOf(gates);
    }

    public ShareWaypointPacket(FriendlyByteBuf buf) {
        this.targetUuid = buf.readUUID();
        this.gates = SharedGate.readList(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(targetUuid);
        SharedGate.writeList(buf, gates);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer sender)) {
                return;
            }
            if (!RaceUtils.isKitsune(sender)) {
                return;
            }
            ServerPlayer target = sender.server.getPlayerList().getPlayer(targetUuid);
            if (target == null) {
                sender.displayClientMessage(Component.translatable("msg.creraces.player_not_found"), true);
                return;
            }
            if (!RaceUtils.isKitsune(target)) {
                sender.displayClientMessage(
                        Component.translatable("msg.creraces.waypoint.share_not_kitsune", target.getDisplayName())
                                .withStyle(ChatFormatting.RED),
                        true);
                return;
            }

            List<SharedGate> valid = gates.stream()
                    .filter(gate -> ResourceLocation.tryParse(gate.dimension()) != null)
                    .toList();
            if (valid.isEmpty()) {
                return;
            }

            BoundaryHandler.sendWaypointOffer(target, new WaypointOfferPacket(sender.getGameProfile().getName(), valid));
            sender.displayClientMessage(valid.size() == 1
                    ? Component.translatable("msg.creraces.waypoint.share_sent", target.getDisplayName())
                    : Component.translatable("msg.creraces.waypoint.share_sent_many", valid.size(),
                            target.getDisplayName()),
                    true);
        });
    }
}
