package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "conditional");

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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        boolean result = condition.evaluate(player, target, slot, interact_pos);
        List<ActionRegistry.RaceAction> actions = result ? ifTrue : ifFalse;

        if (actions.isEmpty()) {
            return true;
        }

        for (ActionRegistry.RaceAction action : actions) {
            if (!action.execute(player, target, slot, interact_pos)) {
                return false;
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            JsonElement condEl = json.get("condition");
            if (condEl == null) {
                CreRaces.LOGGER.error("ConditionalAction missing 'condition' - skipping execution core.");
                return (player, target, slot, interact_pos) -> true;
            }
            Condition condition = Condition.fromJson(condEl.getAsJsonObject());

            List<ActionRegistry.RaceAction> ifTrue = new ArrayList<>();
            if (json.has("if_true")) {
                JsonArray array = json.getAsJsonArray("if_true");
                for (int i = 0; i < array.size(); i++) {
                    ifTrue.add(ActionRegistry.fromJson(array.get(i).getAsJsonObject()));
                }
            }

            List<ActionRegistry.RaceAction> ifFalse = new ArrayList<>();
            if (json.has("if_false")) {
                JsonArray array = json.getAsJsonArray("if_false");
                for (int i = 0; i < array.size(); i++) {
                    ifFalse.add(ActionRegistry.fromJson(array.get(i).getAsJsonObject()));
                }
            }
            return new ConditionalAction(condition, ifTrue, ifFalse);
        });
    }
}
