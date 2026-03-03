package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.TraitRegistry;
import net.minecraft.resources.ResourceLocation;

public class FoodMultiplierTrait implements TraitRegistry.RaceTrait {

    private final mc.sayda.creraces.engine.ScalingValue multiplier;

    public FoodMultiplierTrait(mc.sayda.creraces.engine.ScalingValue multiplier) {
        this.multiplier = multiplier;
    }

    public mc.sayda.creraces.engine.ScalingValue getMultiplier() {
        return multiplier;
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "food_multiplier"), json -> {
            mc.sayda.creraces.engine.ScalingValue multiplier = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                    "multiplier", 1.0);
            return new FoodMultiplierTrait(multiplier);
        });
    }
}
