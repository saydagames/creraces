package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
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
    private final int count;

    public DropItemAction(ResourceLocation itemId, int count) {
        this.itemId = itemId;
        this.count = count;
    }

    @Override
    public boolean execute(Player player, @Nullable net.minecraft.world.entity.LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @Nullable net.minecraft.core.BlockPos interactionPos) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item != null && player.level() != null) {
            ItemEntity itemEntity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(),
                    new ItemStack(item, count));
            player.level().addFreshEntity(itemEntity);
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "drop_item"), json -> {
            ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(json, "item", "minecraft:air"));
            int count = GsonHelper.getAsInt(json, "count", 1);
            return new DropItemAction(itemId, count);
        });
    }
}
