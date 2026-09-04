package mc.sayda.creraces.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.function.Function;

public class PlatformServices {
    public static Function<ItemStack, Integer> burnTimeHandler = stack -> 0;
    public static Function<Player, Optional<ItemStack>> beltFinder = player -> Optional.empty();

    public static int getBurnTime(ItemStack stack) {
        return burnTimeHandler.apply(stack);
    }

    public static Optional<ItemStack> findBelt(Player player) {
        return beltFinder.apply(player);
    }
}
