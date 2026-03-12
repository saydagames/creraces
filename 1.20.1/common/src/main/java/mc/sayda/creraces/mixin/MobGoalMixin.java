package mc.sayda.creraces.mixin;

import mc.sayda.creraces.entity.ai.ServantGoal;
import mc.sayda.creraces.entity.ai.ServantAttackGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import mc.sayda.creraces.util.IPersistentDataAccessor;

@Mixin(Mob.class)
public abstract class MobGoalMixin {
    @Shadow public GoalSelector goalSelector;
    
    @Unique
    private boolean creraces$servantGoalAdded = false;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void creraces$checkServantGoal(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (mob.level().isClientSide) return;

        if (!creraces$servantGoalAdded) {
            CompoundTag nbt = ((IPersistentDataAccessor) mob).creraces$getPersistentData();
            if (nbt.contains("creraces:servant_of")) {
                // Priority 0 (highest) to override other behaviors when commanded
                this.goalSelector.addGoal(0, new ServantGoal(mob));
                this.goalSelector.addGoal(0, new ServantAttackGoal(mob));
                creraces$servantGoalAdded = true;
            }
        }
    }
}
