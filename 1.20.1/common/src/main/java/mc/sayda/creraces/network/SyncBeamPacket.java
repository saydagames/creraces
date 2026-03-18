package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet to sync beam rendering state from server to client.
 */
public class SyncBeamPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "sync_beam");

    private final UUID playerId;
    private final boolean active;
    private final float r, g, b, a;
    private final float radius;
    private final float length;

    public SyncBeamPacket(UUID playerId, boolean active, float r, float g, float b, float a, float radius, float length) {
        this.playerId = playerId;
        this.active = active;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.radius = radius;
        this.length = length;
    }

    public SyncBeamPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        this.active = buf.readBoolean();
        if (this.active) {
            this.r = buf.readFloat();
            this.g = buf.readFloat();
            this.b = buf.readFloat();
            this.a = buf.readFloat();
            this.radius = buf.readFloat();
            this.length = buf.readFloat();
        } else {
            this.r = 0;
            this.g = 0;
            this.b = 0;
            this.a = 0;
            this.radius = 0;
            this.length = 0;
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.playerId);
        buf.writeBoolean(this.active);
        if (this.active) {
            buf.writeFloat(this.r);
            buf.writeFloat(this.g);
            buf.writeFloat(this.b);
            buf.writeFloat(this.a);
            buf.writeFloat(this.radius);
            buf.writeFloat(this.length);
        }
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT,
                    () -> () -> {
                        mc.sayda.creraces.client.render.BeamRenderer.handleSync(this.playerId, this.active, r, g, b, a,
                                radius, length);
                        mc.sayda.creraces.client.render.AnimationHandler.setBeamCasting(this.playerId, this.active);
                    });
        });
    }
}
