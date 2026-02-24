package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
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
    private final double yMultiplier; // Multiplier for Y component in horizontal dashes
    private final double yBoost; // Additive Y boost

    public DashAction(ScalingValue power, String direction, double yMultiplier, double yBoost) {
        this.power = power;
        this.direction = direction;
        this.yMultiplier = yMultiplier;
        this.yBoost = yBoost;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        double p = power.evaluate(player);
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
        double yScale = "up".equalsIgnoreCase(direction) || "down".equalsIgnoreCase(direction) ? 1.0 : yMultiplier;
        player.setDeltaMovement(motion.add(dashVec.x * p, dashVec.y * p * yScale + yBoost, dashVec.z * p));
        player.hurtMarked = true;
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "dash"), json -> {
            ScalingValue power = ScalingValue.fromJson(json, "power", 1.0);
            String direction = GsonHelper.getAsString(json, "direction", "forward");

            // Support nested power configuration with y_multiplier and y_boost
            double yMultiplier = 0.5; // default
            double yBoost = 0.2; // default
            if (json.has("power") && json.get("power").isJsonObject()) {
                JsonObject powerObj = json.getAsJsonObject("power");
                yMultiplier = GsonHelper.getAsDouble(powerObj, "y_multiplier", 0.5);
                yBoost = GsonHelper.getAsDouble(powerObj, "y_boost", 0.2);
            }

            return new DashAction(power, direction, yMultiplier, yBoost);
        });
    }
}
