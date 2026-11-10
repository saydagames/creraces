package mc.sayda.creraces.engine.condition;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.AttackCharge;
import mc.sayda.creraces.engine.Comparison;
import mc.sayda.creraces.engine.DataValue;
import mc.sayda.creraces.engine.EntityFlag;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.engine.ItemSlots;
import mc.sayda.creraces.engine.Position;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.race.StandInEntityTypes;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import mc.sayda.creraces.util.ItemUtils;
import mc.sayda.creraces.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Conditions about one entity, chosen by "entity" ("self" or "target"). Left out, they look at the
 * target when there is one and at the caster otherwise.
 */
@SuppressWarnings("null")
final class EntityConditions {
    private EntityConditions() {
    }

    /** One check against the selected entity. */
    @FunctionalInterface
    interface EntityTest {
        boolean test(LivingEntity entity, Player caster, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
                @Nullable BlockPos interactPos);
    }

    /** Registers a condition that runs {@code test} on the entity its "entity" field selects. */
    static void register(String id, EntitySelector defaultEntity, Function<JsonObject, EntityTest> parser) {
        ConditionRegistry.register(ConditionRegistry.id(id), json -> {
            EntitySelector selector = EntitySelector.fromJson(json, defaultEntity);
            EntityTest test = parser.apply(json);
            return (caster, target, slot, pos) -> {
                LivingEntity entity = selector.resolve(caster, target);
                return entity != null && test.test(entity, caster, target, slot, pos);
            };
        });
    }

    /** A yes/no state of the entity, compared with "value" (default true). */
    private static void registerState(String id, Predicate<LivingEntity> state) {
        register(id, EntitySelector.AUTO, json -> {
            boolean expected = GsonHelper.getAsBoolean(json, "value", true);
            return (entity, caster, target, slot, pos) -> state.test(entity) == expected;
        });
    }

    static void register() {
        registerState("sneaking", LivingEntity::isCrouching);
        registerState("on_ground", LivingEntity::onGround);
        registerState("burning", LivingEntity::isOnFire);
        registerState("exposed_to_rain", WorldUtils::isExposedToRain);
        registerState("flying", entity -> entity instanceof Player p
                ? p.getAbilities().flying || p.isFallFlying() : entity.isFallFlying());
        registerState("in_sunlight", entity -> entity.level().isDay() && !entity.level().isRaining()
                && entity.level().canSeeSky(entity.blockPosition()));

        registerState("in_water", entity -> entity.isInWater()
                || entity.level().getBlockState(entity.blockPosition()).is(Blocks.BUBBLE_COLUMN));

        register("moving", EntitySelector.AUTO, json -> {
            boolean expected = GsonHelper.getAsBoolean(json, "value", true);
            ScalingValue threshold = ScalingValue.fromJson(json, "threshold", 0.1);
            return (entity, caster, target, slot, pos) -> {
                double t = threshold.evaluate(caster, target, slot, pos);
                return (entity.getDeltaMovement().lengthSqr() > t * t) == expected;
            };
        });

        // During on_hit this is the charge of the swing that landed the hit; see AttackCharge.
        register("attack_charged", EntitySelector.SELF, json -> {
            ScalingValue threshold = ScalingValue.fromJson(json, "threshold", 0.9);
            return (entity, caster, target, slot, pos) -> entity instanceof Player p
                    && AttackCharge.of(p) >= threshold.evaluate(caster, target, slot, pos);
        });

        register("has_effect", EntitySelector.AUTO, EntityConditions::hasEffect);
        register("has_item", EntitySelector.AUTO, EntityConditions::hasItem);
        register("entity_type", EntitySelector.AUTO, EntityConditions::entityType);
        register("entity_data", EntitySelector.AUTO, EntityConditions::entityData);

        // Players keep flags in their race data, other entities as tags; see EntityFlag.
        register("flag", EntitySelector.SELF, json -> {
            String name = GsonHelper.getAsString(json, "flag");
            EntityFlag flag = EntityFlag.byId(name);
            if (flag == null) {
                throw new IllegalArgumentException("unknown flag '" + name + "'; known: " + EntityFlag.knownIds());
            }
            boolean expected = GsonHelper.getAsBoolean(json, "value", true);
            return (entity, caster, target, slot, pos) -> flag.get(entity) == expected;
        });

        register("target_filter", EntitySelector.AUTO, json -> {
            TargetFilter filter = TargetFilter.fromJson(json, "targets", Set.of("all"));
            return (entity, caster, target, slot, pos) -> filter.isValid(entity, caster);
        });

        // Whether the entity is within "radius" of "position" (the caster by default).
        register("distance", EntitySelector.AUTO, json -> {
            Position position = Position.fromJson(json, "position", Position.of(Position.Source.SELF));
            ScalingValue radius = ScalingValue.fromJson(json, "radius", 5.0);
            return (entity, caster, target, slot, pos) -> {
                Vec3 point = position.resolve(caster, target, slot, pos);
                double r = radius.evaluate(caster, target, slot, pos);
                return point != null && entity.position().distanceToSqr(point) <= r * r;
            };
        });
    }

