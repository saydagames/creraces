package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;

/** Drops a stack of an item at the caster's feet. */
public class DropItemAction implements ActionRegistry.RaceAction {
    private final ResourceLocation itemId;
    private final ScalingValue amount;

    public DropItemAction(ResourceLocation itemId, ScalingValue amount) {
        this.itemId = itemId;
        this.amount = amount;
    }

    @SuppressWarnings("null")
    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player.level().isClientSide()) {
            return true;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == Items.AIR) {
            return true;
        }
        int count = (int) amount.evaluate(player, target, slot);
        if (count > 0) {
            player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(),
                    new ItemStack(item, count)));
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "drop_item"), json -> new DropItemAction(
                ResourceLocation.parse(GsonHelper.getAsString(json, "item", "minecraft:air")),
                ScalingValue.fromJson(json, "amount", 1.0)));
    }
}
