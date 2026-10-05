package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.render.AnimationHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;
import java.util.function.Supplier;

/** S2C: toggles a named player animation on the client. Only "beam_casting" is handled so far. */
public class SyncAnimationPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "sync_animation");

    private static final int ANIMATION_MAX_LEN = 256;

    private final UUID playerId;
    private final String animation;
    private final boolean active;

    public SyncAnimationPacket(UUID playerId, String animation, boolean active) {
        this.playerId = playerId;
        this.animation = animation;
        this.active = active;
    }

    public SyncAnimationPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        this.animation = buf.readUtf(ANIMATION_MAX_LEN);
        this.active = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeUtf(animation, ANIMATION_MAX_LEN);
        buf.writeBoolean(active);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (animation.equals("beam_casting")) {
                EnvExecutor.runInEnv(Env.CLIENT, () -> () -> AnimationHandler.setBeamCasting(playerId, active));
            }
        });
    }
}
