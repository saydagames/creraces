package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitRegistry;
import net.minecraft.resources.ResourceLocation;

/** Scales the food gained from eating; read through RaceUtils.getFoodMultiplier. */
public class FoodMultiplierTrait implements TraitRegistry.RaceTrait {

    private final ScalingValue multiplier;

    public FoodMultiplierTrait(ScalingValue multiplier) {
        this.multiplier = multiplier;
    }

    public ScalingValue getMultiplier() {
        return multiplier;
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "food_multiplier"),
                json -> new FoodMultiplierTrait(ScalingValue.fromJson(json, "multiplier", 1.0)));
    }
}
