package mc.sayda.creraces.client.waypoint;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.registry.ModBlocks;
import mc.sayda.creraces.util.RaceUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Watches the local player's spirit-realm flag and records the nearest torii gate on the rising
 * edge - covers both ringing the bell yourself and being carried through by someone else, since
 * both look identical from here: the flag flips true and a bell is nearby.
 */
public final class WaypointDiscovery {
    private static final double DISCOVERY_RADIUS = 8.0;

    private static boolean wasInSpiritRealm = false;

    private WaypointDiscovery() {
    }

    public static void tick(Player player) {
        boolean isInSpiritRealm = DataUtils.getVariables(player)
                .map(IPlayerVariables::isInSpiritRealm)
                .orElse(false);
        if (isInSpiritRealm && !wasInSpiritRealm && RaceUtils.isKitsune(player)) {
            scanForGate(player);
        }
        wasInSpiritRealm = isInSpiritRealm;
    }

    private static void scanForGate(Player player) {
        Level level = player.level();
        BlockPos origin = player.blockPosition();
        int r = (int) DISCOVERY_RADIUS;
        BlockPos nearest = null;
        double nearestDistSq = DISCOVERY_RADIUS * DISCOVERY_RADIUS;

        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -r, -r), origin.offset(r, r, r))) {
            if (level.getBlockState(pos).is(ModBlocks.TORII_BELL.get())) {
                double distSq = pos.distSqr(origin);
                if (distSq <= nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = pos.immutable();
                }
            }
        }

        if (nearest == null) {
            return;
        }
        WaypointStore.get().discoverIfNew(level.dimension().location().toString(), nearest);
    }
}
