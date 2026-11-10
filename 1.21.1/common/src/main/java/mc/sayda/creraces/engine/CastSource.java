package mc.sayda.creraces.engine;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Who an ability's actions act from. Normally that is the casting player; while a clone repeats an
 * ability it is the clone. The "self" entity, "self" and "look" positions and the aim of raycasts,
 * beams and projectiles then mean the clone, while the player stays the caster for stats, teams
 * and ownership. Actions that only make sense on the real player (resources, slots, morphing, ...)
 * are skipped during such a replay; see ActionRegistry.
 */
public final class CastSource {
    private static final ThreadLocal<Replay> CURRENT = new ThreadLocal<>();

    /** A clone repeating a cast: where it acts from, and how hard its damage hits. */
    public record Replay(LivingEntity source, double damageMultiplier) {
    }

    private CastSource() {
    }

    @Nullable
    public static Replay current() {
        return CURRENT.get();
    }

    public static boolean isReplay() {
        return CURRENT.get() != null;
    }

    /** The entity actions treat as "self": the clone repeating the cast, else the caster. */
    public static LivingEntity self(Player caster) {
        Replay replay = CURRENT.get();
        return replay != null ? replay.source() : caster;
    }

    /** Scales the damage of damage actions; 1 outside a replay. */
    public static double damageMultiplier() {
        Replay replay = CURRENT.get();
        return replay != null ? replay.damageMultiplier() : 1.0;
    }

    /** Runs {@code body} inside {@code replay} (none when null), then restores what was current before. */
    public static void run(@Nullable Replay replay, Runnable body) {
        Replay previous = CURRENT.get();
        CURRENT.set(replay);
        try {
            body.run();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
