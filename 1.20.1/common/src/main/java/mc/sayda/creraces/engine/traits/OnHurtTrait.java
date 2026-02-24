package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class OnHurtTrait implements TraitRegistry.RaceTrait {

    private final List<ActionRegistry.RaceAction> actions;
    private final Condition condition;

    public OnHurtTrait(List<ActionRegistry.RaceAction> actions, Condition condition) {
        this.actions = actions;
        this.condition = condition;
    }

    @Override
    public void onHurt(Player player, DamageSource source, float amount) {
        if (condition == null || condition.evaluate(player,
                source.getEntity() instanceof net.minecraft.world.entity.LivingEntity le ? le : null, null, null)) {
            for (ActionRegistry.RaceAction action : actions) {
                action.execute(player,
                        source.getEntity() instanceof net.minecraft.world.entity.LivingEntity le ? le : null, null,
                        null);
            }
        }
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "on_hurt"), json -> {
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
            return new OnHurtTrait(actions, condition);
        });
    }
}
