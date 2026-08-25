package mc.sayda.creraces.item;

import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class TowelItem extends Item {
    public TowelItem(Properties properties) {
        super(properties.durability(200));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.CROSSBOW; // CROSSBOW anim plays the "pull-back" charge without showing a crossbow or arrow
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 20; // 1 second to use
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            var effect = mc.sayda.creraces.registry.ModMobEffects.SOGGY.get();
            if (effect != null && player.hasEffect(effect)) {
                player.removeEffect(effect);
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
