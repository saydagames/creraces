package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * With a chance, takes a target player's item into the caster's inventory: the "mainhand" or
 * "offhand" stack, or a "random" non-empty hotbar stack.
 */
public class StealItemAction implements ActionRegistry.RaceAction {

    private static final double DEFAULT_CHANCE = 0.5;

    // 1.0 unless the JSON leaves out "chance": an explicit one is already rolled by the generic wrapper
    // in ActionRegistry.fromJson, and rolling it here as well would square it.
    private final double ownChance;
    private final String targetSlot;

    public StealItemAction(double ownChance, String targetSlot) {
        this.ownChance = ownChance;
        this.targetSlot = targetSlot;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!(target instanceof Player victim)) {
            return true;
        }
        if (player.level().random.nextDouble() >= ownChance) {
            return true;
        }

        ItemStack stolen = switch (targetSlot) {
            case "mainhand" -> take(victim.getMainHandItem());
            case "offhand" -> take(victim.getOffhandItem());
            case "random" -> takeRandomHotbarItem(player, victim.getInventory());
            default -> ItemStack.EMPTY;
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
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "steal_item"), json -> new StealItemAction(
                json.has("chance") ? 1.0 : DEFAULT_CHANCE,
                GsonHelper.getAsString(json, "slot", "random")));
    }
}
