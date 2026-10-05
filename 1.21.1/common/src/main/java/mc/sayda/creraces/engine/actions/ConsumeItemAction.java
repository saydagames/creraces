package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.Objects;

/** Removes items (an id, or a tag with a leading "#") from the caster's inventory, main hand first. */
public class ConsumeItemAction implements ActionRegistry.RaceAction {

    @Nullable
    private final ResourceLocation itemId;
    @Nullable
    private final TagKey<Item> itemTag;
    private final ScalingValue amount;

    private ConsumeItemAction(@Nullable ResourceLocation itemId, @Nullable TagKey<Item> itemTag, ScalingValue amount) {
        this.itemId = itemId;
        this.itemTag = itemTag;
        this.amount = amount;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        int toRemove = (int) amount.evaluate(player, target, slot);
        if (toRemove <= 0) {
            return true;
        }

        Inventory inventory = player.getInventory();
        toRemove = consumeFrom(inventory.getItem(inventory.selected), toRemove);
        for (int i = 0; i < inventory.getContainerSize() && toRemove > 0; i++) {
            if (i != inventory.selected) {
                toRemove = consumeFrom(inventory.getItem(i), toRemove);
            }
        }
        return true;
    }

    /** Shrinks a matching stack by up to {@code toRemove} and returns how many are still left to remove. */
    private int consumeFrom(ItemStack stack, int toRemove) {
        if (stack.isEmpty() || !matches(stack)) {
            return toRemove;
        }
        int take = Math.min(stack.getCount(), toRemove);
        stack.shrink(take);
        return toRemove - take;
    }

    private boolean matches(ItemStack stack) {
        if (itemTag != null) {
            return stack.is(itemTag);
        }
        if (itemId != null) {
            Item item = BuiltInRegistries.ITEM.get(itemId);
            return item != null && item != Items.AIR && stack.is(item);
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
            return new ConsumeItemAction(ResourceLocation.parse(itemStr), null, amount);
        });
    }
}
