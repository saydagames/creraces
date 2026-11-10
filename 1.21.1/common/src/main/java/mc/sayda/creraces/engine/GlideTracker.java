package mc.sayda.creraces.engine;

import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.SyncAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shows a player in the elytra gliding pose after a dash, until they land or touch liquid. Only the
 * look changes: they move exactly as they would without it. A dash can start on the ground or
 * underwater, so the pose waits briefly for the player to reach open air.
 */
public final class GlideTracker {
    private static final int OPEN_AIR_WAIT_TICKS = 20;
    private static final int GLIDING = -1;

    /** Per player: ticks left to reach open air, or GLIDING. Only the server thread touches this. */
    private static final Map<UUID, Integer> GLIDES = new HashMap<>();

    private GlideTracker() {
    }

    public static void start(ServerPlayer player) {
        GLIDES.putIfAbsent(player.getUUID(), OPEN_AIR_WAIT_TICKS);
    }

    public static boolean isGliding(ServerPlayer player) {
        Integer state = GLIDES.get(player.getUUID());
        return state != null && state == GLIDING;
    }

    public static void tick(MinecraftServer server) {
        GLIDES.entrySet().removeIf(glide -> {
            ServerPlayer player = server.getPlayerList().getPlayer(glide.getKey());
            boolean wasGliding = glide.getValue() == GLIDING;
            boolean over = player == null || advance(server, player, glide);
            if (over && wasGliding) {
                show(server, glide.getKey(), false);
            }
            return over;
        });
    }

    /** Returns true once the glide is over. */
    private static boolean advance(MinecraftServer server, ServerPlayer player, Map.Entry<UUID, Integer> glide) {
        if (glide.getValue() == GLIDING) {
            return !isInOpenAir(player);
        }
        if (isInOpenAir(player)) {
            glide.setValue(GLIDING);
            show(server, player.getUUID(), true);
            return false;
        }
        glide.setValue(glide.getValue() - 1);
        return glide.getValue() <= 0;
    }

    /** Sent to everyone, so nobody keeps drawing the pose for a player who flew out of range. */
    private static void show(MinecraftServer server, UUID playerId, boolean gliding) {
        SyncAnimationPacket packet = new SyncAnimationPacket(playerId, SyncAnimationPacket.GLIDING, gliding);
        BoundaryHandler.sendToAll(server, SyncAnimationPacket.ID, packet::encode);
    }

    private static boolean isInOpenAir(ServerPlayer player) {
        return !player.onGround() && !player.isPassenger() && !player.getAbilities().flying && !touchesLiquid(player);
    }

    /** Read from the world, as isInWater stays false for races that ignore the fluid's physics. */
    private static boolean touchesLiquid(ServerPlayer player) {
        return !player.level().getFluidState(player.blockPosition()).isEmpty();
    }
}
