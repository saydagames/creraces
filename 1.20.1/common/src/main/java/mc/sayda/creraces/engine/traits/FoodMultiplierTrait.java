package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.engine.condition.ConditionRegistry;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Multiplies the food and saturation the player gets from eating by "multiplier" while "condition"
 * holds. Several of these multiply together (see RaceUtils.getFoodMultiplier).
 */
public class FoodMultiplierTrait implements TraitRegistry.RaceTrait {
    private final ScalingValue multiplier;
    @Nullable
    private final Condition condition;

    FoodMultiplierTrait(JsonObject json) {
        this.multiplier = ScalingValue.fromJson(json, "multiplier", 1.0);
        this.condition = ConditionRegistry.optional(json, "condition");
    }

    public ScalingValue getMultiplier() {
        return multiplier;
    }

    public boolean applies(Player player) {
        return condition == null || condition.evaluate(player, null, null, null);
    }

    public static void register() {
        TraitRegistry.register(CreRaces.id("food_multiplier"), FoodMultiplierTrait::new);
    }
}
