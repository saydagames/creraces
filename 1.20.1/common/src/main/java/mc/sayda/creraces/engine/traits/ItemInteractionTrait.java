package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.ItemUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Runs actions when the player right-clicks with a matching item. A match consumes the click even
 * if an action fails; the item is only used up (with "consume") when every action succeeded.
 */
public class ItemInteractionTrait implements TraitRegistry.RaceTrait {
    private final String itemDefinition;
    private final List<ActionRegistry.RaceAction> actions;
    private final boolean consumeItem;
    @Nullable
    private final Condition condition;

    public ItemInteractionTrait(String itemDefinition, List<ActionRegistry.RaceAction> actions, boolean consumeItem,
            @Nullable Condition condition) {
        this.itemDefinition = itemDefinition;
        this.actions = actions;
        this.consumeItem = consumeItem;
        this.condition = condition;
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "item_interaction"), data -> {
            String itemStr = GsonHelper.getAsString(data, "item", "minecraft:air");
            boolean consume = GsonHelper.getAsBoolean(data, "consume", false);
            Condition condition = data.has("condition") ? Condition.fromJson(data.getAsJsonObject("condition")) : null;
            return new ItemInteractionTrait(itemStr, ActionRegistry.listFromJson(data, "actions"), consume, condition);
        });
    }

    @Override
    public boolean onInteraction(Player player, ItemStack stack) {
        if (!ItemUtils.matches(stack, itemDefinition))
            return false;
        if (condition != null && !condition.evaluate(player, null, null, null))
            return false;

        if (ActionRegistry.runChain(actions, player, null, null, null)) {
            if (consumeItem && !player.isCreative()) {
                stack.shrink(1);
            }
        } else {
            CreRaces.LOGGER.debug("ItemInteractionTrait: an action failed or was cancelled, skipping consumption.");
        }
        return true;
    }
}
