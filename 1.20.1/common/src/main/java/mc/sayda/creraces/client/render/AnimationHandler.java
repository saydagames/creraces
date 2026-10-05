package mc.sayda.creraces.client.render;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side record of which players are mid beam cast, read by HumanoidModelMixin to pose them. */
public class AnimationHandler {
    private static final Set<UUID> BEAM_CASTING_PLAYERS = ConcurrentHashMap.newKeySet();

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

    public static void clear() {
        BEAM_CASTING_PLAYERS.clear();
    }
}
