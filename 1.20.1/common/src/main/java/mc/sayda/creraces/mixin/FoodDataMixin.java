package mc.sayda.creraces.mixin;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.util.IFoodDataAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

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

    /** Races with no_natural_regeneration skip the health regen FoodData grants from a full hunger bar. */
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"), cancellable = true)
    private void creraces$cancelHeal(Player player, CallbackInfo ci) {
        if (player.level().isClientSide())
            return;

        Optional<IPlayerVariables> varsOpt = DataUtils.getVariables(player);
        if (varsOpt.isPresent()) {
            Race race = RaceRegistry.get(varsOpt.get().getRace());
            if (race != null && race.passives() != null && race.passives().noNaturalRegeneration()) {
                ci.cancel();
            }
        }
    }

    /** Races with no_hunger_drain have their exhaustion wiped every tick, so food never drops. */
    @Inject(method = "tick", at = @At("HEAD"))
    private void creraces$cancelHungerDrain(Player player, CallbackInfo ci) {
        if (player.level().isClientSide())
            return;

        Optional<IPlayerVariables> varsOpt = DataUtils.getVariables(player);
        if (varsOpt.isPresent()) {
            Race race = RaceRegistry.get(varsOpt.get().getRace());
            if (race != null && race.passives() != null && race.passives().noHungerDrain()) {
                this.exhaustionLevel = 0.0f;
            }
        }
    }
}
