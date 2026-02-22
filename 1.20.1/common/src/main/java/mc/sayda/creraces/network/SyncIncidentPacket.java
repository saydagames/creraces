package mc.sayda.creraces.network;

import com.mojang.logging.LogUtils;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet to sync player variables from server to client.
 */
public class SyncIncidentPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "sync_incident");

    private final UUID playerId;
    private final net.minecraft.nbt.CompoundTag data;

    public SyncIncidentPacket(UUID playerId, net.minecraft.nbt.CompoundTag data) {
        this.playerId = playerId;
        this.data = data;
    }

    public SyncIncidentPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        this.data = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.playerId);
        buf.writeNbt(this.data);
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        ayaSpreadNews(contextSupplier);
    }

    private void ayaSpreadNews(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT,
                    () -> () -> mc.sayda.creraces.client.ClientAccess.handleSyncIncident(this.playerId, this.data));
        });
    }
}
