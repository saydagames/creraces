package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.ItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Takes "amount" of "item" (an id or #tag) out of the caster's inventory, the held stack first.
 * With "fail_if_insufficient", having too few takes nothing and fails the action, so it works as a cost.
 */
public class ConsumeItemAction implements ActionRegistry.RaceAction {
    private final String item;
    private final ScalingValue amount;
    private final boolean failIfInsufficient;

    public ConsumeItemAction(String item, ScalingValue amount, boolean failIfInsufficient) {
        this.item = item;
        this.amount = amount;
        this.failIfInsufficient = failIfInsufficient;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        int toRemove = (int) amount.evaluate(player, target, slot, interactPos);
        if (toRemove <= 0) {
            return true;
        }
        Inventory inventory = player.getInventory();
        if (failIfInsufficient && count(inventory) < toRemove) {
            return false;
        }
        toRemove = consumeFrom(inventory.getItem(inventory.selected), toRemove);
        for (int i = 0; i < inventory.getContainerSize() && toRemove > 0; i++) {
            if (i != inventory.selected) {
                toRemove = consumeFrom(inventory.getItem(i), toRemove);
            }
        }
        return true;
    }

    private int count(Inventory inventory) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (ItemUtils.matches(stack, item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Shrinks a matching stack by up to {@code toRemove} and returns how many are still left to remove. */
    private int consumeFrom(ItemStack stack, int toRemove) {
        if (!ItemUtils.matches(stack, item)) {
            return toRemove;
        }
        int take = Math.min(stack.getCount(), toRemove);
        stack.shrink(take);
        return toRemove - take;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("consume_item"), json -> new ConsumeItemAction(
                GsonHelper.getAsString(json, "item"),
                ScalingValue.fromJson(json, "amount", 1.0),
                GsonHelper.getAsBoolean(json, "fail_if_insufficient", false)));
    }
}
