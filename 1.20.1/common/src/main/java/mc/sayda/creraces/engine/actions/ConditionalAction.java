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
    public void execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        boolean result = condition.evaluate(player, target, slot);
        List<ActionRegistry.RaceAction> actions = result ? ifTrue : ifFalse;
        actions.forEach(action -> action.execute(player, target, slot));
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
