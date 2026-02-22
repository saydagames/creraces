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
     * Matches an ItemStack against a definition string.
     * 
     * @param stack      The item stack to check.
     * @param definition The definition string (e.g., "minecraft:feather" or
     *                   "#minecraft:feathers").
     * @return True if the item matches.
     */
    public static boolean matches(ItemStack stack, String definition) {
        if (stack.isEmpty() || definition == null || definition.isEmpty()) {
            return false;
        }

        if (definition.startsWith("#")) {
            // Match against tag
            String tagId = definition.substring(1);
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, new ResourceLocation(tagId));
            return stack.is(tagKey);
        } else {
            // Match against item ID
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id.toString().equals(definition)) {
                return true;
            }

            // Fallback for short IDs if necessary (though engine usually uses full ones)
            if (!definition.contains(":")) {
                return id.getNamespace().equals("minecraft") && id.getPath().equals(definition);
            }
        }

        return false;
    }

    /**
     * Matches an ItemStack against a ResourceLocation (ID only).
     */
    public static boolean matches(ItemStack stack, ResourceLocation id) {
        if (stack.isEmpty() || id == null) {
            return false;
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id);
    }
}
