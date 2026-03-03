package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DomainTrait implements TraitRegistry.RaceTrait {

    private final mc.sayda.creraces.engine.ScalingValue radius;
    private final List<ActionRegistry.RaceAction> actions;
    private final Condition condition;
    private final int interval;
    // Per-player timer (trait is a race-level singleton)
    private final Map<UUID, Integer> timers = new HashMap<>();

    public DomainTrait(mc.sayda.creraces.engine.ScalingValue radius, List<ActionRegistry.RaceAction> actions,
            Condition condition, int interval) {
        this.radius = radius;
        this.actions = actions;
        this.condition = condition;
        this.interval = interval;
    }

    @Override
    public void tick(Player player) {
        if (player.level().isClientSide())
            return;

        int t = timers.getOrDefault(player.getUUID(), 0) + 1;
        timers.put(player.getUUID(), t);
        if (t < interval)
            return;
        timers.put(player.getUUID(), 0);

        if (condition == null || condition.evaluate(player, null, null, null)) {
            // Apply actions to the player while in their own domain
            for (ActionRegistry.RaceAction action : actions) {
                action.execute(player, null, null, null);
            }

            // Regional effects to OTHERS in the domain
            List<Player> others = player.level().getEntitiesOfClass(Player.class,
                    player.getBoundingBox().inflate(radius.evaluate(player)), p -> p != player);
            for (Player other : others) {
                for (ActionRegistry.RaceAction action : actions) {
                    action.execute(player, other, null, null);
                }
            }
        }
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "domain"), json -> {
            mc.sayda.creraces.engine.ScalingValue radius = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                    "radius", 15.0);
            int interval = json.has("interval") ? json.get("interval").getAsInt() : 20;
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
            return new DomainTrait(radius, actions, condition, interval);
        });
    }
}
