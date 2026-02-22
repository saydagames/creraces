package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Trait for passive abilities that execute actions periodically.
 * Example: Harpy hunger drain while flying, auto-regen on low health, etc.
 */
public class OnTickTrait implements TraitRegistry.RaceTrait {

    private final List<ActionRegistry.RaceAction> actions;
    private final List<ActionRegistry.RaceAction> onFail;
    private final int interval;
    private final Condition condition;
    private int tickCounter = 0;
    private boolean failed = false;

    public OnTickTrait(List<ActionRegistry.RaceAction> actions, List<ActionRegistry.RaceAction> onFail, int interval,
            Condition condition) {
        this.actions = actions;
        this.onFail = onFail;
        this.interval = interval;
        this.condition = condition;
    }

    @Override
    public void tick(Player player) {
        tickCounter++;
        if (tickCounter >= interval) {
            tickCounter = 0;

            if (condition != null && !condition.evaluate(player, null, null)) {
                if (!failed) {
                    failed = true;
                    for (ActionRegistry.RaceAction action : onFail) {
                        action.execute(player, null, null);
                    }
                }
                return;
            }

            failed = false;
            for (ActionRegistry.RaceAction action : actions) {
                action.execute(player, null, null);
            }
        }
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "on_tick"), json -> {
            int interval = GsonHelper.getAsInt(json, "interval", 20); // Default: 1 second
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

            return new OnTickTrait(actions, onFail, interval, condition);
        });
    }
}
