package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.List;
import mc.sayda.creraces.engine.condition.Condition;

/**
 * Action that executes sub-actions based on conditions.
 */
public class ConditionalAction implements ActionRegistry.RaceAction {

    private final Condition condition;
    private final List<ActionRegistry.RaceAction> ifTrue;
    private final List<ActionRegistry.RaceAction> ifFalse;

    public ConditionalAction(Condition condition, List<ActionRegistry.RaceAction> ifTrue,
            List<ActionRegistry.RaceAction> ifFalse) {
        this.condition = condition;
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        boolean result = condition.evaluate(player, target, slot, interactionPos);
        List<ActionRegistry.RaceAction> actions = result ? ifTrue : ifFalse;

        if (actions.isEmpty()) {
            // If the condition failed and there are no 'if_false' actions, return false.
            // If the condition succeeded and there are no 'if_true' actions, return true.
            return result;
        }

        for (ActionRegistry.RaceAction action : actions) {
            if (!action.execute(player, target, slot, interactionPos)) {
                return false;
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "conditional"), json -> {
            JsonObject condObj = json.getAsJsonObject("condition");
            Condition condition = Condition.fromJson(condObj);

            List<ActionRegistry.RaceAction> ifTrue = new ArrayList<>();
            if (json.has("if_true")) {
                for (JsonElement e : json.getAsJsonArray("if_true")) {
                    ifTrue.add(ActionRegistry.fromJson(e.getAsJsonObject()));
                }
            }

            List<ActionRegistry.RaceAction> ifFalse = new ArrayList<>();
            if (json.has("if_false")) {
                for (JsonElement e : json.getAsJsonArray("if_false")) {
                    ifFalse.add(ActionRegistry.fromJson(e.getAsJsonObject()));
                }
            }

            return new ConditionalAction(condition, ifTrue, ifFalse);
        });
    }
}
