package mc.sayda.creraces.util;

/**
 * Accessor interface for FoodData to allow scaling nutrition/saturation
 * from other mixins without illegal casts.
 */
public interface IFoodDataAccessor {
    int creraces$getFoodLevel();

    float creraces$getSaturation();

    void creraces$applyFoodMultiplier(int oldFood, float oldSat, double multiplier);
}
