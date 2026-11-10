package mc.sayda.creraces.engine;

import com.google.gson.JsonObject;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.engine.condition.ConditionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The entity search behind aoe and has_entities, read from the same keys:
 * <pre>{ "position": {...}, "radius": 5, "shape": "sphere", "height": 2, "targets": [...],
 *   "condition": {...}, "max_targets": 3, "sort": "nearest" | "random" }</pre>
 * The shapes are sphere and cube, and their flat versions circle and square, which only measure
 * across the ground and reach "height" blocks tall, centered on the position. Round shapes measure
 * to the closest point of each entity's hitbox; square ones are axis-aligned boxes. "condition" is
 * checked with each candidate as the target.
 */
@SuppressWarnings("null")
public final class AreaSearch {
    public enum Shape {
        SPHERE(true, false), CUBE(false, false), CIRCLE(true, true), SQUARE(false, true);

        final boolean round;
        final boolean flat;

        Shape(boolean round, boolean flat) {
            this.round = round;
            this.flat = flat;
        }
    }

    public enum Sort { NEAREST, RANDOM }

    private final Position position;
    private final ScalingValue radius;
    private final Shape shape;
    private final ScalingValue height;
    private final TargetFilter targets;
    @Nullable
    private final Condition condition;
    @Nullable
    private final ScalingValue maxTargets;
    private final Sort sort;

    private AreaSearch(Position position, ScalingValue radius, Shape shape, ScalingValue height, TargetFilter targets,
            @Nullable Condition condition, @Nullable ScalingValue maxTargets, Sort sort) {
        this.position = position;
        this.radius = radius;
        this.shape = shape;
        this.height = height;
        this.targets = targets;
        this.condition = condition;
        this.maxTargets = maxTargets;
        this.sort = sort;
    }

    public static AreaSearch fromJson(JsonObject json, double defaultRadius, Set<String> defaultTargets) {
        Shape shape = parse(Shape.class, GsonHelper.getAsString(json, "shape", "sphere"), "sphere, cube, circle, square");
        Sort sort = parse(Sort.class, GsonHelper.getAsString(json, "sort", "nearest"), "nearest, random");
        return new AreaSearch(
                Position.fromJson(json, "position", Position.of(Position.Source.SELF)),
                ScalingValue.fromJson(json, "radius", defaultRadius),
                shape,
                ScalingValue.fromJson(json, "height", 2.0),
                TargetFilter.fromJson(json, "targets", defaultTargets),
                ConditionRegistry.optional(json, "condition"),
                json.has("max_targets") ? ScalingValue.fromJson(json, "max_targets", 0.0) : null,
                sort);
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String name, String known) {
        try {
            return Enum.valueOf(type, name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown " + type.getSimpleName().toLowerCase() + " '" + name
                    + "' (" + known + ")");
        }
    }

    /** The matching entities, nearest first unless sorted randomly; null when the position can't be resolved. */
    @Nullable
    public List<LivingEntity> find(Player caster, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        Vec3 center = position.resolve(caster, target, slot, interactPos);
        if (center == null) {
            return null;
        }
        double r = clampRadius(radius.evaluate(caster, target, slot, interactPos));
        double halfHeight = shape.flat ? clampRadius(height.evaluate(caster, target, slot, interactPos) / 2) : r;
        // Square shapes are exactly this box; round ones also need their distance checked.
        AABB box = new AABB(center, center).inflate(r, halfHeight, r);
        List<LivingEntity> found = new ArrayList<>(caster.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive()
                        && (!shape.round || distanceToHitbox(center, entity, shape.flat) <= r)
                        && targets.isValid(entity, caster)
                        && (condition == null || condition.evaluate(caster, entity, slot, interactPos))));

        if (sort == Sort.RANDOM) {
            Collections.shuffle(found, new Random(caster.getRandom().nextLong()));
        } else {
            found.sort(Comparator.comparingDouble(entity -> distanceToHitbox(center, entity, false)));
        }
        if (maxTargets != null) {
            int max = (int) maxTargets.evaluate(caster, target, slot, interactPos);
            if (max >= 0 && found.size() > max) {
                found = new ArrayList<>(found.subList(0, max));
            }
        }
        return found;
    }

    /** Distance from the point to the closest point of the hitbox; across the ground only when flat. */
    private static double distanceToHitbox(Vec3 point, LivingEntity entity, boolean flat) {
        AABB box = entity.getBoundingBox();
        double dx = Math.max(Math.max(box.minX - point.x, 0), point.x - box.maxX);
        double dy = flat ? 0 : Math.max(Math.max(box.minY - point.y, 0), point.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - point.z, 0), point.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Caps a radius at the configured AoE limit; a limit of 0 or less (the default, -1) disables the cap. */
    public static double clampRadius(double radius) {
        int maxRadius = CreRacesConfig.AOE_MAX_RADIUS.get();
        return Math.max(0, maxRadius > 0 ? Math.min(radius, maxRadius) : radius);
    }
}
