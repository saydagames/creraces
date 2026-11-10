package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Takes an item from the target player into the caster's inventory: the "slot" "mainhand",
 * "offhand" or "random" (a random filled hotbar slot). Use the generic "chance" for a chance to fail.
 */
public class StealItemAction implements ActionRegistry.RaceAction {
    private final String slot;

    public StealItemAction(String slot) {
        this.slot = slot;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot abilitySlot,
            @Nullable BlockPos interactPos) {
        if (!(target instanceof Player victim)) {
            return true;
        }
        ItemStack stolen = switch (slot) {
            case "mainhand" -> take(victim.getMainHandItem());
            case "offhand" -> take(victim.getOffhandItem());
            default -> takeRandomHotbarItem(player, victim.getInventory());
        };
        if (!stolen.isEmpty() && !player.getInventory().add(stolen)) {
            player.drop(stolen, false);
        }
        return true;
    }

    private static ItemStack take(ItemStack stack) {
        ItemStack copy = stack.copy();
        stack.setCount(0);
        return copy;
    }

    private static ItemStack takeRandomHotbarItem(Player thief, Inventory inventory) {
        List<Integer> filled = new ArrayList<>();
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            if (!inventory.getItem(i).isEmpty()) {
                filled.add(i);
            }
        }
        if (filled.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int picked = filled.get(thief.level().random.nextInt(filled.size()));
        ItemStack stolen = inventory.getItem(picked).copy();
        inventory.setItem(picked, ItemStack.EMPTY);
        return stolen;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("steal_item"), json -> {
            String slot = GsonHelper.getAsString(json, "slot", "random");
            if (!slot.equals("mainhand") && !slot.equals("offhand") && !slot.equals("random")) {
                throw new IllegalArgumentException("unknown slot '" + slot + "' (mainhand, offhand, random)");
            }
            return new StealItemAction(slot);
        });
    }
}
