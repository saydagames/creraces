package mc.sayda.creraces.fabric.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Isolates all direct references to Trinkets' API in their own class, so the class is only ever
 * loaded (and TrinketsApi resolved) when Trinkets is confirmed present. Referencing TrinketsApi
 * directly from CreRacesFabric's beltFinder lambda would throw NoClassDefFoundError as soon as the
 * lambda ran on a Trinkets-less install, since nothing else in that class forced Trinkets to be a
 * hard dependency.
 */
public class TrinketsBeltCompat {
    public static Optional<ItemStack> findBelt(Player player) {
        return dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(player)
                .flatMap(comp -> {
                    var equipped = comp.getEquipped(stack -> stack.getItem() == mc.sayda.creraces.registry.ModItems.ESSENCE_BELT.get());
                    return equipped.isEmpty() ? Optional.<ItemStack>empty() : Optional.of(equipped.get(0).getB());
                });
    }
}
