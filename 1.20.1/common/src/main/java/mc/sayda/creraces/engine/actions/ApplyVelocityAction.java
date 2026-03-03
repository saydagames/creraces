package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Action that applies velocity to the player or target.
 */
public class ApplyVelocityAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation("creraces", "apply_velocity");

    private final mc.sayda.creraces.engine.ScalingValue x;
    private final mc.sayda.creraces.engine.ScalingValue y;
    private final mc.sayda.creraces.engine.ScalingValue z;
    private final boolean relative;
    private final boolean useTarget;
    private final String mode; // push, pull
    private final mc.sayda.creraces.engine.ScalingValue strength;

    public ApplyVelocityAction(mc.sayda.creraces.engine.ScalingValue x, mc.sayda.creraces.engine.ScalingValue y,
            mc.sayda.creraces.engine.ScalingValue z, boolean relative, boolean useTarget, String mode,
            mc.sayda.creraces.engine.ScalingValue strength) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.relative = relative;
        this.useTarget = useTarget;
        this.mode = mode;
        this.strength = strength;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            mc.sayda.creraces.engine.ScalingValue x = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "x", 0.0);
            mc.sayda.creraces.engine.ScalingValue y = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "y", 0.0);
            mc.sayda.creraces.engine.ScalingValue z = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "z", 0.0);
            boolean relative = GsonHelper.getAsBoolean(json, "relative", false);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            String mode = GsonHelper.getAsString(json, "mode", "push");
            mc.sayda.creraces.engine.ScalingValue strength = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                    "strength", 1.0);
            return new ApplyVelocityAction(x, y, z, relative, useTarget, mode, strength);
        });
    }

    @SuppressWarnings("null")
    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        // Smart Targeting: Prefer target if present, otherwise respect useTarget flag
        LivingEntity entity = (target != null) ? target : (useTarget ? null : player);
        if (entity == null)
            return true;

        double s = strength.evaluate(player, target);
        double vx = x.evaluate(player, target);
        double vy = y.evaluate(player, target);
        double vz = z.evaluate(player, target);

        Vec3 velocity;
        if (mode.equalsIgnoreCase("pull")) {
            Vec3 diff = player.position().subtract(entity.position());
            Vec3 dir = diff.lengthSqr() > 1.0E-4D ? diff.normalize() : Vec3.ZERO;
            velocity = dir.scale(s).add(0, vy, 0); // Allow custom Y offset even in pull
        } else if (relative) {
            // Relative to entity looking direction
            float yaw = entity.getYRot();
            float pitch = entity.getXRot();
            velocity = Vec3.directionFromRotation(pitch, yaw).scale(vx).add(0, vy, 0);
        } else {
            velocity = new Vec3(vx, vy, vz);
        }

        entity.push(velocity.x, velocity.y, velocity.z);
        entity.hurtMarked = true;
        return true;
    }
}
