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
        return this.access.evaluate((level, pos) -> MicroMenuUtils.isValidMicroBlockAccess(level, pos, player), true);
    }
}
