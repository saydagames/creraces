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

/**
 * Applies velocity to the player or target.
 */
public class ApplyVelocityAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation("creraces", "apply_velocity");

    private final ScalingValue x;
    private final ScalingValue y;
    private final ScalingValue z;
    private final boolean relative;
    private final boolean useTarget;
    private final String mode; // push, pull, default
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

    @SuppressWarnings("null")
    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable AbilitySlot slot, @javax.annotation.Nullable BlockPos interactionPos) {
        LivingEntity entity = (target != null) ? target : (useTarget ? null : player);
        if (entity == null)
            return false;

        double s = strength.evaluate(player, target, slot);
        double vx = x.evaluate(player, target, slot);
        double vy = y.evaluate(player, target, slot);
        double vz = z.evaluate(player, target, slot);

        Vec3 velocity;
        if (mode.equalsIgnoreCase("pull")) {
            Vec3 diff = player.position().subtract(entity.position());
            Vec3 dir = diff.lengthSqr() > 1.0E-4D ? diff.normalize() : Vec3.ZERO;
            velocity = dir.scale(s).add(0, vy, 0); 
        } else if (mode.equalsIgnoreCase("push")) {
            Vec3 diff = entity.position().subtract(player.position());
            Vec3 dir = diff.lengthSqr() > 1.0E-4D ? diff.normalize() : Vec3.ZERO;
            velocity = dir.scale(s).add(0, vy, 0);
        } else if (relative) {
            float yaw = entity.getYRot();
            float pitch = entity.getXRot();
            velocity = Vec3.directionFromRotation(pitch, yaw).scale(vx).add(0, vy, 0);
        } else {
            velocity = new Vec3(vx, vy, vz);
        }

        if (absolute) {
            entity.setDeltaMovement(velocity);
            entity.hurtMarked = true;
        } else {
            entity.push(velocity.x, velocity.y, velocity.z);
            entity.hurtMarked = true;
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "apply_velocity"), json -> {
            ScalingValue x = ScalingValue.fromJson(json, "x", 0.0);
            ScalingValue y = ScalingValue.fromJson(json, "y", 0.0);
            ScalingValue z = ScalingValue.fromJson(json, "z", 0.0);
            boolean relative = GsonHelper.getAsBoolean(json, "relative", false);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", true);
            String mode = GsonHelper.getAsString(json, "mode", "default");
            ScalingValue strength = ScalingValue.fromJson(json, "strength", 1.0);
            boolean absolute = GsonHelper.getAsBoolean(json, "absolute", false);
            return new ApplyVelocityAction(x, y, z, relative, useTarget, mode, strength, absolute);
        });
    }
}
