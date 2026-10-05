package mc.sayda.creraces.util;

import mc.sayda.creraces.client.waypoint.Waypoint;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/** Loader-specific hooks. Each loader assigns these at startup; the defaults are safe no-ops. */
public class PlatformServices {
    public static Function<ItemStack, Integer> burnTimeHandler = stack -> 0;
    public static Function<Player, Optional<ItemStack>> beltFinder = player -> Optional.empty();

    /**
     * Reassigned by {@code CreRacesJourneyMapPlugin} when JourneyMap is loaded. Called with the
     * gate waypoints that should currently be visible (empty to clear); no-op otherwise.
     */
    public static Consumer<List<Waypoint>> waypointSink = waypoints -> {
    };

    /** True once the JourneyMap plugin above has been initialized, so the HUD renderer can defer to it. */
    public static BooleanSupplier journeyMapPresent = () -> false;

    public static int getBurnTime(ItemStack stack) {
        return burnTimeHandler.apply(stack);
    }

    public static Optional<ItemStack> findBelt(Player player) {
        return beltFinder.apply(player);
    }
}
