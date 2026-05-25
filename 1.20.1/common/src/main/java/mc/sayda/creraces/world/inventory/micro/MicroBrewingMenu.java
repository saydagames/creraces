package mc.sayda.creraces.world.inventory.micro;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;

public class MicroBrewingMenu extends BrewingStandMenu {
    private final ContainerLevelAccess access;

    public MicroBrewingMenu(int syncId, Inventory playerInventory,
            net.minecraft.world.Container container,
            net.minecraft.world.inventory.ContainerData data,
            ContainerLevelAccess access) {
        super(syncId, playerInventory, container, data);
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
