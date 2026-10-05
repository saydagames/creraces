package mc.sayda.creraces.engine.condition;

import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ScalingValue;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Checks whether a state variable modulo a divisor equals a given remainder, e.g. for even/odd checks.
 * All three values are truncated to integers first.
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
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (stateId == null) return false;
        return DataUtils.getVariables(player).map(vars -> {
            int current = (int) vars.getPersistentState(stateId);
            int div = (int) divisor.evaluate(player, target, slot, interactPos);
            int rem = (int) remainder.evaluate(player, target, slot, interactPos);

            if (div == 0) return false;
            return Math.floorMod(current, div) == rem;
        }).orElse(false);
    }
}