    /** The effect is present; its strength and time left are read with the effect() stat. */
    private static EntityTest hasEffect(JsonObject json) {
        ResourceLocation effectId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "effect"));
        Holder<MobEffect> effect = effectId != null ? BuiltInRegistries.MOB_EFFECT.getHolder(effectId).orElse(null) : null;
        if (effect == null) {
            throw new IllegalArgumentException("unknown effect '" + effectId + "'");
        }
        return (entity, caster, target, slot, pos) -> entity.hasEffect(effect);
    }

    /**
     * True when any stack in "slot" (default "hands") matches "item" (an id or #tag), or, with
     * "not_item", is a non-empty stack that does not match it. With neither, any non-empty stack counts.
     */
    private static EntityTest hasItem(JsonObject json) {
        String slot = GsonHelper.getAsString(json, "slot", "hands");
        String item = GsonHelper.getNullableString(json, "item", null);
        String notItem = GsonHelper.getNullableString(json, "not_item", null);
        String repairedWith = GsonHelper.getNullableString(json, "repaired_with", null);
        return (entity, caster, target, slotContext, pos) -> {
            for (ItemStack stack : ItemSlots.stacks(entity, slot)) {
                if (stack.isEmpty()) {
                    continue;
                }
                boolean matches = (item == null || ItemUtils.matches(stack, item))
                        && (notItem == null || !ItemUtils.matches(stack, notItem))
                        && (repairedWith == null || ItemUtils.repairableWith(stack, repairedWith));
                if (matches) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Matches by "entity_type" (an id or #tag), "category" / "not_category" (the mob category, e.g.
     * "monster") and "mob_type" (undead, aquatic, illager, arthropod). Every given key must match.
     */
    private static EntityTest entityType(JsonObject json) {
        String type = GsonHelper.getNullableString(json, "entity_type", null);
        TagKey<EntityType<?>> tag = null;
        ResourceLocation typeId = null;
        if (type != null && type.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(type.substring(1));
            if (tagId == null) {
                throw new IllegalArgumentException("malformed entity tag '" + type + "'");
            }
            tag = TagKey.create(Registries.ENTITY_TYPE, tagId);
        } else if (type != null) {
            typeId = ResourceLocation.tryParse(type);
            if (typeId == null) {
                throw new IllegalArgumentException("malformed entity type '" + type + "'");
            }
        }
        String category = GsonHelper.getNullableString(json, "category", null);
        String notCategory = GsonHelper.getNullableString(json, "not_category", null);
        String mobTypeName = GsonHelper.getNullableString(json, "mob_type", null);
        // 1.21 replaced MobType with these entity type tags.
        TagKey<EntityType<?>> mobType = mobTypeName == null ? null : switch (mobTypeName) {
            case "undead" -> EntityTypeTags.UNDEAD;
            case "aquatic" -> EntityTypeTags.AQUATIC;
            case "illager" -> EntityTypeTags.ILLAGER;
            case "arthropod" -> EntityTypeTags.ARTHROPOD;
            default -> throw new IllegalArgumentException("unknown mob_type '" + mobTypeName + "'");
        };

        TagKey<EntityType<?>> finalTag = tag;
        ResourceLocation finalTypeId = typeId;
        return (entity, caster, target, slot, pos) -> {
            if (finalTypeId != null && !EntityType.getKey(entity.getType()).equals(finalTypeId)) {
                return false;
            }
            if (finalTag != null && !StandInEntityTypes.is(entity, finalTag)) {
                return false;
            }
            String categoryName = entity.getType().getCategory().getName();
            if (category != null && !categoryName.equals(category)) {
                return false;
            }
            if (notCategory != null && categoryName.equals(notCategory)) {
                return false;
            }
            return mobType == null || StandInEntityTypes.is(entity, mobType);
        };
    }

    /** Compares a persistent-data key with "value"; without a value it checks the key exists ("!=" for absent). */
    private static EntityTest entityData(JsonObject json) {
        String key = GsonHelper.getAsString(json, "key");
        Comparison comparison = ConditionRegistry.operator(json);
        DataValue value = DataValue.fromJson(json, "value", DataValue.DataType.DOUBLE);
        if (value == null && comparison != Comparison.EQUAL && comparison != Comparison.NOT_EQUAL) {
            CreRaces.LOGGER.error("entity_data without a value only supports == (present) and != (absent): {}", json);
        }
        return (entity, caster, target, slot, pos) -> {
            var data = ((IPersistentDataAccessor) entity).creraces$getPersistentData();
            if (value == null) {
                return data.contains(key) == (comparison == Comparison.EQUAL);
            }
            Object current = DataValue.readTag(data, key, value.type());
            return comparison.test(current, value.resolve(caster, target, slot, pos));
        };
    }
}
