package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.render.SpiritRealmRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/** S2C: mirrors the spirit flame visibility gamerule to the client renderer. */
public class SyncGamerulePacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "sync_gamerule");

    private final boolean spiritFlameVisible;

    public SyncGamerulePacket(boolean spiritFlameVisible) {
        this.spiritFlameVisible = spiritFlameVisible;
    }

    public SyncGamerulePacket(FriendlyByteBuf buf) {
        this.spiritFlameVisible = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(spiritFlameVisible);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT,
                () -> () -> SpiritRealmRenderer.CLIENT_SPIRIT_FLAME_VISIBLE = spiritFlameVisible));
    }
}
