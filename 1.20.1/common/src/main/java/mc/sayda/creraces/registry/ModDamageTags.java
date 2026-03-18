package mc.sayda.creraces.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public class ModDamageTags {
    public static final TagKey<DamageType> IS_PHYSICAL = TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("creraces", "is_physical"));
    public static final TagKey<DamageType> IS_MAGIC = TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("creraces", "is_magic"));
    public static final TagKey<DamageType> IS_TRUE = TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("creraces", "is_true"));
}
