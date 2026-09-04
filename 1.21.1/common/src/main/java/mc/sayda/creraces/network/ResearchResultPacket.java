package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class ResearchResultPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "research_result");

    private final boolean success;
    @Nullable private final ResourceLocation ability;

    public ResearchResultPacket(boolean success, @Nullable ResourceLocation ability) {
        this.success = success;
        this.ability = ability;
    }

    public ResearchResultPacket(FriendlyByteBuf buf) {
        this.success = buf.readBoolean();
        this.ability = buf.readBoolean() ? buf.readResourceLocation() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(success);
        buf.writeBoolean(ability != null);
        if (ability != null) buf.writeResourceLocation(ability);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (success) {
                mc.level.playLocalSound(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.2f, false);
            } else {
                mc.level.playLocalSound(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        net.minecraft.sounds.SoundEvents.VILLAGER_NO,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, 0.8f, false);
            }
        });
    }
}
