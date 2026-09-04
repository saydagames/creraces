package mc.sayda.creraces.world.inventory.micro;

import mc.sayda.creraces.block.entity.MicroBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;

import javax.annotation.Nullable;

/**
 * Opens a 27-slot barrel/chest inventory backed by the MicroBlockEntity's
 * sparse inventory map.
 */
public class MicroBarrelMenuProvider implements MenuProvider {

    private final MicroBlockEntity micro;
    private final int slotIdx;

    public MicroBarrelMenuProvider(MicroBlockEntity micro, int slotIdx) {
        this.micro = micro;
        this.slotIdx = slotIdx;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.barrel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        // Use the direct wrapper from the MicroBlockEntity for live syncing
        net.minecraft.world.Container container = micro.getInventory(slotIdx, 27);
        return ChestMenu.threeRows(syncId, playerInventory, container);
    }
}
