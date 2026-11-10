package mc.sayda.creraces.engine.condition;

import mc.sayda.creraces.ability.AbilitySlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * A check that actions and traits can be gated on. The built-in types are registered in
 * {@link ConditionRegistry} and grouped into EntityConditions, PlayerConditions, WorldConditions,
 * BlockConditions and EventConditions.
 */
@FunctionalInterface
public interface Condition {
    boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos);
}
