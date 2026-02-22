package mc.sayda.creraces.item;

import mc.sayda.creraces.network.BoundaryHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * An item that opens the racial customization Mirror screen.
 * Consumed on use.
 */
public class MirrorItem extends Item {
    public MirrorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // Open the Mirror screen on the client
            BoundaryHandler.sendOpenMirror(serverPlayer);

            // Consume the item
            if (!player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }
            return InteractionResultHolder.consume(itemStack);
        }
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }
}
