package mc.sayda.creraces.world.inventory;

import mc.sayda.creraces.item.InkAndQuillItem;
import mc.sayda.creraces.item.ScrollItem;
import mc.sayda.creraces.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ResearchTableMenu extends AbstractContainerMenu {

    private final Container tableInventory;
    private final BlockPos tablePos;
    private final int[] storageEssenceCounts = new int[mc.sayda.creraces.ability.EssenceType.values().length];

    public ResearchTableMenu(int syncId, Inventory playerInv, Container tableInventory, BlockPos pos) {
        super(ModMenuTypes.RESEARCH_TABLE.get(), syncId);
        this.tableInventory = tableInventory;
        this.tablePos = pos;
        tableInventory.startOpen(playerInv.player);

        // Slot 0: Ink & Quill
        this.addSlot(new Slot(tableInventory, 0, 136, 10) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof InkAndQuillItem;
            }
        });

        // Slot 1: Ability Scroll
        this.addSlot(new Slot(tableInventory, 1, 155, 10) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ScrollItem;
            }
        });

        // Player inventory: 3 rows
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 105 + col * 18, 219 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 105 + col * 18, 276));
        }
    }

    public ResearchTableMenu(int syncId, Inventory playerInv, net.minecraft.network.FriendlyByteBuf buf) {
        this(syncId, playerInv, new SimpleContainer(2), buf.readBlockPos());
        mc.sayda.creraces.ability.EssenceType[] types = mc.sayda.creraces.ability.EssenceType.values();
        for (int i = 0; i < types.length; i++) {
            storageEssenceCounts[i] = buf.readVarInt();
        }
    }

    public int getStorageCount(mc.sayda.creraces.ability.EssenceType type) {
        return storageEssenceCounts[type.ordinal()];
    }

    public BlockPos getTablePos() {
        return tablePos;
    }

    public boolean hasScroll() {
        return !tableInventory.getItem(1).isEmpty();
    }

    public boolean isScrollCrafted() {
        net.minecraft.nbt.CompoundTag tag = mc.sayda.creraces.util.ItemNbt.get(tableInventory.getItem(1));
        return tag != null && tag.contains("Ability");
    }

    @Override
    public boolean stillValid(Player player) {
        return tableInventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < 2) {
            // Table slot -> move to inventory/hotbar
            if (!this.moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
        } else {
            // Inventory/hotbar -> try quill slot first, then scroll slot
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
                if (!this.moveItemStackTo(stack, 1, 2, false)) {
                    if (index < 29) {
                        // Inv row -> hotbar
                        if (!this.moveItemStackTo(stack, 29, 38, false)) return ItemStack.EMPTY;
                    } else {
                        // Hotbar -> inv rows
                        if (!this.moveItemStackTo(stack, 2, 29, false)) return ItemStack.EMPTY;
                    }
                }
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        tableInventory.stopOpen(player);
    }
}
