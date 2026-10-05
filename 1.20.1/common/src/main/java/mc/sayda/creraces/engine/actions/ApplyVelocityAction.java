package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Applies velocity to the caster or target. "pull" and "push" move it along the line to or from
 * the caster; otherwise x/y/z are used as-is, or along the entity's look direction when relative.
 */
public class ApplyVelocityAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "apply_velocity");

    /** Below this squared distance the push/pull direction is undefined, so no horizontal force is applied. */
    private static final double MIN_DIRECTION_LENGTH_SQR = 1.0E-4D;

    private final ScalingValue x;
    private final ScalingValue y;
    private final ScalingValue z;
    private final boolean relative;
    private final boolean useTarget;
    private final String mode;
    private final ScalingValue strength;
    private final boolean absolute;

    public ApplyVelocityAction(ScalingValue x, ScalingValue y, ScalingValue z, boolean relative, boolean useTarget,
            String mode, ScalingValue strength, boolean absolute) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.relative = relative;
        this.useTarget = useTarget;
        this.mode = mode;
        this.strength = strength;
        this.absolute = absolute;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity entity = useTarget ? target : player;
        if (entity == null) {
            return true;
        }

        double s = strength.evaluate(player, target, slot);
        double vx = x.evaluate(player, target, slot);
        double vy = y.evaluate(player, target, slot);
        double vz = z.evaluate(player, target, slot);

        Vec3 velocity;
        if (mode.equalsIgnoreCase("pull")) {
            velocity = direction(entity.position(), player.position()).scale(s).add(0, vy, 0);
        } else if (mode.equalsIgnoreCase("push")) {
            velocity = direction(player.position(), entity.position()).scale(s).add(0, vy, 0);
        } else if (relative) {
            velocity = Vec3.directionFromRotation(entity.getXRot(), entity.getYRot()).scale(vx).add(0, vy, 0);
        } else {
            velocity = new Vec3(vx, vy, vz);
        }

        if (absolute) {
            entity.setDeltaMovement(velocity);
        } else {
            entity.push(velocity.x, velocity.y, velocity.z);
        }
        entity.hurtMarked = true;
        return true;
    }

    private static Vec3 direction(Vec3 from, Vec3 to) {
        Vec3 diff = to.subtract(from);
        return diff.lengthSqr() > MIN_DIRECTION_LENGTH_SQR ? diff.normalize() : Vec3.ZERO;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            ScalingValue x = ScalingValue.fromJson(json, "x", 0.0);
            ScalingValue y = ScalingValue.fromJson(json, "y", 0.0);
            ScalingValue z = ScalingValue.fromJson(json, "z", 0.0);
            boolean relative = GsonHelper.getAsBoolean(json, "relative", false);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            String mode = GsonHelper.getAsString(json, "mode", "default");
            ScalingValue strength = ScalingValue.fromJson(json, "strength", 1.0);
            boolean absolute = GsonHelper.getAsBoolean(json, "absolute", false);
            return new ApplyVelocityAction(x, y, z, relative, useTarget, mode, strength, absolute);
        });
    }
}
