package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.Ability;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Trait that triggers actions when the player uses ANY ability.
 */
public class OnAbilityUseTrait implements TraitRegistry.RaceTrait {
    private final List<ActionRegistry.RaceAction> actions;
    private final Condition condition;

    public OnAbilityUseTrait(List<ActionRegistry.RaceAction> actions, Condition condition) {
        this.actions = actions;
        this.condition = condition;
    }

    @Override
    public void onAbilityUse(Player player, Ability ability) {
        if (condition == null || condition.evaluate(player, null, null)) {
            for (ActionRegistry.RaceAction action : actions) {
                action.execute(player, null, null);
            }
        }
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "on_ability_use"), json -> {
            Condition condition = json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null;
            List<ActionRegistry.RaceAction> actions = new ArrayList<>();
            if (json.has("actions") && json.get("actions").isJsonArray()) {
                for (JsonElement e : json.getAsJsonArray("actions")) {
                    actions.add(ActionRegistry.fromJson(e.getAsJsonObject()));
                }
            }
            return new OnAbilityUseTrait(actions, condition);
        });
    }
}
