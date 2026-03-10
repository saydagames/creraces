package mc.sayda.creraces.engine.traits;

import java.util.Objects;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.capability.IPlayerVariables;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Trait for passive abilities that execute actions periodically.
 * Example: Harpy hunger drain while flying, auto-regen on low health, etc.
 */
public class OnTickTrait extends PeriodicTrait {

    private final List<ActionRegistry.RaceAction> actions;
    private final List<ActionRegistry.RaceAction> onFail;
    private final Condition condition;

    public OnTickTrait(ResourceLocation traitId, List<ActionRegistry.RaceAction> actions,
            List<ActionRegistry.RaceAction> onFail,
            mc.sayda.creraces.engine.ScalingValue interval,
            Condition condition) {
        super(traitId, interval);
        this.actions = actions;
        this.onFail = onFail;
        this.condition = condition;
    }

    @Override
    protected boolean shouldExecute(Player player, IPlayerVariables vars) {
        if (player.level().isClientSide())
            return false;

        boolean success = condition == null || condition.evaluate(player, null, null, null);
        ResourceLocation failId = new ResourceLocation(Objects.requireNonNull(traitId.getNamespace()),
                Objects.requireNonNull(traitId.getPath()) + "_failed");

        if (!success) {
            // Run onFail only once when transition from success to fail happens
            if (vars.getAbilityState(failId) == 0.0) {
                vars.setAbilityState(failId, 1.0);
                for (ActionRegistry.RaceAction action : onFail) {
                    action.execute(player, null, null, null);
                }
            }
            return false;
        }

        vars.setAbilityState(failId, 0.0);
        return true;
    }

    @Override
    protected void execute(Player player, IPlayerVariables vars) {
        for (ActionRegistry.RaceAction action : actions) {
            action.execute(player, null, null, null);
        }
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "on_tick"), json -> {
            mc.sayda.creraces.engine.ScalingValue interval = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                    "interval", 20.0);
            Condition condition = json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null;

            List<ActionRegistry.RaceAction> actions = new ArrayList<>();
            if (json.has("actions") && json.get("actions").isJsonArray()) {
                for (var actionElem : json.getAsJsonArray("actions")) {
                    if (actionElem.isJsonObject()) {
                        ActionRegistry.RaceAction action = ActionRegistry.fromJson(actionElem.getAsJsonObject());
                        if (action != null) {
                            actions.add(action);
                        }
                    }
                }
            }

            List<ActionRegistry.RaceAction> onFail = new ArrayList<>();
            if (json.has("on_fail") && json.get("on_fail").isJsonArray()) {
                for (var actionElem : json.getAsJsonArray("on_fail")) {
                    if (actionElem.isJsonObject()) {
                        ActionRegistry.RaceAction action = ActionRegistry.fromJson(actionElem.getAsJsonObject());
                        if (action != null) {
                            onFail.add(action);
                        }
                    }
                }
            }

            String traitName = json.has("name") ? json.get("name").getAsString()
                    : "ontick_" + Math.abs(json.toString().hashCode());
            ResourceLocation traitId = new ResourceLocation(CreRaces.MODID, "trait_" + traitName);

            return new OnTickTrait(traitId, actions, onFail, interval, condition);
        });
    }

}
