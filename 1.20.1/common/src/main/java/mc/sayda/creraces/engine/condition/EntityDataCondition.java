package mc.sayda.creraces.engine.condition;

import com.google.gson.JsonObject;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.DataValue;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import mc.sayda.creraces.util.RaceUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/** Compares a persistent-data key on the player or target against a DataValue, or checks that the key exists. */
public class EntityDataCondition implements Condition {
    private final String key;
    private final String operator;
    @Nullable
    private final DataValue value;
    private final boolean useTarget;

    public EntityDataCondition(String key, String operator, @Nullable DataValue value, boolean useTarget) {
        this.key = key;
        this.operator = operator;
        this.value = value;
        this.useTarget = useTarget;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target,
            @Nullable AbilitySlot slot, @Nullable BlockPos interactPos) {
        LivingEntity entity = DataValue.resolveEntity(useTarget, player, target);
        if (entity == null)
            return false;

        CompoundTag data = ((IPersistentDataAccessor) entity).creraces$getPersistentData();

        // No "value" given: this is a presence check, not a comparison.
        if (value == null) {
            boolean present = data.contains(key);
            return operator.equals("!=") != present;
        }

        Object current;
        if (entity instanceof Player entityPlayer && isPlayerShortcutKey(key)) {
            current = DataUtils.getVariables(entityPlayer)
                    .map(vars -> (Object) playerShortcutValue(vars, key))
                    .orElseGet(() -> DataValue.readTag(data, key, value.type()));
        } else {
            current = DataValue.readTag(data, key, value.type());
        }

        Object expected = value.resolve(player, target, slot, interactPos);
        return DataValue.compare(current, expected, operator);
    }

    private static boolean isPlayerShortcutKey(String key) {
        return key.equalsIgnoreCase("is_minibuild") || key.equalsIgnoreCase("is_spirit")
                || key.equalsIgnoreCase("is_tiny");
    }

    private static Double playerShortcutValue(IPlayerVariables vars, String key) {
        if (key.equalsIgnoreCase("is_minibuild"))
            return vars.isSmallBuild() ? 1.0 : 0.0;
        if (key.equalsIgnoreCase("is_spirit"))
            return RaceUtils.isSpirit(vars) ? 1.0 : 0.0;
        if (key.equalsIgnoreCase("is_tiny"))
            return vars.isTiny() ? 1.0 : 0.0;
        return null;
    }

    public static Condition fromJson(JsonObject json) {
        String key = GsonHelper.getAsString(json, "key");
        String op = GsonHelper.getAsString(json, "operator", ">=");
        DataValue value = DataValue.fromJson(json, "value", DataValue.DataType.DOUBLE);
        boolean useTarget = DataValue.readUseTarget(json, false);
        return new EntityDataCondition(key, op, value, useTarget);
    }
}
