package mc.sayda.creraces.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * Item NBT access for the mod's own per-stack data.
 *
 * ItemStack.getTag()/getOrCreateTag()/setTag() were removed in 1.20.5 when item NBT became
 * data components; arbitrary mod data now lives in the CUSTOM_DATA component, which hands out
 * copies rather than a live tag. These wrappers keep the read / read-modify-write / overwrite
 * shapes the call sites already use.
 */
public final class ItemNbt {

    private ItemNbt() {
    }

    /** Returns a copy of the stack's custom data, empty if it has none. Never null. */
    public static CompoundTag get(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    /** True if the stack carries any custom data at all. */
    public static boolean has(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && !data.isEmpty();
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
