package mc.sayda.creraces.mixin;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(FoodData.class)
public class FoodDataMixin {

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
}
