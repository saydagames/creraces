package mc.sayda.creraces.engine.condition;

import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ScalingValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Compares two ScalingValues, e.g. a state variable against a scaled config value. Every operator
 * allows a 0.001 tolerance; "%" is true when the first value is a multiple of the second.
 */
public class ScalingCompareCondition implements Condition {
    private static final double EPSILON = 0.001;

    private final ScalingValue first;
    private final ScalingValue second;
    private final String operator;

    public ScalingCompareCondition(ScalingValue first, ScalingValue second, String operator) {
        this.first = first;
        this.second = second;
        this.operator = operator != null ? operator : "==";
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        double val1 = first.evaluate(player, target, slot, interactPos);
        double val2 = second.evaluate(player, target, slot, interactPos);

        return switch (operator) {
            case "!=" -> Math.abs(val1 - val2) >= EPSILON;
            case ">" -> val1 > val2 + EPSILON;
            case ">=" -> val1 >= val2 - EPSILON;
            case "<" -> val1 < val2 - EPSILON;
            case "<=" -> val1 <= val2 + EPSILON;
            case "%" -> {
                if (Math.abs(val2) < EPSILON)
                    yield false;
                yield Math.abs((val1 % val2)) < EPSILON;
            }
            default -> Math.abs(val1 - val2) < EPSILON;
        };
    }
}
