package mc.sayda.creraces.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Utility for matching items against IDs or tags.
 */
public class ItemUtils {
    /**
     * Matches a stack against an item id ("minecraft:feather") or a #-prefixed item tag
     * ("#minecraft:feathers"). A bare path with no namespace is read as a vanilla item id.
     */
    public static boolean matches(ItemStack stack, String definition) {
        if (stack.isEmpty() || definition == null || definition.isEmpty()) {
            return false;
        }

        if (definition.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(definition.substring(1));
            if (tagId == null) {
                return false;
            }
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
            return stack.is(tagKey);
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id.toString().equals(definition)) {
            return true;
        }
        return !definition.contains(":") && id.getNamespace().equals("minecraft") && id.getPath().equals(definition);
    }
}
