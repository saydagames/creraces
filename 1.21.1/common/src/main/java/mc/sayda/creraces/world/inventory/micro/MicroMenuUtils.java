package mc.sayda.creraces.world.inventory.micro;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Shared stillValid check for the Micro*Menu classes (anvil, brewing,
 * cartography, crafting, enchanting, grindstone, loom, smithing,
 * stonecutter), all of which host a vanilla menu inside a MicroBlock.
 */
public class MicroMenuUtils {
    public static boolean isValidMicroBlockAccess(Level level, BlockPos pos, Player player) {
        if (!level.getBlockState(pos).is(mc.sayda.creraces.registry.ModBlocks.MICRO_BLOCK.get())) {
            return false;
        }
        return player.distanceToSqr((double) pos.getX() + 0.5, (double) pos.getY() + 0.5,
                (double) pos.getZ() + 0.5) <= 64.0;
    }
}
