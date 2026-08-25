package mc.sayda.creraces.item;

import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.registry.ModItems;
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

    /** Creates a filled essence bucket carrying the given type, optionally copying display NBT from a source stack. */
    public static ItemStack of(EssenceType type, @Nullable ItemStack copyDisplayFrom) {
        ItemStack stack = new ItemStack(ModItems.ESSENCE_BUCKET.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_ESSENCE, type.getSerializedName());
        if (copyDisplayFrom != null) {
            CompoundTag srcTag = copyDisplayFrom.getTag();
            if (srcTag != null && srcTag.contains("display")) {
                tag.put("display", srcTag.getCompound("display").copy());
            }
        }
        stack.setTag(tag);
        return stack;
    }

    /** Transfers display NBT (custom name, lore) from one stack to another. Used when emptying the bucket. */
    public static void transferDisplay(ItemStack from, ItemStack to) {
        CompoundTag tag = from.getTag();
        if (tag != null && tag.contains("display")) {
            to.getOrCreateTag().put("display", tag.getCompound("display").copy());
        }
    }

    @Nullable
    public static EssenceType getEssenceType(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ESSENCE)) return null;
        try {
            return EssenceType.byId(tag.getString(TAG_ESSENCE));
        } catch (IllegalArgumentException ignored) {
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
