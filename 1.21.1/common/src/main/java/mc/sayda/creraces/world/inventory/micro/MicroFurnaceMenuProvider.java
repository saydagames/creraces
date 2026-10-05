package mc.sayda.creraces.world.inventory.micro;

import mc.sayda.creraces.block.entity.MicroBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Opens a furnace/blast furnace/smoker GUI backed by the MicroBlockEntity's
 * sparse inventory and timer state.
 * Slot layout: [0] input, [1] fuel, [2] output
 */
public class MicroFurnaceMenuProvider implements MenuProvider {

    private final MicroBlockEntity micro;
    private final int slotIdx;
    private final BlockState slotState;

    public MicroFurnaceMenuProvider(MicroBlockEntity micro, int slotIdx, BlockState slotState) {
        this.micro = micro;
        this.slotIdx = slotIdx;
        this.slotState = slotState;
    }

    @Override
    public Component getDisplayName() {
        if (slotState.getBlock() instanceof BlastFurnaceBlock)
            return Component.translatable("container.blast_furnace");
        if (slotState.getBlock() instanceof SmokerBlock)
            return Component.translatable("container.smoker");
        return Component.translatable("container.furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        // Both views are live, so the menu and the block entity's tick see the same items and timers
        Container container = micro.getInventory(slotIdx, 3);
        ContainerData containerData = MicroMenuUtils.dataView(micro.getOrCreateFurnaceState(slotIdx));

        if (slotState.getBlock() instanceof BlastFurnaceBlock) {
            return new BlastFurnaceMenu(syncId, playerInventory, container, containerData);
        } else if (slotState.getBlock() instanceof SmokerBlock) {
            return new SmokerMenu(syncId, playerInventory, container, containerData);
        } else {
            return new FurnaceMenu(syncId, playerInventory, container, containerData);
        }
    }
}
