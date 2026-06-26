package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * S2C: notifies OFFICER+ that a player wants to join their faction.
 */
@SuppressWarnings("null")
public class JoinRequestNotifyPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "join_request_notify");

    public final UUID applicantUuid;
    public final String applicantName;
    public final ResourceLocation raceId;

    public JoinRequestNotifyPacket(UUID applicantUuid, String applicantName, ResourceLocation raceId) {
        this.applicantUuid = applicantUuid;
        this.applicantName = applicantName;
        this.raceId = raceId;
    }

    public JoinRequestNotifyPacket(FriendlyByteBuf buf) {
        this.applicantUuid = buf.readUUID();
        this.applicantName = buf.readUtf(32);
        this.raceId = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(applicantUuid);
        buf.writeUtf(applicantName);
        buf.writeResourceLocation(raceId);
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                net.minecraft.client.Minecraft.getInstance().gui.getChat()
                        .addMessage(net.minecraft.network.chat.Component.translatable(
                                "creraces.territory.join_request", applicantName));
            });
        });
    }
}
