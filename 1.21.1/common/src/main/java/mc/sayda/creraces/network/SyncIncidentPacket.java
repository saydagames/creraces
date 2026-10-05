package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.ClientAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;
import java.util.function.Supplier;

/** S2C: a player's variables, sent to that player and to everyone tracking them. */
public class SyncIncidentPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "sync_incident");

    private final UUID playerId;
    private final CompoundTag data;

    public SyncIncidentPacket(UUID playerId, CompoundTag data) {
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

    public void handle(Supplier<NetworkManager.PacketContext> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.queue(() -> {
            CreRaces.LOGGER.trace("Handling SyncIncidentPacket for {}", this.playerId);
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientAccess.handleSyncIncident(this.playerId, this.data));
        });
    }
}
