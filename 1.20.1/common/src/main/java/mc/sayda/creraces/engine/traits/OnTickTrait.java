package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Runs actions every interval while the condition holds, e.g. hunger drain while flying or regen at
 * low health. on_fail runs once each time the condition goes from passing to failing.
 */
public class OnTickTrait extends PeriodicTrait {

    private final ResourceLocation failStateId;
    private final List<ActionRegistry.RaceAction> actions;
    private final List<ActionRegistry.RaceAction> onFail;
    @Nullable
    private final Condition condition;

    public OnTickTrait(ResourceLocation traitId, List<ActionRegistry.RaceAction> actions,
            List<ActionRegistry.RaceAction> onFail, ScalingValue interval, @Nullable Condition condition) {
        super(traitId, interval);
        this.failStateId = traitId.withSuffix("_failed");
        this.actions = actions;
        this.onFail = onFail;
        this.condition = condition;
    }

    @Override
    protected boolean shouldExecute(Player player, IPlayerVariables vars) {
        boolean success = condition == null || condition.evaluate(player, null, null, null);

        if (!success) {
            if (vars.getPersistentState(failStateId) == 0.0) {
                vars.setPersistentState(failStateId, 1.0);
                for (ActionRegistry.RaceAction action : onFail) {
                    action.execute(player, null, null, null);
                }
            }
            return false;
        }

        vars.setPersistentState(failStateId, 0.0);
        return true;
    }

    @Override
    protected void execute(Player player, IPlayerVariables vars) {
        ActionRegistry.runChain(actions, player, null, null, null);
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "on_tick"), json -> {
            ScalingValue interval = ScalingValue.fromJson(json, "interval", 20.0);
            Condition condition = json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null;
            return new OnTickTrait(TraitIds.fromJson(json, "ontick_"), ActionRegistry.listFromJson(json, "actions"),
                    ActionRegistry.listFromJson(json, "on_fail"), interval, condition);
        });
    }
}
