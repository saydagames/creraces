package mc.sayda.creraces.engine.condition;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ScalingValue;
import net.minecraft.world.entity.player.Player;

/**
 * Compares two ScalingValues using a specific operator.
 * Allows for complex logic like comparing state variables against multiplied
 * config values.
 */
public class ScalingCompareCondition implements Condition {
    private final ScalingValue first;
    private final ScalingValue second;
    private final String operator;

    public ScalingCompareCondition(ScalingValue first, ScalingValue second, String operator) {
        this.first = first;
        this.second = second;
        this.operator = operator != null ? operator : "==";
    }

    @Override
    public boolean evaluate(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {

        double val1 = first.evaluate(player, target, slot, interact_pos);
        double val2 = second.evaluate(player, target, slot, interact_pos);

        // Standard epsilon for robust floating point comparisons
        final double epsilon = 0.001;

        boolean result = switch (operator) {
            case "!=" -> Math.abs(val1 - val2) >= epsilon;
            case ">" -> val1 > val2 + epsilon;
            case ">=" -> val1 >= val2 - epsilon;
            case "<" -> val1 < val2 - epsilon;
            case "<=" -> val1 <= val2 + epsilon;
            case "%" -> {
                if (Math.abs(val2) < epsilon)
                    yield false;
                yield Math.abs((val1 % val2)) < epsilon;
            }
            default -> Math.abs(val1 - val2) < epsilon;
        };

        CreRaces.LOGGER.debug("[ScalingCompareCondition] Evaluating: {} {} {} -> Result: {}", val1, operator, val2,
                result);

        return result;
    }
}
