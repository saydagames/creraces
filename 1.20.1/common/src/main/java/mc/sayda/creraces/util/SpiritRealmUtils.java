package mc.sayda.creraces.util;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.network.BoundaryHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Shared spirit realm transition, used by the JSON set_flag action, the Torii Bell and the realm effects. */
public final class SpiritRealmUtils {

    private SpiritRealmUtils() {
    }

    /**
     * Moves the player in or out of the spirit realm and resyncs visibility.
     * The return point is recorded on the way in only, so a repeated entry never overwrites it.
     * Returns true when the state actually changed.
     */
    public static boolean setInSpiritRealm(Player player, boolean next) {
        return DataUtils.getVariables(player).map(vars -> {
            if (vars.isInSpiritRealm() == next) {
                return false;
            }
            if (next) {
                vars.setReturnX(player.getX());
                vars.setReturnY(player.getY());
                vars.setReturnZ(player.getZ());
                vars.setReturnDim(player.level().dimension().location().toString());
            }
            vars.setInSpiritRealm(next);
            BoundaryHandler.resyncForAllTrackers(player);
            BoundaryHandler.resyncVariables(player, player);
            return true;
        }).orElse(false);
    }

    /** Carries every other player within radius of the origin to the same state. */
    public static void applyToNearby(Player origin, double radius, boolean next) {
        if (radius <= 0) {
            return;
        }
        AABB area = origin.getBoundingBox().inflate(radius);
        for (Player nearby : origin.level().getEntitiesOfClass(Player.class, area)) {
            if (nearby != origin) {
                setInSpiritRealm(nearby, next);
            }
        }
    }
}
