package mc.sayda.creraces.world.inventory.micro;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SmithingMenu;

public class MicroSmithingMenu extends SmithingMenu {
    public MicroSmithingMenu(int syncId, Inventory playerInventory, ContainerLevelAccess access) {
        super(syncId, playerInventory, access);
    }

    @Override
    public boolean stillValid(Player player) {
        return MicroMenuUtils.stillValid(access, player);
    }
}
