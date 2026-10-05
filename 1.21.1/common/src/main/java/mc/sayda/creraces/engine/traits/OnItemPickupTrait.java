package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/** Runs actions when the player picks up an item. */
public class OnItemPickupTrait implements TraitRegistry.RaceTrait {
    private final List<ActionRegistry.RaceAction> actions;
    @Nullable
    private final Condition condition;

    public OnItemPickupTrait(List<ActionRegistry.RaceAction> actions, @Nullable Condition condition) {
        this.actions = actions;
        this.condition = condition;
    }

    @Override
    public void onItemPickup(Player player, ItemStack stack) {
        if (condition == null || condition.evaluate(player, null, null, null)) {
            ActionRegistry.runChain(actions, player, null, null, null);
        }
    }

    public static void register() {
        TraitRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "on_item_pickup"), json -> {
            Condition condition = json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null;
            return new OnItemPickupTrait(ActionRegistry.listFromJson(json, "actions"), condition);
        });
    }
}
