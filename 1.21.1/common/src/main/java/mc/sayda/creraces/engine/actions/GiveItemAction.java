package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** Gives the caster items, dropping whatever doesn't fit in the inventory. */
public class GiveItemAction implements ActionRegistry.RaceAction {
    private final ResourceLocation itemId;
    private final ScalingValue amount;

    public GiveItemAction(ResourceLocation itemId, ScalingValue amount) {
        this.itemId = itemId;
        this.amount = amount;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player.level().isClientSide()) {
            return true;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null) {
            return true;
        }
        int count = (int) amount.evaluate(player, target, slot);
        int maxCount = CreRacesConfig.GIVE_ITEM_MAX_COUNT.get();
        if (maxCount > 0) {
            count = Math.min(count, maxCount);
        }
        if (count > 0) {
            ItemStack stack = new ItemStack(item, count);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "give_item"), json -> new GiveItemAction(
                ResourceLocation.parse(GsonHelper.getAsString(json, "item", "minecraft:air")),
                ScalingValue.fromJson(json, "amount", 1.0)));
    }
}
