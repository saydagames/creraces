package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.CastSource;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Moves an entity. "mode" picks the direction: "vector" (x, y, z as given), "look" (x along the
 * entity's view, plus y), "push" (away from the caster) or "pull" (towards the caster), the last
 * two scaled by "strength" and lifted by y. "operation" "add" (default) pushes, "set" replaces the motion.
 */
public class ApplyVelocityAction implements ActionRegistry.RaceAction {
    /** Below this squared distance the push/pull direction is undefined, so no horizontal force is applied. */
    private static final double MIN_DIRECTION_LENGTH_SQR = 1.0E-4D;

    private enum Mode { VECTOR, LOOK, PUSH, PULL }

    private final Mode mode;
    private final ScalingValue x;
    private final ScalingValue y;
    private final ScalingValue z;
    private final ScalingValue strength;
    private final boolean replace;
    private final EntitySelector entity;

    private ApplyVelocityAction(Mode mode, ScalingValue x, ScalingValue y, ScalingValue z, ScalingValue strength,
            boolean replace, EntitySelector entity) {
        this.mode = mode;
        this.x = x;
        this.y = y;
        this.z = z;
        this.strength = strength;
        this.replace = replace;
        this.entity = entity;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity subject = entity.resolve(player, target);
        if (subject == null) {
            return true;
        }
        double vx = x.evaluate(player, target, slot, interactPos);
        double vy = y.evaluate(player, target, slot, interactPos);
        double vz = z.evaluate(player, target, slot, interactPos);
        double s = strength.evaluate(player, target, slot, interactPos);

        Vec3 velocity = switch (mode) {
            case VECTOR -> new Vec3(vx, vy, vz);
            case LOOK -> Vec3.directionFromRotation(subject.getXRot(), subject.getYRot()).scale(vx).add(0, vy, 0);
            case PUSH -> direction(CastSource.self(player).position(), subject.position()).scale(s).add(0, vy, 0);
            case PULL -> direction(subject.position(), CastSource.self(player).position()).scale(s).add(0, vy, 0);
        };
        if (replace) {
            subject.setDeltaMovement(velocity);
        } else {
            subject.push(velocity.x, velocity.y, velocity.z);
        }
        subject.hurtMarked = true;
        return true;
    }

    private static Vec3 direction(Vec3 from, Vec3 to) {
        Vec3 diff = to.subtract(from);
        return diff.lengthSqr() > MIN_DIRECTION_LENGTH_SQR ? diff.normalize() : Vec3.ZERO;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("apply_velocity"), json -> {
            String modeName = GsonHelper.getAsString(json, "mode", "vector");
            Mode mode = switch (modeName) {
                case "vector" -> Mode.VECTOR;
                case "look" -> Mode.LOOK;
                case "push" -> Mode.PUSH;
                case "pull" -> Mode.PULL;
                default -> throw new IllegalArgumentException("unknown mode '" + modeName + "' (vector, look, push, pull)");
            };
            String operation = GsonHelper.getAsString(json, "operation", "add");
            if (!operation.equals("add") && !operation.equals("set")) {
                throw new IllegalArgumentException("unknown operation '" + operation + "' (add, set)");
            }
            return new ApplyVelocityAction(mode,
                    ScalingValue.fromJson(json, "x", 0.0),
                    ScalingValue.fromJson(json, "y", 0.0),
                    ScalingValue.fromJson(json, "z", 0.0),
                    ScalingValue.fromJson(json, "strength", 1.0),
                    operation.equals("set"),
                    EntitySelector.fromJson(json, EntitySelector.AUTO));
        });
    }
}
