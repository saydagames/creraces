package mc.sayda.creraces.world.inventory.micro;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;

public class MicroAnvilMenu extends AnvilMenu {
    private final ContainerLevelAccess access;

    public MicroAnvilMenu(int syncId, Inventory playerInventory, ContainerLevelAccess access) {
        super(syncId, playerInventory, access);
        this.access = access;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) -> MicroMenuUtils.isValidMicroBlockAccess(level, pos, player), true);
    }
}
