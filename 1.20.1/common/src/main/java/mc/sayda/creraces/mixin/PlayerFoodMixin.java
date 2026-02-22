package mc.sayda.creraces.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerFoodMixin {

    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "eat", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int creraces$modifyFood(int food, net.minecraft.world.level.Level level,
            net.minecraft.world.item.ItemStack stack) {
        return (int) (food * mc.sayda.creraces.util.RaceUtils.getFoodMultiplier((Player) (Object) this));
    }

    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "eat", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float creraces$modifySaturation(float saturation, net.minecraft.world.level.Level level,
            net.minecraft.world.item.ItemStack stack) {
        return (float) (saturation * mc.sayda.creraces.util.RaceUtils.getFoodMultiplier((Player) (Object) this));
    }
}
