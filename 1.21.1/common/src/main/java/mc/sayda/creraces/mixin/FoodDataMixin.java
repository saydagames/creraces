package mc.sayda.creraces.mixin;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.util.IFoodDataAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(FoodData.class)
public class FoodDataMixin implements IFoodDataAccessor {

    @Shadow
    private int foodLevel;
    @Shadow
    private float saturationLevel;
    @Shadow
    private float exhaustionLevel;

    @Override
    public int creraces$getFoodLevel() {
        return foodLevel;
    }

    @Override
    public void creraces$setFoodLevel(int food) {
        this.foodLevel = food;
    }

    @Override
    public float creraces$getSaturation() {
        return saturationLevel;
    }

    @Override
    public void creraces$setSaturation(float saturation) {
        this.saturationLevel = saturation;
    }

    /** Called after vanilla eat(): scales only what was just gained, so a 2x multiplier doubles the gain. */
    @Override
    public void creraces$applyFoodMultiplier(int oldFood, float oldSat, double multiplier) {
        int foodGained = foodLevel - oldFood;
        float satGained = saturationLevel - oldSat;
        int adjustedFood = (int) Math.round(foodGained * multiplier);
        float adjustedSat = (float) (satGained * multiplier);
        foodLevel = oldFood + adjustedFood;
        saturationLevel = Math.min(oldSat + adjustedSat, (float) foodLevel);
    }

    /** The ticking player's regeneration_multiplier, read once at the start of each tick. */
    @Unique
    private float creraces$regenerationMultiplier = 1.0f;

    /**
     * Reads the race passives for this tick: no_hunger_drain wipes exhaustion so food never drops,
     * and regeneration_multiplier is kept for the heal below.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void creraces$readRacePassives(Player player, CallbackInfo ci) {
        creraces$regenerationMultiplier = 1.0f;
        if (player.level().isClientSide())
            return;

        Race race = DataUtils.getVariables(player).map(vars -> RaceRegistry.get(vars.getRace())).orElse(null);
        if (race == null || race.passives() == null)
            return;

        if (race.passives().noHungerDrain()) {
            this.exhaustionLevel = 0.0f;
        }
        creraces$regenerationMultiplier = (float) race.passives().regenerationMultiplier().evaluate(player);
    }

    /** A regeneration_multiplier of 0 or less skips natural regeneration and the hunger it costs. */
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"), cancellable = true)
    private void creraces$skipRegeneration(Player player, CallbackInfo ci) {
        if (creraces$regenerationMultiplier <= 0.0f) {
            ci.cancel();
        }
    }

    /** Scales the health natural regeneration restores by regeneration_multiplier. */
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private float creraces$scaleRegeneration(float amount) {
        return amount * creraces$regenerationMultiplier;
    }
}
