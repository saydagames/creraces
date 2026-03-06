package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet to sync tether rendering state from server to client.
 */
public class SyncTetherPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "sync_tether");

    private final UUID casterId;
    private final UUID targetId;
    private final boolean active;
    private final String texture;
    private final float width;

    public SyncTetherPacket(UUID casterId, UUID targetId, boolean active, String texture, float width) {
        this.casterId = casterId;
        this.targetId = targetId;
        this.active = active;
        this.texture = texture;
        this.width = width;
    }

    public SyncTetherPacket(FriendlyByteBuf buf) {
        this.casterId = buf.readUUID();
        this.targetId = buf.readUUID();
        this.active = buf.readBoolean();
        if (this.active) {
            this.texture = buf.readUtf();
            this.width = buf.readFloat();
        } else {
            this.texture = "";
            this.width = 0f;
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.casterId);
        buf.writeUUID(this.targetId);
        buf.writeBoolean(this.active);
        if (this.active) {
            buf.writeUtf(this.texture);
            buf.writeFloat(this.width);
        }
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT,
                    () -> () -> {
                        mc.sayda.creraces.client.render.TetherRenderer.handleSync(this.casterId, this.targetId,
                                this.active, this.texture, this.width);
                    });
        });
    }
}
