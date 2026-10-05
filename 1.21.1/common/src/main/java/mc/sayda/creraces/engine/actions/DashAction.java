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
 * Launches the caster in a direction: "forward"/"look" (default), "backward", "up", "down",
 * "toward_target" or "away_from_target". The target directions fall back to the look direction
 * when there is no target.
 */
public class DashAction implements ActionRegistry.RaceAction {

    private final ScalingValue power;
    private final String direction;
    /** Scales the vertical part of look-based dashes; 0 keeps them flat. */
    private final ScalingValue yMultiplier;
    /** Flat vertical boost added on top of any dash. */
    private final ScalingValue yBoost;
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
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        double p = power.evaluate(player, target, slot);
        double ym = yMultiplier.evaluate(player, target, slot);
        double yb = yBoost.evaluate(player, target, slot);
        Vec3 look = player.getLookAngle();

        boolean vertical = "up".equalsIgnoreCase(direction) || "down".equalsIgnoreCase(direction);
        Vec3 dashVec;
        if ("backward".equalsIgnoreCase(direction)) {
            dashVec = look.reverse();
        } else if ("up".equalsIgnoreCase(direction)) {
            dashVec = new Vec3(0, 1, 0);
        } else if ("down".equalsIgnoreCase(direction)) {
            dashVec = new Vec3(0, -1, 0);
        } else if ("toward_target".equalsIgnoreCase(direction) && target != null) {
            dashVec = bodyCenter(target).subtract(player.getEyePosition()).normalize();
        } else if ("away_from_target".equalsIgnoreCase(direction) && target != null) {
            dashVec = player.getEyePosition().subtract(bodyCenter(target)).normalize();
        } else {
            dashVec = look;
        }

        double yScale = vertical ? 1.0 : ym;
        player.setDeltaMovement(player.getDeltaMovement().add(dashVec.x * p, dashVec.y * p * yScale + yb,
                dashVec.z * p));
        player.hurtMarked = true;
        if (resetFall) {
            player.fallDistance = 0;
        }
        return true;
    }

    private static Vec3 bodyCenter(LivingEntity entity) {
        return entity.position().add(0, entity.getBbHeight() * 0.5, 0);
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "dash"), json -> new DashAction(
                ScalingValue.fromJson(json, "power", 1.0),
                GsonHelper.getAsString(json, "direction", "forward"),
                ScalingValue.fromJson(json, "y_multiplier", 0.0),
                ScalingValue.fromJson(json, "y_boost", 0.0),
                GsonHelper.getAsBoolean(json, "reset_fall", false)));
    }
}
