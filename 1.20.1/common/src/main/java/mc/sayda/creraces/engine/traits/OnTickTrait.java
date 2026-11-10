package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.engine.condition.ConditionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Runs "actions" every "interval" ticks while "condition" holds. "on_fail" runs once each time the
 * condition stops holding.
 */
public class OnTickTrait extends PeriodicTrait {
    private final ResourceLocation failStateId;
    private final List<ActionRegistry.RaceAction> actions;
    private final List<ActionRegistry.RaceAction> onFail;
    @Nullable
    private final Condition condition;

    OnTickTrait(JsonObject json) {
        super(TraitIds.of(json), ScalingValue.fromJson(json, "interval", 20.0));
        this.failStateId = traitId.withSuffix("_failed");
        this.actions = ActionRegistry.listFromJson(json, "actions");
        this.onFail = ActionRegistry.listFromJson(json, "on_fail");
        this.condition = ConditionRegistry.optional(json, "condition");
    }

    @Override
    protected boolean shouldExecute(Player player, IPlayerVariables vars) {
        if (condition == null || condition.evaluate(player, null, null, null)) {
            vars.setPersistentState(failStateId, 0.0);
            return true;
        }
        if (vars.getPersistentState(failStateId) == 0.0) {
            vars.setPersistentState(failStateId, 1.0);
            ActionRegistry.runChain(onFail, player, null, null, null);
        }
        return false;
    }

    @Override
    protected void execute(Player player, IPlayerVariables vars) {
        ActionRegistry.runChain(actions, player, null, null, null);
    }

    public static void register() {
        TraitRegistry.register(CreRaces.id("on_tick"), OnTickTrait::new);
    }
}
