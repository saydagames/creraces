package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Action that applies velocity to the player or target.
 */
public class ApplyVelocityAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation("creraces", "apply_velocity");

    private final double x;
    private final double y;
    private final double z;
    private final boolean relative;
    private final boolean useTarget;
    private final String mode; // push, pull
    private final double strength;

    public ApplyVelocityAction(double x, double y, double z, boolean relative, boolean useTarget, String mode,
            double strength) {
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
            double x = GsonHelper.getAsDouble(json, "x", 0.0);
            double y = GsonHelper.getAsDouble(json, "y", 0.0);
            double z = GsonHelper.getAsDouble(json, "z", 0.0);
            boolean relative = GsonHelper.getAsBoolean(json, "relative", false);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            String mode = GsonHelper.getAsString(json, "mode", "push");
            double strength = GsonHelper.getAsDouble(json, "strength", 1.0);
            return new ApplyVelocityAction(x, y, z, relative, useTarget, mode, strength);
        });
    }

    @Override
    public void execute(@Nonnull Player p, @Nullable LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        LivingEntity entity = useTarget ? target : p;
        if (entity == null)
            return;

        Vec3 velocity;
        if (mode.equalsIgnoreCase("pull")) {
            Vec3 dir = p.position().subtract(entity.position()).normalize();
            velocity = dir.scale(strength).add(0, y, 0); // Allow custom Y offset even in pull
        } else if (relative) {
            // Relative to entity looking direction
            float yaw = entity.getYRot();
            float pitch = entity.getXRot();
            velocity = Vec3.directionFromRotation(pitch, yaw).scale(x).add(0, y, 0);
        } else {
            velocity = new Vec3(x, y, z);
        }

        entity.push(velocity.x, velocity.y, velocity.z);
        entity.hurtMarked = true;
    }
}
