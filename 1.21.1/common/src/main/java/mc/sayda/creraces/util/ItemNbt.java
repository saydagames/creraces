package mc.sayda.creraces.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * The mod's own per-stack NBT, kept in the CUSTOM_DATA component now that 1.20.5 removed
 * ItemStack.getTag(). The component hands out copies, so writes go through mutate or set.
 */
public final class ItemNbt {

    private ItemNbt() {
    }

    /** Returns a copy of the stack's custom data, empty if it has none. Never null. */
    public static CompoundTag get(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    /** Read-modify-write: the consumer edits the tag, and the result is stored back. */
    public static void mutate(ItemStack stack, Consumer<CompoundTag> action) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, action);
    }

    /** Replaces the stack's custom data wholesale. */
    public static void set(ItemStack stack, CompoundTag tag) {
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }
}
