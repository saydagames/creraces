package mc.sayda.creraces.util;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.traits.FoodMultiplierTrait;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.world.entity.player.Player;

public class RaceUtils {
    public static double getFoodMultiplier(Player player) {
        return DataUtils.getVariables(player).map(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race != null && race.traits() != null) {
                for (TraitRegistry.RaceTrait trait : race.traits()) {
                    if (trait instanceof FoodMultiplierTrait fmt) {
                        return fmt.getMultiplier().evaluate(player);
                    }
                }
            }
            return 1.0;
        }).orElse(1.0);
    }
}
