package mc.sayda.creraces.item;

import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.registry.ModItems;
import mc.sayda.creraces.util.ItemNbt;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class EssenceBucketItem extends Item {

    public static final String TAG_ESSENCE = "essence";

    public EssenceBucketItem(Properties properties) {
        super(properties);
    }

    /** Creates a filled essence bucket carrying the given type, optionally copying display data from a source stack. */
    public static ItemStack of(EssenceType type, @Nullable ItemStack copyDisplayFrom) {
        ItemStack stack = new ItemStack(ModItems.ESSENCE_BUCKET.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_ESSENCE, type.getSerializedName());
        ItemNbt.set(stack, tag);
        if (copyDisplayFrom != null) {
            transferDisplay(copyDisplayFrom, stack);
        }
        return stack;
    }

    /**
     * Transfers display data (custom name, lore) from one stack to another. Used when emptying
     * the bucket. Both are data components on 1.20.5+, not a "display" NBT subtag.
     */
    public static void transferDisplay(ItemStack from, ItemStack to) {
        var name = from.get(DataComponents.CUSTOM_NAME);
        if (name != null) {
            to.set(DataComponents.CUSTOM_NAME, name);
        }
        var lore = from.get(DataComponents.LORE);
        if (lore != null) {
            to.set(DataComponents.LORE, lore);
        }
    }

    @Nullable
    public static EssenceType getEssenceType(ItemStack stack) {
        CompoundTag tag = ItemNbt.get(stack);
        if (!tag.contains(TAG_ESSENCE)) return null;
        try {
            return EssenceType.byId(tag.getString(TAG_ESSENCE));
        } catch (IllegalArgumentException ignored) {
            // Unrecognised essence id; getName falls back to the plain bucket name.
            return null;
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        EssenceType type = getEssenceType(stack);
        if (type != null) {
            return Component.translatable("item.creraces.essence_bucket",
                    Component.translatable("essence.creraces." + type.getSerializedName()));
        }
        return super.getName(stack);
    }
}
