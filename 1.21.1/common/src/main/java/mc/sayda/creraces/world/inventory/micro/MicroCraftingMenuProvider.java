package mc.sayda.creraces.world.inventory.micro;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.MenuProvider;

import javax.annotation.Nullable;

/**
 * Opens a vanilla crafting table GUI.
 * Stateless: the 3x3 grid is per-player, nothing is stored in the micro-block.
 */
public class MicroCraftingMenuProvider implements MenuProvider {

    private final Level level;
    private final BlockPos pos;

    public MicroCraftingMenuProvider(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.crafting");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
        return new MicroCraftingMenu(syncId, inventory, ContainerLevelAccess.create(level, pos));
    }
}
