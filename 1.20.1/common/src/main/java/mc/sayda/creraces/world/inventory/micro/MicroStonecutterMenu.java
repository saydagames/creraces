package mc.sayda.creraces.world.inventory.micro;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.StonecutterMenu;

public class MicroStonecutterMenu extends StonecutterMenu {
    private final ContainerLevelAccess access;

    public MicroStonecutterMenu(int syncId, Inventory playerInventory, ContainerLevelAccess access) {
        super(syncId, playerInventory, access);
        this.access = access;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) -> {
            if (!level.getBlockState(pos).is(mc.sayda.creraces.registry.ModBlocks.MICRO_BLOCK.get())) {
                return false;
            }
            return player.distanceToSqr((double) pos.getX() + 0.5, (double) pos.getY() + 0.5,
                    (double) pos.getZ() + 0.5) <= 64.0;
        }, true);
    }
}
