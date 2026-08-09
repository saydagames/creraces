package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonArray;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OnEventsTrait implements TraitRegistry.RaceTrait {

    private final Set<String> triggers;
    private final List<ActionRegistry.RaceAction> actions;
    private final Condition condition;

    public OnEventsTrait(Set<String> triggers, List<ActionRegistry.RaceAction> actions, Condition condition) {
        this.triggers = triggers;
        this.actions = actions;
        this.condition = condition;
    }

    private void fire(Player player) {
        if (condition == null || condition.evaluate(player, null, null, null)) {
            for (ActionRegistry.RaceAction action : actions) {
                if (!action.execute(player, null, null, null)) break;
            }
        }
    }

    @Override
    public void onRespawn(Player player) {
        if (triggers.contains("on_respawn")) fire(player);
    }

    @Override
    public void onSelect(Player player) {
        if (triggers.contains("on_select")) fire(player);
    }

    @Override
    public void onDeath(Player player, DamageSource source) {
        if (triggers.contains("on_death")) fire(player);
    }

    @Override
    public void onItemPickup(Player player, ItemStack stack) {
        if (triggers.contains("on_item_pickup")) fire(player);
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "on_events"), json -> {
            Set<String> triggers = new HashSet<>();
            if (json.has("triggers")) {
                JsonArray array = json.getAsJsonArray("triggers");
                for (int i = 0; i < array.size(); i++) {
                    triggers.add(array.get(i).getAsString());
                }
            }
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
            return new OnEventsTrait(triggers, actions, condition);
        });
    }
}
