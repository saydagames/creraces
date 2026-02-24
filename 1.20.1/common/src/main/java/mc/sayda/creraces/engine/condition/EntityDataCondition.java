package mc.sayda.creraces.engine.condition;

import com.google.gson.JsonObject;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public record EntityDataCondition(String key, String operator, double value, boolean useTarget) implements Condition {

    @Override
    public boolean evaluate(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        net.minecraft.world.entity.LivingEntity entity = (useTarget && target != null) ? target : player;

        // Use creraces$getPersistentData() from IPersistentDataAccessor
        net.minecraft.nbt.CompoundTag data = ((IPersistentDataAccessor) entity).creraces$getPersistentData();
        double current = data.getDouble(key);

        return switch (operator) {
            case ">=" -> current >= value;
            case "<=" -> current <= value;
            case ">" -> current > value;
            case "<" -> current < value;
            case "==" -> Math.abs(current - value) < 0.001;
            case "!=" -> Math.abs(current - value) >= 0.001;
            default -> false;
        };
    }

    public static Condition fromJson(JsonObject json) {
        String key = GsonHelper.getAsString(json, "key");
        String op = GsonHelper.getAsString(json, "operator", "==");
        double val = GsonHelper.getAsDouble(json, "value", 0.0);
        boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
        return new EntityDataCondition(key, op, val, useTarget);
    }
}
