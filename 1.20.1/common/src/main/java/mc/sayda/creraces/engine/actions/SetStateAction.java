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

    public SetStateAction(String stateVariable, mc.sayda.creraces.engine.ScalingValue value,
            @javax.annotation.Nullable ResourceLocation abilityId, String operation) {
        this.stateVariable = stateVariable;
        this.value = value;
        this.abilityId = abilityId;
        this.operation = operation;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            ResourceLocation targetAbilityId = abilityId;

            if (targetAbilityId == null && "slot".equalsIgnoreCase(stateVariable) && slot != null) {
                targetAbilityId = vars.getAbilityInSlot(slot);
            }

            if (targetAbilityId != null) {
                double current = vars.getAbilityState(targetAbilityId);
                double val = value.evaluate(player, target);
                double next = switch (operation.toLowerCase()) {
                    case "add" -> current + val;
                    case "multiply" -> current * val;
                    default -> val;
                };
                vars.setAbilityState(targetAbilityId, next);
            }
            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player);
        });
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "set_state"), json -> {
            String state = GsonHelper.getAsString(json, "state", "slot");
            mc.sayda.creraces.engine.ScalingValue value = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "value",
                    0.0);
            String ability = GsonHelper.getAsString(json, "ability", null);
            String operation = GsonHelper.getAsString(json, "operation", "set");
            ResourceLocation abilityLoc = ability != null ? new ResourceLocation(ability) : null;
            return new SetStateAction(state, value, abilityLoc, operation);
        });
        // Alias for legacy modify_state
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "modify_state"), json -> {
            String state = GsonHelper.getAsString(json, "state", "slot");
            mc.sayda.creraces.engine.ScalingValue value = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "value",
                    0.0);
            String ability = GsonHelper.getAsString(json, "ability", null);
            String operation = GsonHelper.getAsString(json, "operation", "add");
            ResourceLocation abilityLoc = ability != null ? new ResourceLocation(ability) : null;
            return new SetStateAction(state, value, abilityLoc, operation);
        });
    }
}
