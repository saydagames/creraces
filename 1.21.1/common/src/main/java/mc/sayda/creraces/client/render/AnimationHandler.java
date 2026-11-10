package mc.sayda.creraces.client.render;

import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;

/** Client-side record of which players are mid beam cast or in a glide pose, read by the model and renderer mixins. */
public class AnimationHandler {
    private static final Set<UUID> BEAM_CASTING_PLAYERS = ConcurrentHashMap.newKeySet();
    /** When each gliding player started, in level game time. */
    private static final Map<UUID, Long> GLIDING_SINCE = new ConcurrentHashMap<>();

    public static void setBeamCasting(UUID playerId, boolean casting) {
        if (casting) {
            BEAM_CASTING_PLAYERS.add(playerId);
        } else {
            BEAM_CASTING_PLAYERS.remove(playerId);
        }
    }

    public static boolean isCastingBeam(UUID playerId) {
        return BEAM_CASTING_PLAYERS.contains(playerId);
    }

    public static void setGliding(UUID playerId, boolean gliding) {
        if (gliding) {
            GLIDING_SINCE.put(playerId, gameTime());
        } else {
            GLIDING_SINCE.remove(playerId);
        }
    }

    public static boolean isGliding(UUID playerId) {
        return GLIDING_SINCE.containsKey(playerId);
    }

    /** Ticks since the glide pose started; vanilla eases the pose in over these. */
    public static int glideTicks(UUID playerId) {
        Long since = GLIDING_SINCE.get(playerId);
        return since == null ? 0 : (int) (gameTime() - since);
    }

    private static long gameTime() {
        var level = Minecraft.getInstance().level;
        return level != null ? level.getGameTime() : 0;
    }

    public static void clear() {
        BEAM_CASTING_PLAYERS.clear();
        GLIDING_SINCE.clear();
    }
}
