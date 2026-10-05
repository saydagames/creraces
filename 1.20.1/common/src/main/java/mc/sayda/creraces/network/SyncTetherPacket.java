package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.render.TetherRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;
import java.util.function.Supplier;

/** S2C: starts or stops rendering a tether between two entities. */
public class SyncTetherPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "sync_tether");

    private static final int TEXTURE_MAX_LEN = 512;

    private final UUID casterId;
    private final UUID targetId;
    private final boolean active;
    private final String texture;
    private final float width;
    private final boolean effects;

    public SyncTetherPacket(UUID casterId, UUID targetId, boolean active, String texture, float width, boolean effects) {
        this.casterId = casterId;
        this.targetId = targetId;
        this.active = active;
        this.texture = texture;
        this.width = width;
        this.effects = effects;
    }

    public SyncTetherPacket(FriendlyByteBuf buf) {
        this.casterId = buf.readUUID();
        this.targetId = buf.readUUID();
        this.active = buf.readBoolean();
        if (this.active) {
            this.texture = buf.readUtf(TEXTURE_MAX_LEN);
            this.width = buf.readFloat();
            this.effects = buf.readBoolean();
        } else {
            this.texture = "";
            this.width = 0f;
            this.effects = false;
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.casterId);
        buf.writeUUID(this.targetId);
        buf.writeBoolean(this.active);
        if (this.active) {
            buf.writeUtf(this.texture, TEXTURE_MAX_LEN);
            buf.writeFloat(this.width);
            buf.writeBoolean(this.effects);
        }
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> TetherRenderer.handleSync(
                this.casterId, this.targetId, this.active, this.texture, this.width, this.effects)));
    }
}
