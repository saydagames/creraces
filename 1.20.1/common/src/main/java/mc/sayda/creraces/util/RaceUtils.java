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

    /**
     * Checks if the entity (if it's a player) is immune to the specified potion
     * effect
     * based on their race's negate_effects list.
     */
    public static boolean isImmuneToEffect(net.minecraft.world.entity.LivingEntity entity,
            net.minecraft.resources.ResourceLocation effectId) {
        if (!(entity instanceof Player player))
            return false;

        return DataUtils.getVariables(player).map(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race == null || race.passives() == null)
                return false;

            java.util.List<String> negated = race.passives().immuneToPotionEffects();
            if (negated == null || negated.isEmpty())
                return false;

            String idStr = effectId.toString();
            String path = effectId.getPath();
            for (String blocked : negated) {
                if (blocked.equals(idStr) || blocked.equals(path)) {
                    return true;
                }
            }
            return false;
        }).orElse(false);
    }
}
