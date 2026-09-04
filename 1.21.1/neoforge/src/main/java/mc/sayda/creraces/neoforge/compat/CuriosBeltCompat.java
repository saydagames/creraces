package mc.sayda.creraces.neoforge.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Isolates all direct references to Curios' API in their own class, so the class is only ever
 * loaded (and CuriosApi resolved) when Curios is confirmed present. Referencing CuriosApi directly
 * from CreRacesNeoForge's beltFinder lambda would throw NoClassDefFoundError as soon as the lambda
 * ran on a Curios-less install, since nothing else in that class forced Curios to be a hard
 * dependency.
 */
public class CuriosBeltCompat {
    public static Optional<ItemStack> findBelt(Player player) {
        return top.theillusivec4.curios.api.CuriosApi
                .getCuriosInventory(player)
                .flatMap(inv -> inv.findFirstCurio(
                        mc.sayda.creraces.registry.ModItems.ESSENCE_BELT.get()))
                .map(slotResult -> slotResult.stack());
    }
}
