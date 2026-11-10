package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;

/** Puts "amount" of "item" in a player's inventory, dropping what doesn't fit. */
@SuppressWarnings("null")
public class GiveItemAction implements ActionRegistry.RaceAction {
    private final Item item;
    private final ScalingValue amount;
    private final EntitySelector entity;

    public GiveItemAction(Item item, ScalingValue amount, EntitySelector entity) {
        this.item = item;
        this.amount = amount;
        this.entity = entity;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player.level().isClientSide() || !(entity.resolve(player, target) instanceof Player receiver)) {
            return true;
        }
        int count = (int) amount.evaluate(player, target, slot, interactPos);
        int maxCount = CreRacesConfig.GIVE_ITEM_MAX_COUNT.get();
        if (maxCount > 0) {
            count = Math.min(count, maxCount);
        }
        if (count > 0) {
            ItemStack stack = new ItemStack(item, count);
            if (!receiver.getInventory().add(stack)) {
                receiver.drop(stack, false);
            }
        }
        return true;
    }

    /** The item named by "item"; unknown ids are a data error rather than silently air. */
    static Item parseItem(String text) {
        ResourceLocation id = ResourceLocation.tryParse(text);
        Item item = id != null && BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : null;
        if (item == null || item == Items.AIR) {
            throw new IllegalArgumentException("unknown item '" + text + "'");
        }
        return item;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("give_item"), json -> new GiveItemAction(
                parseItem(GsonHelper.getAsString(json, "item")),
                ScalingValue.fromJson(json, "amount", 1.0),
                EntitySelector.fromJson(json, EntitySelector.SELF)));
    }
}
