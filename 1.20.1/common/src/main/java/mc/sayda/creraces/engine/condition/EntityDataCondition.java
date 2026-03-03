package mc.sayda.creraces.engine.condition;

import com.google.gson.JsonObject;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public record EntityDataCondition(String key, String operator, mc.sayda.creraces.engine.ScalingValue value,
        boolean useTarget) implements Condition {

    @Override
    @SuppressWarnings("null")
    public boolean evaluate(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        // Smart Targeting: Prefer target if present, otherwise respect useTarget flag
        net.minecraft.world.entity.LivingEntity entity = (target != null) ? target : (useTarget ? target : player);
        if (entity == null)
            return false;

        // Use creraces$getPersistentData() from IPersistentDataAccessor
        net.minecraft.nbt.CompoundTag data = ((IPersistentDataAccessor) entity).creraces$getPersistentData();

        double current;
        if (entity instanceof Player entityPlayer) {
            current = DataUtils.getVariables(entityPlayer).map(vars -> {
                if (key.equalsIgnoreCase("minibuild") || key.equalsIgnoreCase("smallBuild")) {
                    return vars.isSmallBuild() ? 1.0 : 0.0;
                }
                return data.getDouble(key);
            }).orElse(data.getDouble(key));
        } else {
            current = data.getDouble(key);
        }

        double val = value.evaluate(player, target);

        return switch (operator) {
            case ">=" -> current >= val;
            case "<=" -> current <= val;
            case ">" -> current > val;
            case "<" -> current < val;
            case "==" -> Math.abs(current - val) < 0.001;
            case "!=" -> Math.abs(current - val) >= 0.001;
            default -> false;
        };
    }

    public static Condition fromJson(JsonObject json) {
        String key = GsonHelper.getAsString(json, "key");
        String op = GsonHelper.getAsString(json, "operator", "==");
        mc.sayda.creraces.engine.ScalingValue val = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "value", 0.0);
        boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
        return new EntityDataCondition(key, op, val, useTarget);
    }
}
