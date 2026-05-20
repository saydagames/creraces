package mc.sayda.creraces.engine.actions;
 
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
 
/**
 * Action to set race state variables (a1, a2, a3, a4).
 */
public class SetStateAction implements ActionRegistry.RaceAction {
 
    private final String stateVariable;
    private final mc.sayda.creraces.engine.ScalingValue value;
    private final @javax.annotation.Nullable ResourceLocation abilityId;
    private final String operation;
    private final String mode; // "STATIC", "POS_X", "BLOCK_X", "TARGET_BLOCK_X", etc.
    private final mc.sayda.creraces.engine.ScalingValue offsetX;
    private final mc.sayda.creraces.engine.ScalingValue offsetY;
    private final mc.sayda.creraces.engine.ScalingValue offsetZ;
    private final boolean persistent;
 
    public SetStateAction(String stateVariable, mc.sayda.creraces.engine.ScalingValue value,
            @javax.annotation.Nullable ResourceLocation abilityId, String operation, String mode,
            mc.sayda.creraces.engine.ScalingValue offsetX, mc.sayda.creraces.engine.ScalingValue offsetY,
            mc.sayda.creraces.engine.ScalingValue offsetZ, boolean persistent) {
        this.stateVariable = stateVariable;
        this.value = value;
        this.abilityId = abilityId;
        this.operation = operation;
        this.mode = mode;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.persistent = persistent;
    }
 
    @Override
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            ResourceLocation targetAbilityId = abilityId;
 
            if (targetAbilityId == null && "self".equalsIgnoreCase(stateVariable) && slot != null) {
                targetAbilityId = vars.getAbilityInSlot(slot);
            }
 
            if (targetAbilityId != null) {
                double current = vars.getPersistentState(targetAbilityId);
                double val = value.evaluate(player, target, slot);
 
                // Mode logic for capturing coordinates
                double ox = offsetX.evaluate(player, target, slot);
                double oy = offsetY.evaluate(player, target, slot);
                double oz = offsetZ.evaluate(player, target, slot);
 
                double contextualValue = val;
                String m = mode.toUpperCase();
                switch (m) {
                    case "POS_X" -> contextualValue = player.getX() + ox;
                    case "POS_Y" -> contextualValue = player.getY() + oy;
                    case "POS_Z" -> contextualValue = player.getZ() + oz;
                    case "BLOCK_X" -> contextualValue = player.blockPosition().getX() + (int) ox;
                    case "BLOCK_Y" -> contextualValue = player.blockPosition().getY() + (int) oy;
                    case "BLOCK_Z" -> contextualValue = player.blockPosition().getZ() + (int) oz;
                    case "TARGET_X" -> {
                        if (target != null)
                            contextualValue = target.getX() + ox;
                    }
                    case "TARGET_Y" -> {
                        if (target != null)
                            contextualValue = target.getY() + oy;
                    }
                    case "TARGET_Z" -> {
                        if (target != null)
                            contextualValue = target.getZ() + oz;
                    }
                    case "TARGET_BLOCK_X" -> {
                        if (interact_pos != null) {
                            contextualValue = interact_pos.getX() + (int) ox;
                        } else {
                            net.minecraft.world.phys.HitResult hit = player.pick(5.0, 0.0F, false);
                            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                                contextualValue = ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos().getX()
                                        + (int) ox;
                            }
                        }
                    }
                    case "TARGET_BLOCK_Y" -> {
                        if (interact_pos != null) {
                            contextualValue = interact_pos.getY() + (int) oy;
                        } else {
                            net.minecraft.world.phys.HitResult hit = player.pick(5.0, 0.0F, false);
                            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                                contextualValue = ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos().getY()
                                        + (int) oy;
                            }
                        }
                    }
                    case "TARGET_BLOCK_Z" -> {
                        if (interact_pos != null) {
                            contextualValue = interact_pos.getZ() + (int) oz;
                        } else {
                            net.minecraft.world.phys.HitResult hit = player.pick(5.0, 0.0F, false);
                            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                                contextualValue = ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos().getZ()
                                        + (int) oz;
                            }
                        }
                    }
                }
 
                double next = switch (operation.toLowerCase()) {
                    case "add" -> current + contextualValue;
                    case "multiply" -> current * contextualValue;
                    default -> contextualValue;
                };
                vars.setPersistentState(targetAbilityId, next);
                if (persistent) {
                    vars.setStatePersistent(targetAbilityId, true);
                }
            }
            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player);
        });
        return true;
    }
 
    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "set_state"), json -> {
            String stateStr = GsonHelper.getAsString(json, "state", "self");
            mc.sayda.creraces.engine.ScalingValue value = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "value", 0.0);
            String operation = GsonHelper.getAsString(json, "operation", "set");
            String mode = GsonHelper.getAsString(json, "mode", "STATIC");
            mc.sayda.creraces.engine.ScalingValue ox = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "offset_x", 0.0);
            mc.sayda.creraces.engine.ScalingValue oy = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "offset_y", 0.0);
            mc.sayda.creraces.engine.ScalingValue oz = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "offset_z", 0.0);
            boolean persistent = GsonHelper.getAsBoolean(json, "persistent", false);
 
            ResourceLocation stateLoc = null;
            if (!"self".equalsIgnoreCase(stateStr)) {
                String sub = stateStr.startsWith("state:") ? stateStr.substring(6) : stateStr;
                if (!sub.contains(":")) {
                    sub = "creraces:" + sub;
                }
                stateLoc = ResourceLocation.tryParse(sub);
            }
            return new SetStateAction(stateStr, value, stateLoc, operation, mode, ox, oy, oz, persistent);
        });
        // Alias for legacy modify_state
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "modify_state"), json -> {
            String stateStr = GsonHelper.getAsString(json, "state", "self");
            mc.sayda.creraces.engine.ScalingValue value = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "value", 0.0);
            String operation = GsonHelper.getAsString(json, "operation", "add");
            String mode = GsonHelper.getAsString(json, "mode", "STATIC");
            mc.sayda.creraces.engine.ScalingValue ox = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "offset_x", 0.0);
            mc.sayda.creraces.engine.ScalingValue oy = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "offset_y", 0.0);
            mc.sayda.creraces.engine.ScalingValue oz = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "offset_z", 0.0);
            boolean persistent = GsonHelper.getAsBoolean(json, "persistent", false);
 
            ResourceLocation stateLoc = null;
            if (!"self".equalsIgnoreCase(stateStr)) {
                String sub = stateStr.startsWith("state:") ? stateStr.substring(6) : stateStr;
                if (!sub.contains(":")) {
                    sub = "creraces:" + sub;
                }
                stateLoc = ResourceLocation.tryParse(sub);
            }
            return new SetStateAction(stateStr, value, stateLoc, operation, mode, ox, oy, oz, persistent);
        });
    }
}
