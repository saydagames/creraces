package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

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

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                mc.sayda.creraces.client.render.SpiritRealmRenderer.CLIENT_SPIRIT_FLAME_VISIBLE = spiritFlameVisible;
            });
        });
    }
}
