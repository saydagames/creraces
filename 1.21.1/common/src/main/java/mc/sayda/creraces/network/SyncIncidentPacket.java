package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet to sync player variables from server to client.
 */
public class SyncIncidentPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "sync_incident");

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

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.queue(() -> {
            mc.sayda.creraces.CreRaces.LOGGER.trace("SyncIncidentPacket: [TRACE] Handling packet for UUID {}",
                    this.playerId);
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT,
                    () -> () -> mc.sayda.creraces.client.ClientAccess.handleSyncIncident(this.playerId, this.data));
        });
    }
}
