package mc.sayda.creraces.block;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.client.screen.WaypointEditorScreen;
import mc.sayda.creraces.item.SpiritCompassItem;
import mc.sayda.creraces.registry.ModBlocks;
import mc.sayda.creraces.util.RaceUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Sneak-clicks on a finished torii gate: a held Spirit Compass binds to it (any race), otherwise a
 * kitsune opens their waypoint editor. Vanilla skips a block's own use handler entirely while a
 * player sneaks with anything in hand, so this has to hook the click event itself rather than the
 * block or the item. A sneak-click that matches neither case passes through untouched.
 */
public final class ToriiGateInteractions {

    private ToriiGateInteractions() {
    }

    public static void register() {
        InteractionEvent.RIGHT_CLICK_BLOCK.register(ToriiGateInteractions::onRightClickBlock);
    }

    private static EventResult onRightClickBlock(Player player, InteractionHand hand, BlockPos pos, Direction face) {
        // The event fires once per hand; handle it on the main-hand pass and look at both hands.
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
            return EventResult.pass();
        }
        Level level = player.level();
        if (!level.getBlockState(pos).is(ModBlocks.TORII_BELL.get())) {
            return EventResult.pass();
        }

        ItemStack compass = heldCompass(player);
        if (compass != null) {
            if (!level.isClientSide()) {
                SpiritCompassItem.bindTo(compass, level, pos);
                player.displayClientMessage(Component.translatable("item.creraces.spirit_compass.bound"), true);
            }
            return EventResult.interruptTrue();
        }

        if (RaceUtils.isKitsune(player)) {
            if (level.isClientSide()) {
                BlockPos gate = pos.immutable();
                String dimension = level.dimension().location().toString();
                EnvExecutor.runInEnv(Env.CLIENT, () -> () -> WaypointEditorScreen.openFor(dimension, gate));
            }
            return EventResult.interruptTrue();
        }

        return EventResult.pass();
    }

    @Nullable
    private static ItemStack heldCompass(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof SpiritCompassItem) {
                return stack;
            }
        }
        return null;
    }
}
