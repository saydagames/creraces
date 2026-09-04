package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonArray;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class OnSelectTrait implements TraitRegistry.RaceTrait {

    private final List<ActionRegistry.RaceAction> actions;
    private final Condition condition;

    public OnSelectTrait(List<ActionRegistry.RaceAction> actions, Condition condition) {
        this.actions = actions;
        this.condition = condition;
    }

    @Override
    public void onSelect(Player player) {
        if (condition == null || condition.evaluate(player, null, null, null)) {
            for (ActionRegistry.RaceAction action : actions) {
                if (!action.execute(player, null, null, null)) {
                    break;
                }
            }
        }
    }

    public static void register() {
        TraitRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "on_select"), json -> {
            List<ActionRegistry.RaceAction> actions = new ArrayList<>();
            if (json.has("actions")) {
                JsonArray array = json.getAsJsonArray("actions");
                for (int i = 0; i < array.size(); i++) {
                    actions.add(ActionRegistry.fromJson(array.get(i).getAsJsonObject()));
                }
            }
            Condition condition = null;
            if (json.has("condition")) {
                condition = Condition.fromJson(json.getAsJsonObject("condition"));
            }
            return new OnSelectTrait(actions, condition);
        });
    }
}
