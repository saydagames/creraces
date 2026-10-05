package mc.sayda.creraces.engine.condition;

import com.google.gson.JsonObject;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/** Matches the target (or the player when there is none) by entity type, type tag, mob category or mob type. */
public class EntityCondition implements Condition {
    private final @Nullable String entityType;
    private final @Nullable String tag;
    private final @Nullable String category;
    private final @Nullable String notCategory;
    private final @Nullable String mobType;
    private final boolean useTarget;

    public EntityCondition(@Nullable String entityType, @Nullable String tag, @Nullable String category,
            @Nullable String notCategory, @Nullable String mobType, boolean useTarget) {
        this.entityType = entityType;
        this.tag = tag;
        this.category = category;
        this.notCategory = notCategory;
        this.mobType = mobType;
        this.useTarget = useTarget;
    }

    @Override
    public boolean evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity entity = TargetFilter.resolveSmartTarget(player, target, useTarget);
        if (entity == null)
            return false;

        if (entityType != null && !EntityType.getKey(entity.getType()).toString().equals(entityType))
            return false;

        if (tag != null) {
            @SuppressWarnings("null")
            TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE,
                    ResourceLocation.parse(tag.startsWith("#") ? tag.substring(1) : tag));
            if (!entity.getType().is(tagKey))
                return false;
        }

        String categoryName = entity.getType().getCategory().getName();
        if (category != null && !categoryName.equalsIgnoreCase(category))
            return false;
        if (notCategory != null && categoryName.equalsIgnoreCase(notCategory))
            return false;

        if (mobType != null) {
            // 1.21 replaced MobType with these entity type tags.
            TagKey<EntityType<?>> mobTypeTag = switch (mobType.toLowerCase()) {
                case "undead" -> EntityTypeTags.UNDEAD;
                case "aquatic" -> EntityTypeTags.AQUATIC;
                case "illager" -> EntityTypeTags.ILLAGER;
                case "arthropod" -> EntityTypeTags.ARTHROPOD;
                default -> null;
            };
            if (mobTypeTag == null || !entity.getType().is(mobTypeTag))
                return false;
        }

        return true;
    }

    public static Condition fromJson(JsonObject json) {
        String type = GsonHelper.getNullableString(json, "entity_type", null);
        String tag = GsonHelper.getNullableString(json, "tag", null);
        String category = GsonHelper.getNullableString(json, "category", null);
        String notCategory = GsonHelper.getNullableString(json, "not_category", null);
        String mobType = GsonHelper.getNullableString(json, "mob_type", null);
        boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
        return new EntityCondition(type, tag, category, notCategory, mobType, useTarget);
    }
}
