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

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One action list shared by several events, picked with "triggers": on_respawn, on_select,
 * on_death and on_item_pickup. The actions never get a target.
 */
public class OnEventsTrait implements TraitRegistry.RaceTrait {

    private final Set<String> triggers;
    private final List<ActionRegistry.RaceAction> actions;
    @Nullable
    private final Condition condition;

    public OnEventsTrait(Set<String> triggers, List<ActionRegistry.RaceAction> actions, @Nullable Condition condition) {
        this.triggers = triggers;
        this.actions = actions;
        this.condition = condition;
    }

    private void fire(Player player) {
        if (condition == null || condition.evaluate(player, null, null, null)) {
            ActionRegistry.runChain(actions, player, null, null, null);
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
            Condition condition = json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null;
            return new OnEventsTrait(triggers, ActionRegistry.listFromJson(json, "actions"), condition);
        });
    }
}
