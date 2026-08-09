package mc.sayda.creraces.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class FairyBottleItem extends Item {

    public FairyBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return net.minecraft.world.item.ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            // Grant short beneficial effects: regeneration and speed
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0, false, true, true));
        }
        // Return the empty glass bottle via the standard finishUsingItem contract;
        // do not also add it to inventory or the player gets two bottles.
        if (!entity.level().isClientSide() && !(entity instanceof Player p && p.getAbilities().instabuild)) {
            stack.shrink(1);
        }
        return stack.isEmpty() ? new ItemStack(Items.GLASS_BOTTLE) : stack;
    }
}
