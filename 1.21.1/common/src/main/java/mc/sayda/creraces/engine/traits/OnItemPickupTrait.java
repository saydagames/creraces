package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.EventContext;
import mc.sayda.creraces.engine.TraitRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Runs when the player picks an item up; event_item and event:item_count read it. */
public class OnItemPickupTrait extends EventTrait {
    OnItemPickupTrait(JsonObject json) {
        super(json, null);
    }

    @Override
    public void onItemPickup(Player player, ItemStack stack) {
        fire(player, null, null, EventContext.ofItem(stack));
    }

    public static void register() {
        TraitRegistry.register(CreRaces.id("on_item_pickup"), OnItemPickupTrait::new);
    }
}
