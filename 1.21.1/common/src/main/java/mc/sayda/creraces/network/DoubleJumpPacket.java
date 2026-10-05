package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.function.Supplier;

/**
 * C2S: the client performed a double jump. The server only broadcasts its sound and particles
 * to nearby players; the jump itself already happened client-side.
 */
@SuppressWarnings("null")
public class DoubleJumpPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "double_jump");

    private static final String LAST_JUMP_KEY = "creraces:last_double_jump";

    public DoubleJumpPacket() {
    }

    public DoubleJumpPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            // Per-player cooldown so a spammed packet can't flood nearby players with effects
            CompoundTag data = ((IPersistentDataAccessor) player).creraces$getPersistentData();
            long now = player.level().getGameTime();
            if (now - data.getLong(LAST_JUMP_KEY) < CreRacesConfig.DOUBLE_JUMP_COOLDOWN_TICKS.get()) {
                return;
            }
            data.putLong(LAST_JUMP_KEY, now);

            if (player.onGround()) {
                return;
            }

            ServerLevel level = player.serverLevel();
            // Passing the jumper excludes them: they already played the sound locally
            level.playSound(player, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.5f, 1.5f);
            level.sendParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY(), player.getZ(),
                    10, 0.2, 0.2, 0.2, 0.02);
        });
    }
}
