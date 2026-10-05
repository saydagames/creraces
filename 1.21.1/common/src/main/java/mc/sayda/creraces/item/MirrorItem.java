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
import java.util.Objects;

/**
 * An item that opens the racial customization Mirror screen.
 * Does not consume the item on use.
 */
public class MirrorItem extends Item {
    public MirrorItem(Properties properties) {
        super(properties);
    }

    @Override
    public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BoundaryHandler.sendOpenMirror(serverPlayer);
            return Objects.requireNonNull(InteractionResultHolder.consume(Objects.requireNonNull(itemStack)));
        }
        return Objects.requireNonNull(InteractionResultHolder.sidedSuccess(Objects.requireNonNull(itemStack), level.isClientSide()));
    }
}
