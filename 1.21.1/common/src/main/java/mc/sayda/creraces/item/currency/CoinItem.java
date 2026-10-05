package mc.sayda.creraces.item.currency;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A coin that cashes itself in: using one adds its value to the player's coin balance, and a
 * stack of more than one is cashed in a coin per tick while it sits in the inventory.
 */
public class CoinItem extends Item {
    private final int value;
    private final String descriptionKey;

    protected CoinItem(Properties properties, int value, String descriptionKey) {
        super(properties);
        this.value = value;
        this.descriptionKey = descriptionKey;
    }

    @Override
    public @Nonnull UseAnim getUseAnimation(@Nonnull ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, Item.TooltipContext context, @Nonnull List<Component> tooltip,
            @Nonnull TooltipFlag flag) {
        tooltip.add(Component.translatable(descriptionKey));
    }

    @Override
    public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player,
            @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        cashIn(level, player, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull Entity entity, int slotId,
            boolean isSelected) {
        if (!level.isClientSide() && entity instanceof Player player && stack.getCount() > 1) {
            cashIn(level, player, stack);
        }
    }

    private void cashIn(Level level, Player player, ItemStack stack) {
        if (level.isClientSide())
            return;

        DataUtils.getVariables(player).ifPresent(vars -> {
            vars.setCoins(vars.getCoins() + value);
            vars.sync(player);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.COIN_PICKUP_1.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

            // Removes 1 across the whole inventory (matches Classic), not just from this stack,
            // otherwise every separate stack of the coin independently decays down to 1 instead
            // of the player ending up with exactly 1 coin total.
            player.getInventory().clearOrCountMatchingItems(
                    s -> s.getItem() == stack.getItem(), 1, player.inventoryMenu.getCraftSlots());
        });
    }
}
