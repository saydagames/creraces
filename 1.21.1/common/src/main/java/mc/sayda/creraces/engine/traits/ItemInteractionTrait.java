package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.EventContext;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.ItemUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Runs when the player uses an "item" (an id or #tag) from their hand. With "consume", one is used
 * up when the actions complete; a failed or cancelled run keeps it.
 */
public class ItemInteractionTrait extends EventTrait {
    private final String item;
    private final boolean consume;

    ItemInteractionTrait(JsonObject json) {
        super(json, null);
        this.item = GsonHelper.getAsString(json, "item");
        this.consume = GsonHelper.getAsBoolean(json, "consume", false);
    }

    @Override
    public boolean onInteraction(Player player, ItemStack stack) {
        if (!ItemUtils.matches(stack, item)) {
            return false;
        }
        if (fire(player, null, null, EventContext.ofItem(stack)) && consume && !player.isCreative()) {
            stack.shrink(1);
        }
        return true;
    }

    public static void register() {
        TraitRegistry.register(CreRaces.id("item_interaction"), ItemInteractionTrait::new);
    }
}
