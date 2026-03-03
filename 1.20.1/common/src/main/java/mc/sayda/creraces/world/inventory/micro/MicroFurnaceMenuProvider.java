package mc.sayda.creraces.world.inventory.micro;

import mc.sayda.creraces.block.entity.MicroBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.MenuProvider;

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
        // Use the direct wrapper from the MicroBlockEntity for live syncing
        net.minecraft.world.Container container = micro.getInventory(slotIdx, 3);

        // ContainerData wrapping our furnace state array
        int[] state = micro.getOrCreateFurnaceState(slotIdx);
        ContainerData containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return state[index];
            }

            @Override
            public void set(int index, int value) {
                state[index] = value;
            }

            @Override
            public int getCount() {
                return 4;
            }
        };

        if (slotState.getBlock() instanceof BlastFurnaceBlock) {
            return new BlastFurnaceMenu(syncId, playerInventory, container, containerData);
        } else if (slotState.getBlock() instanceof SmokerBlock) {
            return new SmokerMenu(syncId, playerInventory, container, containerData);
        } else {
            return new FurnaceMenu(syncId, playerInventory, container, containerData);
        }
    }
}
