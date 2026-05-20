package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonElement;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class ToggleStateAction implements ActionRegistry.RaceAction {

    private final String stateVariable;
    private final @javax.annotation.Nullable ResourceLocation abilityId;
    private final ScalingValue onValue;
    private final ScalingValue offValue;
    private final List<ActionRegistry.RaceAction> onEnable;
    private final List<ActionRegistry.RaceAction> onDisable;

    public ToggleStateAction(String stateVariable, @javax.annotation.Nullable ResourceLocation abilityId,
            ScalingValue onValue, ScalingValue offValue,
            List<ActionRegistry.RaceAction> onEnable,
            List<ActionRegistry.RaceAction> onDisable) {
        this.stateVariable = stateVariable;
        this.abilityId = abilityId;
        this.onValue = onValue;
        this.offValue = offValue;
        this.onEnable = onEnable;
        this.onDisable = onDisable;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        boolean[] success = { true };
        DataUtils.getVariables(player).ifPresent(vars -> {
            ResourceLocation targetAbilityId = abilityId;

            if (targetAbilityId == null && "self".equalsIgnoreCase(stateVariable) && slot != null) {
                targetAbilityId = vars.getAbilityInSlot(slot);
            } else if (targetAbilityId == null) {
                // Handle stateVariable as a potential ResourceLocation string with prefixes
                String parsedStateVariable = stateVariable;
                if (stateVariable.startsWith("ability:")) {
                    parsedStateVariable = stateVariable.substring(8);
                } else if (stateVariable.startsWith("state:")) { // New alias for ability:
                    parsedStateVariable = stateVariable.substring(6);
                } else if (stateVariable.startsWith("custom:")) {
                    parsedStateVariable = stateVariable.substring(7);
                }
                targetAbilityId = ResourceLocation.tryParse(java.util.Objects.requireNonNull(parsedStateVariable));
            }

            if (targetAbilityId == null)
                return;

            double on = onValue.evaluate(player, target, slot);
            double off = offValue.evaluate(player, target, slot);

            double current = vars.getPersistentState(targetAbilityId);
            if (Math.abs(current - off) < 0.001) {
                vars.setPersistentState(targetAbilityId, on);
                for (ActionRegistry.RaceAction a : onEnable) {
                    if (!a.execute(player, target, slot, interact_pos)) {
                        success[0] = false;
                        break;
                    }
                }
            } else {
                vars.setPersistentState(targetAbilityId, off);
                for (ActionRegistry.RaceAction a : onDisable) {
                    if (!a.execute(player, target, slot, interact_pos)) {
                        success[0] = false;
                        break;
                    }
                }
            }

            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player);
        });
        return success[0];
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "toggle_state"), json -> {
            String stateStr = GsonHelper.getAsString(json, "state", "self");
            ScalingValue on = ScalingValue.fromJson(json, "on_value", 1.0);
            ScalingValue off = ScalingValue.fromJson(json, "off_value", 0.0);

            List<ActionRegistry.RaceAction> onEnable = new ArrayList<>();
            if (json.has("on_enable")) {
                for (JsonElement e : json.getAsJsonArray("on_enable")) {
                    onEnable.add(ActionRegistry.fromJson(e.getAsJsonObject()));
                }
            }

            List<ActionRegistry.RaceAction> onDisable = new ArrayList<>();
            if (json.has("on_disable")) {
                for (JsonElement e : json.getAsJsonArray("on_disable")) {
                    onDisable.add(ActionRegistry.fromJson(e.getAsJsonObject()));
                }
            }

            ResourceLocation stateLoc = null;
            if (!"self".equalsIgnoreCase(stateStr)) {
                String sub = stateStr.startsWith("state:") ? stateStr.substring(6) : stateStr;
                if (!sub.contains(":")) {
                    sub = "creraces:" + sub;
                }
                stateLoc = ResourceLocation.tryParse(sub);
            }

            return new ToggleStateAction(stateStr, stateLoc, on, off, onEnable, onDisable);
        });
    }
}
