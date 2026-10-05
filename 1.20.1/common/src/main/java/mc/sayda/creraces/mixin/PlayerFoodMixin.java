package mc.sayda.creraces.mixin;

import mc.sayda.creraces.util.RaceUtils;
import mc.sayda.creraces.util.IFoodDataAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Scales food and saturation gains by the race food multiplier: eat() is snapshotted at HEAD and the
 * gained amount is scaled at RETURN (see FoodDataMixin.creraces$applyFoodMultiplier).
 */
@Mixin(Player.class)
public abstract class PlayerFoodMixin {

    @Shadow
    public abstract FoodData getFoodData();

    // ThreadLocal rather than a field: the snapshot only lives for the duration of one eat() call.
    private static final ThreadLocal<long[]> PRE_EAT_SNAPSHOT = ThreadLocal.withInitial(() -> new long[] { 0L, 0L });

    @Inject(method = "eat", at = @At("HEAD"))
    private void creraces$snapshotPreEat(Level level, ItemStack stack,
            CallbackInfoReturnable<ItemStack> cir) {
        IFoodDataAccessor fdm = (IFoodDataAccessor) getFoodData();
        long[] snap = PRE_EAT_SNAPSHOT.get();
        snap[0] = fdm.creraces$getFoodLevel();
        snap[1] = Float.floatToRawIntBits(fdm.creraces$getSaturation());
    }

    @Inject(method = "eat", at = @At("RETURN"))
    private void creraces$applyMultiplier(Level level, ItemStack stack,
            CallbackInfoReturnable<ItemStack> cir) {
        double multiplier = RaceUtils.getFoodMultiplier((Player) (Object) this);
        if (multiplier == 1.0)
            return;

        IFoodDataAccessor fdm = (IFoodDataAccessor) getFoodData();
        long[] snap = PRE_EAT_SNAPSHOT.get();
        int oldFood = (int) snap[0];
        float oldSat = Float.intBitsToFloat((int) snap[1]);
        fdm.creraces$applyFoodMultiplier(oldFood, oldSat, multiplier);
    }
}
