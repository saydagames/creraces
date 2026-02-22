package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ModifyEntityDataAction implements ActionRegistry.RaceAction {

    private final String key;
    private final String operation; // set, add, remove
    private final double value;
    private final boolean useTarget;

    public ModifyEntityDataAction(String key, String operation, double value, boolean useTarget) {
        this.key = key;
        this.operation = operation;
        this.value = value;
        this.useTarget = useTarget;
    }

    @Override
    public void execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        LivingEntity entity = (useTarget && target != null) ? target : player;
        CompoundTag persistentData = ((IPersistentDataAccessor) entity).creraces$getPersistentData();

        if (operation.equalsIgnoreCase("remove")) {
            persistentData.remove(key);
            return;
        }

        double current = persistentData.getDouble(key);
        double newValue = current;

        if (operation.equalsIgnoreCase("add")) {
            newValue += value;
        } else if (operation.equalsIgnoreCase("set")) {
            newValue = value;
        }

        persistentData.putDouble(key, newValue);
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "modify_entity_data"), json -> {
            String key = GsonHelper.getAsString(json, "key");
            String op = GsonHelper.getAsString(json, "operation", "set");
            double val = GsonHelper.getAsDouble(json, "value", 0.0);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", true);

            return new ModifyEntityDataAction(key, op, val, useTarget);
        });
    }
}
