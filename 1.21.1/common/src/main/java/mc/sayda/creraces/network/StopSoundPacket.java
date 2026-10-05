package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import java.util.function.Supplier;

/** S2C: stops a sound that is still playing on the client. */
public class StopSoundPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "stop_sound");

    private final ResourceLocation soundId;
    private final SoundSource source;

    public StopSoundPacket(ResourceLocation soundId, SoundSource source) {
        this.soundId = soundId;
        this.source = source;
    }

    public StopSoundPacket(FriendlyByteBuf buf) {
        this.soundId = buf.readResourceLocation();
        this.source = buf.readEnum(SoundSource.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(soundId);
        buf.writeEnum(source);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT,
                () -> () -> Minecraft.getInstance().getSoundManager().stop(soundId, source)));
    }
}
