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

    /** Creates a filled essence bucket carrying the given type, optionally copying display data from a source stack. */
    public static ItemStack of(EssenceType type, @Nullable ItemStack copyDisplayFrom) {
        ItemStack stack = new ItemStack(ModItems.ESSENCE_BUCKET.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_ESSENCE, type.getSerializedName());
        mc.sayda.creraces.util.ItemNbt.set(stack, tag);
        if (copyDisplayFrom != null) {
            transferDisplay(copyDisplayFrom, stack);
        }
        return stack;
    }

    /**
     * Transfers display data (custom name, lore) from one stack to another. Used when emptying
     * the bucket. These lived in a "display" NBT subtag before 1.20.5 and are separate data
     * components now.
     */
    public static void transferDisplay(ItemStack from, ItemStack to) {
        var name = from.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (name != null) {
            to.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, name);
        }
        var lore = from.get(net.minecraft.core.component.DataComponents.LORE);
        if (lore != null) {
            to.set(net.minecraft.core.component.DataComponents.LORE, lore);
        }
    }

    @Nullable
    public static EssenceType getEssenceType(ItemStack stack) {
        CompoundTag tag = mc.sayda.creraces.util.ItemNbt.get(stack);
        if (!tag.contains(TAG_ESSENCE)) return null;
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
