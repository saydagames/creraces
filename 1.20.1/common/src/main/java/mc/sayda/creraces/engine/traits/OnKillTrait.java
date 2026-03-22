package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonElement;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Trait that triggers actions when the player kills an entity.
 */
public class OnKillTrait implements TraitRegistry.RaceTrait {
    private final List<ActionRegistry.RaceAction> actions;
    private final Condition condition;
    private final boolean bypassSafety;

    public OnKillTrait(List<ActionRegistry.RaceAction> actions, Condition condition, boolean bypassSafety) {
        this.actions = actions;
        this.condition = condition;
        this.bypassSafety = bypassSafety;
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation("creraces:on_kill"), data -> {
            List<ActionRegistry.RaceAction> actions = new ArrayList<>();
            Condition condition = data.has("condition") ? Condition.fromJson(data.getAsJsonObject("condition")) : null;
            boolean bypassSafety = GsonHelper.getAsBoolean(data, "bypass_safety", false);
            if (data.has("actions")) {
                for (JsonElement e : data.getAsJsonArray("actions")) {
                    actions.add(ActionRegistry.fromJson(e.getAsJsonObject()));
                }
            }
            return new OnKillTrait(actions, condition, bypassSafety);
        });
    }

    @Override
    public void onKill(Player player, LivingEntity target) {
        if (!bypassSafety && !mc.sayda.creraces.team.RaceTeamManager.canHurt(target, player)) {
            return;
        }

        if (condition == null || condition.evaluate(player, target, null, null)) {
            for (ActionRegistry.RaceAction action : actions) {
                if (!action.execute(player, target, null, null)) {
                    break;
                }
            }
        }
    }
}
