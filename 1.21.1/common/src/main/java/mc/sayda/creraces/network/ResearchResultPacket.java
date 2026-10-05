package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.function.Supplier;

/** S2C: outcome of a research attempt, answered on the client with a success or failure sound. */
public class ResearchResultPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "research_result");

    private final boolean success;

    public ResearchResultPacket(boolean success) {
        this.success = success;
    }

    public ResearchResultPacket(FriendlyByteBuf buf) {
        this.success = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(success);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            double x = minecraft.player.getX();
            double y = minecraft.player.getY();
            double z = minecraft.player.getZ();
            if (success) {
                minecraft.level.playLocalSound(x, y, z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.2f, false);
            } else {
                minecraft.level.playLocalSound(x, y, z, SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.4f, 0.8f, false);
            }
        });
    }
}
