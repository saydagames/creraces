package mc.sayda.creraces.engine.condition;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ScalingValue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Checks if a state variable modulo a divisor equals a specific remainder.
 * Useful for parity checks (even/odd) in ability states.
 */
public class ModuloCondition implements Condition {
    private final ResourceLocation stateId;
    private final ScalingValue divisor;
    private final ScalingValue remainder;

    public ModuloCondition(ResourceLocation stateId, ScalingValue divisor, ScalingValue remainder) {
        this.stateId = stateId;
        this.divisor = divisor;
        this.remainder = remainder;
    }

    @Override
    public boolean evaluate(Player player, 
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        
        if (stateId == null) return false;
        return DataUtils.getVariables(player).map(vars -> {
            int current = (int) vars.getPersistentState(stateId);
            int div = (int) divisor.evaluate(player, target, slot, interact_pos);
            int rem = (int) remainder.evaluate(player, target, slot, interact_pos);

            if (div == 0) return false;
            return (current % div) == rem;
        }).orElse(false);
    }
}
