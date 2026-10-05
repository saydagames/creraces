package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.DataValue;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/** Sets, adds to, multiplies or removes a key in an entity's persistent data. */
@SuppressWarnings("null")
public class ModifyEntityDataAction implements ActionRegistry.RaceAction {

    // Writing either key on a player also sets its small-build flag (a value of 1 or more turns it on).
    private static final String KEY_MINIBUILD = "minibuild";
    private static final String KEY_SMALL_BUILD = "smallBuild";

    public enum Operation {
        SET, ADD, REMOVE, MULTIPLY;

        /** Case-insensitive; unknown names fall back to SET. */
        public static Operation fromString(String op) {
            for (Operation o : values()) {
                if (o.name().equalsIgnoreCase(op)) {
                    return o;
                }
            }
            return SET;
        }
    }

    private final String key;
    private final Operation operation;
    private final DataValue value;
    private final boolean useTarget;

    public ModifyEntityDataAction(String key, Operation operation, DataValue value, boolean useTarget) {
        this.key = key;
        this.operation = operation;
        this.value = value;
        this.useTarget = useTarget;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity entity = DataValue.resolveEntity(useTarget, player, target);
        if (!(entity instanceof IPersistentDataAccessor accessor)) {
            return true;
        }
        CompoundTag persistentData = accessor.creraces$getPersistentData();

        if (operation == Operation.REMOVE) {
            persistentData.remove(key);
            return true;
        }

        if (operation == Operation.SET) {
            boolean written = value.writeInto(persistentData, key, player, target, slot, interactPos);
            if (written && entity instanceof Player playerEntity
                    && value.resolve(player, target, slot, interactPos) instanceof Number number) {
                applyMinibuildBridge(playerEntity, number.doubleValue());
            }
            return true;
        }

        // ADD and MULTIPLY are numeric only.
        if (!(value.resolve(player, target, slot, interactPos) instanceof Number number)) {
            return true;
        }
        double current = persistentData.getDouble(key);
        double newValue = operation == Operation.ADD ? current + number.doubleValue() : current * number.doubleValue();
        if (value.type() == DataValue.DataType.INT) {
            persistentData.putInt(key, (int) Math.round(newValue));
        } else {
            persistentData.putDouble(key, newValue);
        }
        if (entity instanceof Player playerEntity) {
            applyMinibuildBridge(playerEntity, newValue);
        }
        return true;
    }

    private void applyMinibuildBridge(Player playerEntity, double newValue) {
        if (!key.equalsIgnoreCase(KEY_MINIBUILD) && !key.equalsIgnoreCase(KEY_SMALL_BUILD)) {
            return;
        }
        DataUtils.getVariables(playerEntity).ifPresent(vars -> {
            vars.setSmallBuild(newValue >= 1.0);
            BoundaryHandler.resyncVariables(playerEntity, playerEntity);
        });
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "modify_entity_data"), json -> {
            String key = DataValue.sanitizeKey(GsonHelper.getAsString(json, "key"), "ModifyEntityDataAction");
            Operation op = Operation.fromString(GsonHelper.getAsString(json, "operation", "SET"));

            DataValue value = DataValue.fromJson(json, "value", DataValue.DataType.DOUBLE);
            if (value == null) {
                value = DataValue.literal(DataValue.DataType.DOUBLE, 0.0);
            }
            if ((op == Operation.ADD || op == Operation.MULTIPLY)
                    && value.type() != DataValue.DataType.INT && value.type() != DataValue.DataType.DOUBLE) {
                CreRaces.LOGGER.error(
                        "ModifyEntityDataAction: operation {} requires a numeric value type, got {} on key '{}' - using SET instead",
                        op, value.type(), key);
                op = Operation.SET;
            }
            return new ModifyEntityDataAction(key, op, value, DataValue.readUseTarget(json, false));
        });
    }
}
