package mc.sayda.creraces.engine;

import com.google.common.collect.MapMaker;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

/**
 * How charged a player's swing was. Vanilla empties the attack meter before the hit is dealt, so
 * during on_hit the meter always reads as uncharged; the charge is remembered as the swing starts.
 */
public final class AttackCharge {
    /** Weak keys compare by identity, so a singleplayer client's swing never lands on the server's player. */
    private static final Map<Player, Swing> LAST_SWING = new MapMaker().weakKeys().makeMap();

    private record Swing(long tick, float charge) {
    }

    private AttackCharge() {
    }

    /** Called as a melee attack starts, before vanilla resets the meter. */
    public static void remember(Player player) {
        LAST_SWING.put(player, new Swing(player.level().getGameTime(), player.getAttackStrengthScale(0.5f)));
    }

    /** The charge of a swing made this tick, otherwise the attack meter as it stands. */
    public static float of(Player player) {
        Swing swing = LAST_SWING.get(player);
        return swing != null && swing.tick() == player.level().getGameTime()
                ? swing.charge()
                : player.getAttackStrengthScale(0.5f);
    }
}
