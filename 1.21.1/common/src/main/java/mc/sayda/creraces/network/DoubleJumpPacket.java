package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;

import java.util.function.Supplier;

/**
 * Packet sent from client to server when a double jump occurs.
 * The server then broadcasts sound and particles to all nearby players.
 */
@SuppressWarnings("null")
public class DoubleJumpPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "double_jump");

    public DoubleJumpPacket() {
    }

    public DoubleJumpPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
                // Rate limit: enforce a per-player cooldown to prevent packet spam
                var data = ((mc.sayda.creraces.util.IPersistentDataAccessor) player).creraces$getPersistentData();
                long now = player.level().getGameTime();
                long lastJump = data.getLong("creraces:last_double_jump");
                if (now - lastJump < mc.sayda.creraces.config.CreRacesConfig.DOUBLE_JUMP_COOLDOWN_TICKS.get()) {
                    return;
                }
                data.putLong("creraces:last_double_jump", now);

                // Validate: player must not be on the ground
                if (player.onGround()) {
                    return;
                }

                ServerLevel level = player.serverLevel();
                
                // Broadcast sound to trackers (except the jumper, who plays it locally)
                level.playSound(player, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.5f, 1.5f);

                // Broadcast particles to trackers
                level.sendParticles(ParticleTypes.CLOUD, 
                        player.getX(), player.getY(), player.getZ(), 
                        10, 0.2, 0.2, 0.2, 0.02);
            }
        });
    }
}
