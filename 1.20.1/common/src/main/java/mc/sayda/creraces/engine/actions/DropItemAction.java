package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Action that drops an item at the player's location.
 */
public class DropItemAction implements ActionRegistry.RaceAction {
    private final ResourceLocation itemId;
    private final ScalingValue amount;

    public DropItemAction(ResourceLocation itemId, ScalingValue amount) {
        this.itemId = itemId;
        this.amount = amount;
    }

    @SuppressWarnings("null")
    @Override
    public boolean execute(Player player, @Nullable net.minecraft.world.entity.LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @Nullable net.minecraft.core.BlockPos interact_pos) {
        if (player.level() == null || player.level().isClientSide()) return true;
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item != null && item != net.minecraft.world.item.Items.AIR) {
            int c = (int) amount.evaluate(player, target, slot);
            if (c <= 0)
                return true;
            ItemEntity itemEntity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(),
                    new ItemStack(item, c));
            player.level().addFreshEntity(itemEntity);
        }
        return true;
    }

    @SuppressWarnings("null")
    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "drop_item"), json -> {
            @SuppressWarnings("null")
            String idStr = GsonHelper.getAsString(json, "item", "minecraft:air");
            ResourceLocation itemId = new ResourceLocation(idStr);
            ScalingValue amount = ScalingValue.fromJson(json, "amount", 1.0);
            return new DropItemAction(itemId, amount);
        });
    }
}
