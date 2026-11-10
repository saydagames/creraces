package mc.sayda.creraces.engine;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A place in the world, read from a "position" object:
 * <pre>{ "from": "self" | "target" | "block" | "look" | "origin", "x": 0, "y": 0, "z": 0,
 *   "forward": 0, "range": 10, "adjacent": false, "math": "floor" }</pre>
 * Entities resolve to their feet, blocks to their corner coordinates (add 0.5 for the centre),
 * "block" is the interacted block, "look" the block under the crosshair within "range" ("adjacent"
 * takes the air block on the face that was hit) and "origin" is 0, 0, 0 for absolute coordinates.
 * The offsets are scaling values; "forward" moves along the caster's horizontal facing, and "math"
 * rounds the result when it is used as a block position. "Self" and "look" measure from the clone
 * while one repeats a cast (see CastSource).
 */
@SuppressWarnings("null")
public final class Position {
    /** Where a position is measured from. AUTO is the default of some actions: the target, else the caster. */
    public enum Source {
        AUTO, SELF, TARGET, BLOCK, LOOK, ORIGIN
    }

    private static final double DEFAULT_LOOK_RANGE = 10.0;

    private final Source source;
    private final ScalingValue x;
    private final ScalingValue y;
    private final ScalingValue z;
    private final ScalingValue forward;
    private final ScalingValue range;
    private final boolean adjacent;
    private final ScalingValue.MathOp math;

    private Position(Source source, ScalingValue x, ScalingValue y, ScalingValue z, ScalingValue forward,
            ScalingValue range, boolean adjacent, ScalingValue.MathOp math) {
        this.source = source;
        this.x = x;
        this.y = y;
        this.z = z;
        this.forward = forward;
        this.range = range;
        this.adjacent = adjacent;
        this.math = math;
    }

    public static Position of(Source source) {
        return of(source, 0);
    }

    /** {@code source} lifted by {@code yOffset}, e.g. particles a block above the feet. */
    public static Position of(Source source, double yOffset) {
        return new Position(source, ScalingValue.fixed(0), ScalingValue.fixed(yOffset), ScalingValue.fixed(0),
                ScalingValue.fixed(0), ScalingValue.fixed(DEFAULT_LOOK_RANGE), false, ScalingValue.MathOp.FLOOR);
    }

    public static Position fromJson(JsonObject json, String key, Position fallback) {
        if (!json.has(key) || !json.get(key).isJsonObject()) {
            return fallback;
        }
        JsonObject obj = json.getAsJsonObject(key);
        String from = GsonHelper.getAsString(obj, "from", "self");
        Source source = parseSource(from);
        if (source == null) {
            CreRaces.LOGGER.error("Unknown position source '{}' in {}; using \"self\"", from, obj);
            source = Source.SELF;
        }
        return new Position(source,
                ScalingValue.fromJson(obj, "x", 0.0),
                ScalingValue.fromJson(obj, "y", 0.0),
                ScalingValue.fromJson(obj, "z", 0.0),
                ScalingValue.fromJson(obj, "forward", 0.0),
                ScalingValue.fromJson(obj, "range", DEFAULT_LOOK_RANGE),
                GsonHelper.getAsBoolean(obj, "adjacent", false),
                ScalingValue.parseMath(obj, ScalingValue.MathOp.FLOOR));
    }

    @Nullable
    public static Source parseSource(String name) {
        return switch (name) {
            case "self" -> Source.SELF;
            case "target" -> Source.TARGET;
            case "block" -> Source.BLOCK;
            case "look" -> Source.LOOK;
            case "origin" -> Source.ORIGIN;
            default -> null;
        };
    }

    /** The exact point, or null when the source is missing (no target, no block, nothing in sight). */
    @Nullable
    public Vec3 resolve(Player caster, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        Vec3 base = base(caster, target, interactPos);
        if (base == null) {
            return null;
        }
        Vec3 point = base.add(x.evaluate(caster, target, slot, interactPos), y.evaluate(caster, target, slot, interactPos),
                z.evaluate(caster, target, slot, interactPos));
        double ahead = forward.evaluate(caster, target, slot, interactPos);
        if (ahead != 0) {
            double yaw = Math.toRadians(CastSource.self(caster).getYRot());
            point = point.add(-Math.sin(yaw) * ahead, 0, Math.cos(yaw) * ahead);
        }
        return point;
    }

    @Nullable
    public BlockPos resolveBlock(Player caster, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        Vec3 point = resolve(caster, target, slot, interactPos);
        return point != null ? toBlock(point, math) : null;
    }

    @Nullable
    private Vec3 base(Player caster, @Nullable LivingEntity target, @Nullable BlockPos interactPos) {
        LivingEntity self = CastSource.self(caster);
        return switch (source) {
            case AUTO -> (target != null ? target : self).position();
            case SELF -> self.position();
            case TARGET -> target != null ? target.position() : null;
            case BLOCK -> interactPos != null ? Vec3.atLowerCornerOf(interactPos) : null;
            case LOOK -> {
                BlockHitResult hit = raycast(caster.level(), self, range.evaluate(caster, target));
                if (hit.getType() == HitResult.Type.MISS) {
                    yield null;
                }
                BlockPos block = adjacent ? hit.getBlockPos().relative(hit.getDirection()) : hit.getBlockPos();
                yield Vec3.atLowerCornerOf(block);
            }
            case ORIGIN -> Vec3.ZERO;
        };
    }

    /** The coordinates of a bare source, as the position(...) scaling value reads them. */
    @Nullable
    public static Vec3 base(Source source, Player caster, @Nullable LivingEntity target,
            @Nullable BlockPos interactPos) {
        return of(source).base(caster, target, interactPos);
    }

    public static BlockPos toBlock(Vec3 point, ScalingValue.MathOp math) {
        return new BlockPos(round(point.x, math), round(point.y, math), round(point.z, math));
    }

    private static int round(double coordinate, ScalingValue.MathOp math) {
        return switch (math) {
            case ROUND -> (int) Math.round(coordinate);
            case CEIL -> (int) Math.ceil(coordinate);
            default -> (int) Math.floor(coordinate);
        };
    }

    /** Ray-traces block outlines (fluids ignored) along an entity's view, up to {@code range} blocks. */
    public static BlockHitResult raycast(Level level, Entity viewer, double range) {
        Vec3 eye = viewer.getEyePosition(1f);
        return level.clip(new ClipContext(eye, eye.add(viewer.getViewVector(1f).scale(range)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, viewer));
    }
}
