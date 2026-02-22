package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ConsumeItemAction implements ActionRegistry.RaceAction {

    private final String itemId;
    private final int amount;

    public ConsumeItemAction(String itemId, int amount) {
        this.itemId = itemId;
        this.amount = amount;
    }

    @Override
    public void execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())
                    .toString().equals(itemId)) {
                int toRemove = Math.min(stack.getCount(), amount);
                stack.shrink(toRemove);
                // Currently only consumes from first found stack up to amount.
                // Could be improved to iterate until full amount is consumed.
                break;
            }
        }
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "consume_item"), json -> {
            String item = json.has("item") ? json.get("item").getAsString() : "minecraft:apple";
            int amount = json.has("amount") ? json.get("amount").getAsInt() : 1;
            return new ConsumeItemAction(item, amount);
        });
    }
}
