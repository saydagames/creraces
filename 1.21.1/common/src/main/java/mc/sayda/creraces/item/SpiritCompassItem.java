package mc.sayda.creraces.item;

import mc.sayda.creraces.util.ItemNbt;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A lodestone-style needle. Sneak-ringing a finished torii bell while holding one binds it to
 * that gate. Right-clicking reads out the current binding without changing it. The needle spins
 * while unbound or while the holder is in a different dimension than the bound gate, and
 * otherwise points at it.
 */
public class SpiritCompassItem extends Item {
    private static final String TAG_HAS_TARGET = "HasTarget";
    private static final String TAG_X = "TargetX";
    private static final String TAG_Y = "TargetY";
    private static final String TAG_Z = "TargetZ";
    private static final String TAG_DIMENSION = "TargetDim";

    public SpiritCompassItem(Properties properties) {
        super(properties);
    }

    /** Binds this stack to a gate. Called from the torii bell on a sneak-ring. */
    public static void bindTo(ItemStack stack, Level level, BlockPos pos) {
        ItemNbt.mutate(stack, tag -> {
            tag.putInt(TAG_X, pos.getX());
            tag.putInt(TAG_Y, pos.getY());
            tag.putInt(TAG_Z, pos.getZ());
            tag.putString(TAG_DIMENSION, level.dimension().location().toString());
            tag.putBoolean(TAG_HAS_TARGET, true);
        });
    }

    /**
     * Model predicate value for the needle, 0.0-1.0, on vanilla's compass scale: 0 is straight
     * ahead and 0.25 is to the right.
     */
    public static float needleAngle(ItemStack stack, @Nullable Level level, @Nullable Entity holder) {
        // A compass in an item frame has no holder, so it reads the frame's facing, as vanilla does.
        Entity entity = holder != null ? holder : stack.getEntityRepresentation();
        if (entity == null) {
            return 0f;
        }
        Level world = level != null ? level : entity.level();
        CompoundTag tag = ItemNbt.get(stack);
        boolean sameDim = world.dimension().location().toString().equals(tag.getString(TAG_DIMENSION));
        if (!tag.getBoolean(TAG_HAS_TARGET) || !sameDim) {
            return (world.getGameTime() % 32) / 32f;
        }
        double dx = tag.getInt(TAG_X) + 0.5 - entity.getX();
        double dz = tag.getInt(TAG_Z) + 0.5 - entity.getZ();
        // atan2 measures from east while yaw 0 faces south, hence the quarter turn
        double bearing = Math.atan2(dz, dx) / (Math.PI * 2) - 0.25;
        double facing = entity.getVisualRotationYInDegrees() / 360.0;
        return Mth.positiveModulo((float) (bearing - facing), 1.0F);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return ItemNbt.get(stack).getBoolean(TAG_HAS_TARGET);
    }

    /** Read-only: prints the current binding to the action bar without changing it. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            player.displayClientMessage(trackingMessage(ItemNbt.get(stack)), true);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = ItemNbt.get(stack);
        if (tag.getBoolean(TAG_HAS_TARGET)) {
            tooltip.add(Component.translatable("item.creraces.spirit_compass.tracking",
                    tag.getInt(TAG_X), tag.getInt(TAG_Y), tag.getInt(TAG_Z))
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.creraces.spirit_compass.tracking_dim", tag.getString(TAG_DIMENSION))
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("item.creraces.spirit_compass.unbound")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static Component trackingMessage(CompoundTag tag) {
        if (!tag.getBoolean(TAG_HAS_TARGET)) {
            return Component.translatable("item.creraces.spirit_compass.unbound");
        }
        return Component.translatable("item.creraces.spirit_compass.tracking_actionbar",
                tag.getInt(TAG_X), tag.getInt(TAG_Y), tag.getInt(TAG_Z), tag.getString(TAG_DIMENSION));
    }
}
