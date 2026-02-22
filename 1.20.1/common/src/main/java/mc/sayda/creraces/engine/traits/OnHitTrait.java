package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.List;

public class OnHitTrait implements TraitRegistry.RaceTrait {
    private final List<ActionRegistry.RaceAction> actions = new ArrayList<>();
    private final Condition condition;
    private final boolean bypassSafety;

    public OnHitTrait(JsonObject data) {
        this.condition = data.has("condition") ? Condition.fromJson(data.getAsJsonObject("condition")) : null;
        this.bypassSafety = GsonHelper.getAsBoolean(data, "bypass_safety", false);
        if (data.has("actions")) {
            for (JsonElement e : data.getAsJsonArray("actions")) {
                actions.add(ActionRegistry.fromJson(e.getAsJsonObject()));
            }
        }
    }

    @Override
    public void onHit(Player player, LivingEntity target) {
        if (!bypassSafety && !mc.sayda.creraces.team.RaceTeamManager.canHurt(target, player)) {
            return;
        }

        if (condition == null || condition.evaluate(player, target, null)) {
            actions.forEach(action -> action.execute(player, target, null));
        }
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation("creraces", "on_hit"), OnHitTrait::new);
    }
}
