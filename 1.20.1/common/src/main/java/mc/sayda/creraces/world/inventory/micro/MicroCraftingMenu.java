package mc.sayda.creraces.world.inventory.micro;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;

/**
 * Custom CraftingMenu that overrides stillValid to allow usage within a
 * MicroBlock.
 */
public class MicroCraftingMenu extends CraftingMenu {
    private final ContainerLevelAccess access;

    public MicroCraftingMenu(int syncId, Inventory playerInventory, ContainerLevelAccess access) {
        super(syncId, playerInventory, access);
        this.access = access;
    }

    @Override
    public boolean stillValid(Player player) {
        // We override this because the vanilla check looks for the CRAFTING_TABLE
        // block,
        // but our crafting table is just a mini-slot inside a MicroBlock host.
        return this.access.evaluate((level, pos) -> {
            // Check if there's a MicroBlock at this position
            if (!level.getBlockState(pos).is(mc.sayda.creraces.registry.ModBlocks.MICRO_BLOCK.get())) {
                return false;
            }
            // Basic proximity check (vanilla uses 64.0)
            return player.distanceToSqr((double) pos.getX() + 0.5, (double) pos.getY() + 0.5,
                    (double) pos.getZ() + 0.5) <= 64.0;
        }, true);
    }
}
