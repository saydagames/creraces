package mc.sayda.creraces.world.inventory.micro;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;

public class MicroBrewingMenu extends BrewingStandMenu {
    private final ContainerLevelAccess access;

    public MicroBrewingMenu(int syncId, Inventory playerInventory, Container container, ContainerData data,
            ContainerLevelAccess access) {
        super(syncId, playerInventory, container, data);
        this.access = access;
    }

    @Override
    public boolean stillValid(Player player) {
        return MicroMenuUtils.stillValid(access, player);
    }
}
