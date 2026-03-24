package mc.sayda.creraces.item;

import mc.sayda.creraces.network.BoundaryHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import javax.annotation.Nonnull;

/**
 * An item that opens the racial customization Mirror screen.
 * Consumed on use.
 */
public class MirrorItem extends Item {
    public MirrorItem(Properties properties) {
        super(properties);
    }

    @Override
    public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // Open the Mirror screen on the client
            BoundaryHandler.sendOpenMirror(serverPlayer);

            return java.util.Objects.requireNonNull(InteractionResultHolder.consume(java.util.Objects.requireNonNull(itemStack)));
        }
        return java.util.Objects.requireNonNull(InteractionResultHolder.sidedSuccess(java.util.Objects.requireNonNull(itemStack), level.isClientSide()));
    }

    @Override
    public void inventoryTick(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull net.minecraft.world.entity.Entity entity, int slot,
            boolean selected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            if (!player.isCreative() && !player.isSpectator()) {
                if (!(player.containerMenu instanceof mc.sayda.creraces.world.inventory.MirrorMenu)) {
                    BoundaryHandler.sendOpenMirror(player);
                }
            }
        }
    }
}

            