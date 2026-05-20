package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Action that gives an item to the player.
 */
public class GiveItemAction implements ActionRegistry.RaceAction {
    private final ResourceLocation itemId;
    private final ScalingValue amount;

    public GiveItemAction(ResourceLocation itemId, ScalingValue amount) {
        this.itemId = itemId;
        this.amount = amount;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @Nullable net.minecraft.core.BlockPos interact_pos) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item != null) {
            int count = (int) amount.evaluate(player, target, slot);
            int maxCount = mc.sayda.creraces.config.CreRacesConfig.GIVE_ITEM_MAX_COUNT.get();
            if (maxCount > 0) count = Math.min(count, maxCount);
            if (count > 0) {
                ItemStack stack = new ItemStack(item, count);
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "give_item"), json -> {
            String idStr = GsonHelper.getAsString(json, "item", "minecraft:air");
            ResourceLocation itemId = new ResourceLocation(idStr);
            ScalingValue amount = ScalingValue.fromJson(json, "amount", 1.0);
            return new GiveItemAction(itemId, amount);
        });
    }
}
