package mc.sayda.creraces.mixin;

import mc.sayda.creraces.race.SocialPassivesHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * Mobs never count a player whose race they respect as a combat target. Besides attacks and
 * retaliation, this covers goat rams and fleeing (AvoidEntityGoal), which pick targets the same way.
 */
@Mixin(TargetingConditions.class)
public abstract class TargetingConditionsMixin {
    @Shadow
    @Final
    private boolean isCombat;

    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void creraces$skipRespectedPlayers(@Nullable LivingEntity attacker, LivingEntity target,
            CallbackInfoReturnable<Boolean> cir) {
        if (this.isCombat && attacker instanceof Mob mob && target instanceof Player player
                && SocialPassivesHelper.isRespectedBy(player, mob)) {
            cir.setReturnValue(false);
        }
    }
}
