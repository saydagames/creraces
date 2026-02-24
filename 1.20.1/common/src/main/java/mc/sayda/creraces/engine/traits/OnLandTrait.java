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
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Trait that executes actions when a player lands on the ground.
 */
public class OnLandTrait implements TraitRegistry.RaceTrait {

    private final List<ActionRegistry.RaceAction> actions;
    private final Condition condition;
    private final Map<UUID, Boolean> lastOnGround = new WeakHashMap<>();

    public OnLandTrait(List<ActionRegistry.RaceAction> actions, Condition condition) {
        this.actions = actions;
        this.condition = condition;
    }

    @Override
    public void tick(Player player) {
        boolean onGround = player.onGround();
        boolean wasOnGround = lastOnGround.getOrDefault(player.getUUID(), true);

        if (onGround && !wasOnGround) {
            if (condition == null || condition.evaluate(player, null, null, null)) {
                for (ActionRegistry.RaceAction action : actions) {
                    action.execute(player, null, null, null);
                }
            }
        }

        lastOnGround.put(player.getUUID(), onGround);
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "on_land"), json -> {
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

            return new OnLandTrait(actions, condition);
        });
    }
}
