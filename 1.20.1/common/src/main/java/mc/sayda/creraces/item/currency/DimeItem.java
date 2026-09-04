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
import javax.annotation.Nullable;
import java.util.List;

public class DimeItem extends Item {
    public DimeItem(Properties properties) {
        super(properties);
    }

    @Override
    public @Nonnull UseAnim getUseAnimation(@Nonnull ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level, @Nonnull List<Component> tooltip,
            @Nonnull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.creraces.dime.description_0"));
    }

    @Override
    public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player,
            @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        addCoin(level, player, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull Entity entity, int slotId,
            boolean isSelected) {
        if (!level.isClientSide() && entity instanceof Player player && stack.getCount() > 1) {
            addCoin(level, player, stack);
        }
    }

    private void addCoin(Level level, Player player, ItemStack stack) {
        if (level.isClientSide())
            return;

        DataUtils.getVariables(player).ifPresent(vars -> {
            vars.setCoins(vars.getCoins() + 10);
            vars.sync(player);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.COIN_PICKUP_1.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

            // Removes 1 across the whole inventory (matches Classic), not just from this stack,
            // otherwise every separate dime stack independently decays down to 1 instead of the
            // player ending up with exactly 1 dime total.
            player.getInventory().clearOrCountMatchingItems(
                    s -> s.getItem() == stack.getItem(), 1, player.inventoryMenu.getCraftSlots());
        });
    }
}
