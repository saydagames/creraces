package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonElement;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import mc.sayda.creraces.util.ItemUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Trait that triggers actions when the player right-clicks with a specific
 * item.
 */
public class ItemInteractionTrait implements TraitRegistry.RaceTrait {
    private final String itemDefinition;
    private final List<ActionRegistry.RaceAction> actions;
    private final boolean consumeItem;
    private final Condition condition;

    public ItemInteractionTrait(String itemDefinition, List<ActionRegistry.RaceAction> actions, boolean consumeItem,
            Condition condition) {
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
            List<ActionRegistry.RaceAction> actions = new ArrayList<>();
            if (data.has("actions")) {
                for (JsonElement e : data.getAsJsonArray("actions")) {
                    actions.add(ActionRegistry.fromJson(e.getAsJsonObject()));
                }
            }
            return new ItemInteractionTrait(itemStr, actions, consume, condition);
        });
    }

    @Override
    public boolean onInteraction(Player player, ItemStack stack) {
        if (ItemUtils.matches(stack, itemDefinition)) {
            if (condition != null && !condition.evaluate(player, null, null, null)) {
                return false;
            }

            boolean success = true;
            for (ActionRegistry.RaceAction action : actions) {
                if (!action.execute(player, null, null, null)) {
                    success = false;
                    mc.sayda.creraces.CreRaces.LOGGER
                            .debug("ItemInteractionTrait: Action failed/cancelled, aborting consumption.");
                    break;
                }
            }

            if (success && consumeItem && !player.isCreative()) {
                stack.shrink(1);
            } else if (!success) {
                mc.sayda.creraces.CreRaces.LOGGER
                        .debug("ItemInteractionTrait: Actions failed/cancelled, skipping consumption.");
            }
                        
                return true;
        }
        return false;
                        
    }
}
