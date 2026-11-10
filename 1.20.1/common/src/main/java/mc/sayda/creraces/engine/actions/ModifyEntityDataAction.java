package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.DataValue;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Writes a persistent-data "key" on an entity. "operation": set (default) writes "value" (a data
 * value of any type), add and multiply change a number, remove deletes the key.
 */
public class ModifyEntityDataAction implements ActionRegistry.RaceAction {
    private enum Operation { SET, ADD, MULTIPLY, REMOVE }

    private final String key;
    private final Operation operation;
    private final DataValue value;
    private final EntitySelector entity;

    private ModifyEntityDataAction(String key, Operation operation, DataValue value, EntitySelector entity) {
        this.key = key;
        this.operation = operation;
        this.value = value;
        this.entity = entity;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!(entity.resolve(player, target) instanceof IPersistentDataAccessor accessor)) {
            return true;
        }
        CompoundTag data = accessor.creraces$getPersistentData();
        switch (operation) {
            case REMOVE -> data.remove(key);
            case SET -> value.writeInto(data, key, player, target, slot, interactPos);
            case ADD, MULTIPLY -> {
                if (value.resolve(player, target, slot, interactPos) instanceof Number number) {
                    double current = data.getDouble(key);
                    double next = operation == Operation.ADD ? current + number.doubleValue()
                            : current * number.doubleValue();
                    if (value.type() == DataValue.DataType.INT) {
                        data.putInt(key, (int) Math.round(next));
                    } else {
                        data.putDouble(key, next);
                    }
                }
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("modify_entity_data"), json -> {
            String key = DataValue.sanitizeKey(GsonHelper.getAsString(json, "key"), "modify_entity_data");
            String operationName = GsonHelper.getAsString(json, "operation", "set");
            Operation operation = switch (operationName) {
                case "set" -> Operation.SET;
                case "add" -> Operation.ADD;
                case "multiply" -> Operation.MULTIPLY;
                case "remove" -> Operation.REMOVE;
                default -> throw new IllegalArgumentException(
                        "unknown operation '" + operationName + "' (set, add, multiply, remove)");
            };
            DataValue value = DataValue.fromJson(json, "value", DataValue.DataType.DOUBLE);
            if (value == null) {
                if (operation != Operation.REMOVE) {
                    throw new IllegalArgumentException("needs a \"value\"");
                }
                value = DataValue.literal(DataValue.DataType.DOUBLE, 0.0);
            }
            if ((operation == Operation.ADD || operation == Operation.MULTIPLY)
                    && value.type() != DataValue.DataType.INT && value.type() != DataValue.DataType.DOUBLE) {
                throw new IllegalArgumentException(operationName + " needs a number value");
            }
            return new ModifyEntityDataAction(key, operation, value, EntitySelector.fromJson(json, EntitySelector.AUTO));
        });
    }
}
