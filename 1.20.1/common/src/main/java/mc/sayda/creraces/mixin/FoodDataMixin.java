package mc.sayda.creraces.mixin;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(FoodData.class)
public class FoodDataMixin implements mc.sayda.creraces.util.IFoodDataAccessor {

    @Shadow
    private int foodLevel;
    @Shadow
    private float saturationLevel;
    @Shadow
    private float exhaustionLevel;

    // ─── Food multiplier helpers (called from PlayerFoodMixin) ─────────────────

    /** Snapshot current food level (before eat runs). */
    @Override
    public int creraces$getFoodLevel() {
        return foodLevel;
    }

    /** Snapshot current saturation (before eat runs). */
    @Override
    public float creraces$getSaturation() {
        return saturationLevel;
    }

    /**
     * Apply the race food multiplier AFTER vanilla eat() has updated the values.
     * Scales the delta (gained amount) so a 2x multiplier doubles what was gained.
     */
    @Override
    public void creraces$applyFoodMultiplier(int oldFood, float oldSat, double multiplier) {
        int foodGained = foodLevel - oldFood;
        float satGained = saturationLevel - oldSat;
        int adjustedFood = (int) Math.round(foodGained * multiplier);
        float adjustedSat = (float) (satGained * multiplier);
        foodLevel = oldFood + adjustedFood;
        saturationLevel = Math.min(oldSat + adjustedSat, (float) foodLevel);
    }

    /**
     * Cancels the natural health regen tick for races with no_natural_regeneration.
     */
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

    /**
     * Cancels the hunger exhaustion/drain tick for races with no_hunger_drain.
     * This prevents the food level from ever decreasing by resetting exhaustion.
     */
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
