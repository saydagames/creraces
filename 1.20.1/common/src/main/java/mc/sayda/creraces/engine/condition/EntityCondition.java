package mc.sayda.creraces.engine.condition;

import com.google.gson.JsonObject;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class EntityCondition implements Condition {
    private final String entityType;
    private final String tag;
    private final String category;
    private final String notCategory;
    private final boolean useTarget;

    public EntityCondition(@javax.annotation.Nullable String entityType, @javax.annotation.Nullable String tag,
            @javax.annotation.Nullable String category, @javax.annotation.Nullable String notCategory,
            boolean useTarget) {
        this.entityType = entityType;
        this.tag = tag;
        this.category = category;
        this.notCategory = notCategory;
        this.useTarget = useTarget;
    }

    public @javax.annotation.Nullable String entityType() {
        return entityType;
    }

    public @javax.annotation.Nullable String tag() {
        return tag;
    }

    public @javax.annotation.Nullable String category() {
        return category;
    }

    public @javax.annotation.Nullable String notCategory() {
        return notCategory;
    }

    public boolean useTarget() {
        return useTarget;
    }

    @Override
    public boolean evaluate(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        // Smart Targeting: Prefer target if present, otherwise respect useTarget flag
        LivingEntity entity = (target != null) ? target : (useTarget ? null : player);
        if (entity == null)
            return false;

        if (entityType != null) {
            ResourceLocation typeId = net.minecraft.world.entity.EntityType.getKey(entity.getType());
            if (!typeId.toString().equals(entityType))
                return false;
        }

        if (tag != null) {
            @SuppressWarnings("null")
            net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> tagKey = net.minecraft.tags.TagKey.create(
                    net.minecraft.core.registries.Registries.ENTITY_TYPE,
                    new ResourceLocation(tag.startsWith("#") ? tag.substring(1) : tag));
            if (!entity.getType().is(tagKey))
                return false;
        }

        if (category != null) {
            String catName = entity.getType().getCategory().getName();
            if (!catName.equalsIgnoreCase(category))
                return false;
        }

        if (notCategory != null) {
            String catName = entity.getType().getCategory().getName();
            if (catName.equalsIgnoreCase(notCategory))
                return false;
        }

        return true;
    }

    public static Condition fromJson(JsonObject json) {
        @javax.annotation.Nullable String type = GsonHelper.getNullableString(json, "entity_type", null);
        @javax.annotation.Nullable String tag = GsonHelper.getNullableString(json, "tag", null);
        @javax.annotation.Nullable String category = GsonHelper.getNullableString(json, "category", null);
        @javax.annotation.Nullable String notCategory = GsonHelper.getNullableString(json, "not_category", null);
        boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
        return new EntityCondition(type, tag, category, notCategory, useTarget);
    }
}
