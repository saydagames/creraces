package mc.sayda.creraces.engine.actions;

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
    private final mc.sayda.creraces.engine.ScalingValue value;
    private final boolean useTarget;

    public ModifyEntityDataAction(String key, String operation, mc.sayda.creraces.engine.ScalingValue value,
            boolean useTarget) {
        this.key = key;
        this.operation = operation;
        this.value = value;
        this.useTarget = useTarget;
    }

    @Override
    @SuppressWarnings("null")
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        // Smart Targeting: Prefer target if present, otherwise respect useTarget flag
        LivingEntity entity = (target != null) ? target : (useTarget ? null : player);
        if (entity == null)
            return true;
        CompoundTag persistentData = ((IPersistentDataAccessor) entity).creraces$getPersistentData();

        if (operation.equalsIgnoreCase("remove")) {
            persistentData.remove(key);
            return true;
        }

        double val = value.evaluate(player, target);
        double current = persistentData.getDouble(key);
        double newValue = current;

        if (operation.equalsIgnoreCase("add")) {
            newValue += val;
        } else if (operation.equalsIgnoreCase("set")) {
            newValue = val;
        } else if (operation.equalsIgnoreCase("multiply")) {
            newValue *= val;
        }

        persistentData.putDouble(key, newValue);

        // Bridge to PlayerVariables if applicable
        if (entity instanceof Player playerEntity) {
            final double finalNewValue = newValue;
            mc.sayda.creraces.capability.DataUtils.getVariables(playerEntity).ifPresent(vars -> {
                if (key.equalsIgnoreCase("minibuild") || key.equalsIgnoreCase("smallBuild")) {
                    vars.setSmallBuild(finalNewValue >= 1.0);
                    // Sync to client
                    mc.sayda.creraces.network.BoundaryHandler.resyncVariables(playerEntity, playerEntity);
                }
            });
        }

        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "modify_entity_data"), json -> {
            String key = GsonHelper.getAsString(json, "key");
            // Limit key length to prevent NBT bloat via crafted race JSONs
            int keyMaxLen = mc.sayda.creraces.config.CreRacesConfig.ENTITY_DATA_KEY_MAX_LENGTH.get();
            if (keyMaxLen > 0 && key.length() > keyMaxLen) {
                CreRaces.LOGGER.warn("[CreRaces] ModifyEntityDataAction: key '{}' exceeds {} chars, truncating", key,
                        keyMaxLen);
                key = key.substring(0, keyMaxLen);
            }
            String op = GsonHelper.getAsString(json, "operation", "set");
            mc.sayda.creraces.engine.ScalingValue val = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "value",
                    0.0);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", true);

            return new ModifyEntityDataAction(key, op, val, useTarget);
        });
    }
}
