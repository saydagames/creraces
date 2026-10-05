package mc.sayda.creraces.world.inventory.micro;

import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;

/**
 * Shared helpers for the vanilla menus hosted inside a MicroBlock. Vanilla's own stillValid checks
 * look for the real workstation block, so every Micro*Menu swaps in {@link #stillValid} instead.
 */
public final class MicroMenuUtils {

    private MicroMenuUtils() {
    }

    public static boolean stillValid(ContainerLevelAccess access, Player player) {
        return access.evaluate((level, pos) -> isValidMicroBlockAccess(level, pos, player), true);
    }

    public static boolean isValidMicroBlockAccess(Level level, BlockPos pos, Player player) {
        if (!level.getBlockState(pos).is(ModBlocks.MICRO_BLOCK.get())) {
            return false;
        }
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    /** Exposes a live state array (furnace or brewing timers) to a vanilla menu. */
    public static ContainerData dataView(int[] values) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return values[index];
            }

            @Override
            public void set(int index, int value) {
                values[index] = value;
            }

            @Override
            public int getCount() {
                return values.length;
            }
        };
    }
}
