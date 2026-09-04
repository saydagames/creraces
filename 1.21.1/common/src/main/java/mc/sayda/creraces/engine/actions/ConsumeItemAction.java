package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.Objects;

public class ConsumeItemAction implements ActionRegistry.RaceAction {

    @Nullable private final ResourceLocation itemId;
    @Nullable private final TagKey<Item>    itemTag;
    private final ScalingValue amount;

    private ConsumeItemAction(@Nullable ResourceLocation itemId,
                               @Nullable TagKey<Item> itemTag,
                               ScalingValue amount) {
        this.itemId  = itemId;
        this.itemTag = itemTag;
        this.amount  = amount;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @Nullable net.minecraft.core.BlockPos interact_pos) {
        int toRemove = (int) amount.evaluate(player, target, slot);
        if (toRemove <= 0) return true;

        // Prioritize main hand
        int selected = player.getInventory().selected;
        ItemStack mainHand = player.getInventory().getItem(selected);
        if (!mainHand.isEmpty() && matches(mainHand)) {
            int take = Math.min(mainHand.getCount(), toRemove);
            mainHand.shrink(take);
            toRemove -= take;
        }

        // Fall through to the rest of the inventory
        for (int i = 0; i < player.getInventory().getContainerSize() && toRemove > 0; i++) {
            if (i == selected) continue;
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && matches(stack)) {
                int take = Math.min(stack.getCount(), toRemove);
                stack.shrink(take);
                toRemove -= take;
            }
        }
        return true;
    }

    private boolean matches(ItemStack stack) {
        if (itemTag != null)  return stack.is(itemTag);
        if (itemId  != null) {
            Item target = BuiltInRegistries.ITEM.get(itemId);
            return target != null && target != Items.AIR && stack.is(target);
        }
        return false;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "consume_item"), json -> {
            String itemStr = GsonHelper.getAsString(json, "item", "minecraft:air");
            ScalingValue amount = ScalingValue.fromJson(json, "amount", 1.0);

            if (itemStr.startsWith("#")) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM,
                        ResourceLocation.parse(Objects.requireNonNull(itemStr.substring(1))));
                return new ConsumeItemAction(null, tag, amount);
            }
            return new ConsumeItemAction(
                    ResourceLocation.parse(Objects.requireNonNull(itemStr)), null, amount);
        });
    }
}
