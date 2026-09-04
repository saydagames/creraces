package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import mc.sayda.creraces.CreRaces;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncAnimationPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "sync_animation");

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
        this.animation = buf.readUtf(256);
        this.active = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeUtf(animation);
        buf.writeBoolean(active);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (animation.equals("beam_casting")) {
                EnvExecutor.runInEnv(Env.CLIENT,
                    () -> () -> mc.sayda.creraces.client.render.AnimationHandler.setBeamCasting(playerId, active));
            }
        });
    }
}
