package mc.sayda.creraces.engine;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.TraitRegistry.RaceTrait;
import mc.sayda.creraces.global.GlobalRegistry;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The single view every trait dispatch site iterates: a player's race traits, plus any
 * player-scoped trait blocks declared in a global handler file (which apply regardless of
 * race). This is what lets a trait type registered once work in races, abilities and globals
 * with no per-trigger code.
 */
public final class TraitDispatch {
    private TraitDispatch() {
    }

    public static List<RaceTrait> forPlayer(Player player) {
        List<RaceTrait> raceTraits = DataUtils.getVariables(player)
                .map(vars -> RaceRegistry.get(vars.getRace()))
                .map(Race::traits)
                .orElse(null);
        List<RaceTrait> globalTraits = GlobalRegistry.playerScopedTraits();

        // Some of these sites run every tick per player, so avoid copying when one side is
        // empty - which is the common case (no global handler declares trait blocks).
        if (globalTraits.isEmpty()) {
            return raceTraits != null ? raceTraits : List.of();
        }
        if (raceTraits == null || raceTraits.isEmpty()) {
            return globalTraits;
        }

        List<RaceTrait> combined = new ArrayList<>(raceTraits.size() + globalTraits.size());
        combined.addAll(raceTraits);
        combined.addAll(globalTraits);
        return combined;
    }

    /** Void hooks (onKill, onHit, onItemPickup, onSelect, onHurt, tick, ...). */
    public static void runVoid(String source, Player player, Consumer<RaceTrait> hook) {
        for (RaceTrait trait : forPlayer(player)) {
            try {
                hook.accept(trait);
            } catch (Exception e) {
                reportFailure(source, trait, e);
            }
        }
    }

    /** Boolean hooks that stop and report true on the first trait that handles the event. */
    public static boolean runUntilTrue(String source, Player player, Predicate<RaceTrait> hook) {
        for (RaceTrait trait : forPlayer(player)) {
            try {
                if (hook.test(trait)) {
                    return true;
                }
            } catch (Exception e) {
                reportFailure(source, trait, e);
            }
        }
        return false;
    }

    /** modifyDamageTaken: each trait transforms the running value in turn. */
    public static float runFloatChain(String source, Player player, float initial,
            BiFunction<RaceTrait, Float, Float> hook) {
        float current = initial;
        for (RaceTrait trait : forPlayer(player)) {
            try {
                current = hook.apply(trait, current);
            } catch (Exception e) {
                reportFailure(source, trait, e);
            }
        }
        return current;
    }

    private static void reportFailure(String source, RaceTrait trait, Exception e) {
        CreRaces.LOGGER.error("[{}] trait {} failed - {}", source, trait.getTraitId(), e.getMessage(), e);
    }
}
