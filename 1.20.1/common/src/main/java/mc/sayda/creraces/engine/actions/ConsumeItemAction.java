package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ConsumeItemAction implements ActionRegistry.RaceAction {

    private final ResourceLocation itemId;
    private final ScalingValue amount;

    public ConsumeItemAction(ResourceLocation itemId, ScalingValue amount) {
        this.itemId = itemId;
        this.amount = amount;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        int toRemoveTotal = (int) amount.evaluate(player, target);
        if (toRemoveTotal <= 0)
            return true;

        net.minecraft.world.item.Item targetItem = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .get(Objects.requireNonNull(itemId));
        if (targetItem == net.minecraft.world.item.Items.AIR)
            return true;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(targetItem)) {
                int toRemove = Math.min(stack.getCount(), toRemoveTotal);
                stack.shrink(toRemove);
                toRemoveTotal -= toRemove;
                if (toRemoveTotal <= 0)
                    break;
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "consume_item"), json -> {
            String itemStr = GsonHelper.getAsString(json, "item", "minecraft:air");
            ResourceLocation item = new ResourceLocation(Objects.requireNonNull(itemStr));
            ScalingValue amount = ScalingValue.fromJson(json, "amount", 1.0);
            return new ConsumeItemAction(item, amount);
        });
    }
}
