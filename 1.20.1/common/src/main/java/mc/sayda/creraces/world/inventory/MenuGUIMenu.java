package mc.sayda.creraces.world.inventory;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.network.FriendlyByteBuf;
import mc.sayda.creraces.registry.ModMenuTypes;
import net.minecraft.world.item.ItemStack;

public class MenuGUIMenu extends AbstractContainerMenu {
    public final Level world;
    public final Player entity;
    public final int x, y, z;

    public MenuGUIMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(ModMenuTypes.MENU_GUI.get(), id);
        this.entity = inv.player;
        this.world = inv.player.level();
        if (extraData != null) {
            var pos = extraData.readBlockPos();
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
        } else {
            this.x = this.y = this.z = 0;
        }
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
