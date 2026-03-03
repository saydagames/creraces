package mc.sayda.creraces.engine.condition;

import com.google.gson.JsonObject;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public record EntityCondition(
        @javax.annotation.Nullable String entityType,
        @javax.annotation.Nullable String tag,
        boolean useTarget) implements Condition {

    @Override
    public boolean evaluate(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        // Smart Targeting: Prefer target if present, otherwise respect useTarget flag
        LivingEntity entity = (target != null) ? target : (useTarget ? target : player);
        if (entity == null)
            return false;

        if (entityType != null) {
            ResourceLocation typeId = EntityType.getKey(entity.getType());
            if (typeId.toString().equals(entityType))
                return true;
        }

        if (tag != null) {
            @SuppressWarnings("null")
            net.minecraft.tags.TagKey<EntityType<?>> tagKey = net.minecraft.tags.TagKey.create(
                    net.minecraft.core.registries.Registries.ENTITY_TYPE,
                    new ResourceLocation(tag.startsWith("#") ? tag.substring(1) : tag));
            if (entity.getType().is(tagKey))
                return true;
        }

        return false;
    }

    public static Condition fromJson(JsonObject json) {
        String type = GsonHelper.getAsString(json, "entity_type", null);
        String tag = GsonHelper.getAsString(json, "tag", null);
        boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
        return new EntityCondition(type, tag, useTarget);
    }
}
