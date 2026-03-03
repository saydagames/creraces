package mc.sayda.creraces.client.render;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages custom animation states for player models.
 */
public class AnimationHandler {
    private static final Map<UUID, Boolean> BEAM_CASTING_PLAYERS = new HashMap<>();

    public static void setBeamCasting(UUID playerId, boolean casting) {
        if (casting) {
            BEAM_CASTING_PLAYERS.put(playerId, true);
        } else {
            BEAM_CASTING_PLAYERS.remove(playerId);
        }
    }

    public static boolean isCastingBeam(UUID playerId) {
        return BEAM_CASTING_PLAYERS.getOrDefault(playerId, false);
    }
}
