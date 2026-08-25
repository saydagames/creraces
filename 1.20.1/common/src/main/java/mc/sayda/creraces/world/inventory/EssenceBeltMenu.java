package mc.sayda.creraces.world.inventory;

import mc.sayda.creraces.item.EssenceBeltItem;
import mc.sayda.creraces.registry.ModMenuTypes;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class EssenceBeltMenu extends AbstractContainerMenu {

    private final SimpleContainer beltInventory;

    public EssenceBeltMenu(int syncId, Inventory playerInv, SimpleContainer beltInv) {
        super(ModMenuTypes.ESSENCE_BELT.get(), syncId);
        this.beltInventory = beltInv;

        // 8 belt slots: 4 left, 16px gap, 4 right
        for (int i = 0; i < 4; i++) {
            addSlot(new BeltSlot(beltInv, i, 8 + i * 18, 19));
        }
        for (int i = 0; i < 4; i++) {
            addSlot(new BeltSlot(beltInv, 4 + i, 98 + i * 18, 19));
        }

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 51 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 109));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return findBeltStack(player) != null;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        ItemStack belt = findBeltStack(player);
        if (belt != null) {
            EssenceBeltItem.saveInventory(belt, beltInventory);
            player.getInventory().setChanged();
        }
    }

    /** Finds the belt stack from the curio/trinket slot, or the player's hands as a fallback. */
    public static ItemStack findBeltStack(Player player) {
        java.util.Optional<net.minecraft.world.item.ItemStack> curio = mc.sayda.creraces.util.PlatformServices.findBelt(player);
        if (curio.isPresent()) return curio.get();
        if (player.getMainHandItem().getItem() instanceof EssenceBeltItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof EssenceBeltItem) return player.getOffhandItem();
        return null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < EssenceBeltItem.SLOTS) {
            if (!moveItemStackTo(stack, EssenceBeltItem.SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(stack, 0, EssenceBeltItem.SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return copy;
    }

    private static class BeltSlot extends Slot {
        BeltSlot(SimpleContainer container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof mc.sayda.creraces.item.EssenceBottleItem;
        }
    }
}
