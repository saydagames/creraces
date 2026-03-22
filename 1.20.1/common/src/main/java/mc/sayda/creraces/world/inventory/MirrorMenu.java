package mc.sayda.creraces.world.inventory;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.FriendlyByteBuf;
import mc.sayda.creraces.registry.ModMenuTypes;
import net.minecraft.world.item.ItemStack;

public class MirrorMenu extends AbstractContainerMenu {
    public MirrorMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(ModMenuTypes.MIRROR_GUI.get(), id);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
