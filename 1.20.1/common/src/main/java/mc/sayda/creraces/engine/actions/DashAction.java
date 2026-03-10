package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class DashAction implements ActionRegistry.RaceAction {

    private final ScalingValue power;
    private final String direction; // "forward", "backward", "look", "up", "down"
    private final ScalingValue yMultiplier; // Multiplier for Y component in horizontal dashes
    private final ScalingValue yBoost; // Additive Y boost
    private final boolean resetFall;

    public DashAction(ScalingValue power, String direction, ScalingValue yMultiplier, ScalingValue yBoost,
            boolean resetFall) {
        this.power = power;
        this.direction = direction;
        this.yMultiplier = yMultiplier;
        this.yBoost = yBoost;
        this.resetFall = resetFall;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        double p = power.evaluate(player, target);
        double ym = yMultiplier.evaluate(player, target);
        double yb = yBoost.evaluate(player, target);
        Vec3 look = player.getLookAngle();
        Vec3 motion = player.getDeltaMovement();

        Vec3 dashVec;
        if ("backward".equalsIgnoreCase(direction)) {
            dashVec = look.reverse();
        } else if ("up".equalsIgnoreCase(direction)) {
            dashVec = new Vec3(0, 1, 0);
        } else if ("down".equalsIgnoreCase(direction)) {
            dashVec = new Vec3(0, -1, 0);
        } else {
            // "forward" or "look" (default)
            dashVec = look;
        }

        // Apply dash with configurable Y scaling
        double yScale = "up".equalsIgnoreCase(direction) || "down".equalsIgnoreCase(direction)
                ? 1.0
                : ym;
        player.setDeltaMovement(motion.add(dashVec.x * p, dashVec.y * p * yScale + yb, dashVec.z * p));
        player.hurtMarked = true;

        if (resetFall) {
            player.fallDistance = 0;
        }

        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "dash"), json -> {
            ScalingValue power = ScalingValue.fromJson(json, "power", 1.0);
            String direction = GsonHelper.getAsString(json, "direction", "forward");
            boolean resetFall = GsonHelper.getAsBoolean(json, "reset_fall", false);
            ScalingValue yMultiplier = ScalingValue.fromJson(json, "y_multiplier", 0.0);
            ScalingValue yBoost = ScalingValue.fromJson(json, "y_boost", 0.0);

            return new DashAction(power, direction, yMultiplier, yBoost, resetFall);
        });
    }
}
