package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.TraitRegistry;
import net.minecraft.resources.ResourceLocation;

public class FoodMultiplierTrait implements TraitRegistry.RaceTrait {

    private final double multiplier;

    public FoodMultiplierTrait(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "food_multiplier"), json -> {
            double multiplier = json.has("multiplier") ? json.get("multiplier").getAsDouble() : 1.0;
            return new FoodMultiplierTrait(multiplier);
        });
    }
}
