package mc.sayda.creraces.engine.condition;

import mc.sayda.creraces.engine.EventContext;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.ItemUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ItemStack;

/**
 * Conditions about the event that triggered the running trait (see EventContext): the damage type of
 * an on_hurt, on_death or damage_multiplier, the item of an on_item_pickup or item_interaction, and
 * the ability of an on_ability_use. Outside such an event they are false.
 */
@SuppressWarnings("null")
final class EventConditions {
    private EventConditions() {
    }

    static void register() {
        // "damage_type" is a damage type id or #tag.
        ConditionRegistry.register(ConditionRegistry.id("damage_type"), json -> {
            String type = GsonHelper.getAsString(json, "damage_type");
            if (type.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(type.substring(1));
                if (tagId == null) {
                    throw new IllegalArgumentException("malformed damage type tag '" + type + "'");
                }
                TagKey<DamageType> tag = TagKey.create(Registries.DAMAGE_TYPE, tagId);
                return (player, target, slot, pos) -> {
                    DamageSource source = EventContext.current().damageSource();
                    return source != null && source.is(tag);
                };
            }
            ResourceLocation typeId = ResourceLocation.tryParse(type.contains(":") ? type : "minecraft:" + type);
            if (typeId == null) {
                throw new IllegalArgumentException("malformed damage type '" + type + "'");
            }
            return (player, target, slot, pos) -> {
                DamageSource source = EventContext.current().damageSource();
                return source != null && source.typeHolder().unwrapKey()
                        .map(key -> key.location().equals(typeId)).orElse(false);
            };
        });

        // "item" (an id or #tag) and/or "not_item" against the event's item.
        ConditionRegistry.register(ConditionRegistry.id("event_item"), json -> {
            String item = GsonHelper.getNullableString(json, "item", null);
            String notItem = GsonHelper.getNullableString(json, "not_item", null);
            return (player, target, slot, pos) -> {
                ItemStack stack = EventContext.current().item();
                return !stack.isEmpty() && (item == null || ItemUtils.matches(stack, item))
                        && (notItem == null || !ItemUtils.matches(stack, notItem));
            };
        });

        ConditionRegistry.register(ConditionRegistry.id("event_ability"), json -> {
            ResourceLocation ability = ResourceLocation.tryParse(GsonHelper.getAsString(json, "ability"));
            if (ability == null) {
                throw new IllegalArgumentException("malformed ability id");
            }
            return (player, target, slot, pos) -> ability.equals(EventContext.current().ability());
        });
    }
}
