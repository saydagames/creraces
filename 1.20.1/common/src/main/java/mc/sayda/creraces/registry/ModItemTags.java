package mc.sayda.creraces.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModItemTags {
    /** Items that can never be enchanted, such as ones whose durability counts their remaining uses. */
    public static final TagKey<Item> UNENCHANTABLE = TagKey.create(Registries.ITEM, new ResourceLocation("creraces", "unenchantable"));
}
