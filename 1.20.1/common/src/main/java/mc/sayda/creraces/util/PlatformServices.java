package mc.sayda.creraces.util;

import net.minecraft.world.item.ItemStack;
import java.util.function.Function;

public class PlatformServices {
    public static Function<ItemStack, Integer> burnTimeHandler = stack -> 0;

    public static int getBurnTime(ItemStack stack) {
        return burnTimeHandler.apply(stack);
    }
}
