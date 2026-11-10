package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.GlideTracker;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Launches the caster with "strength" in a "direction": forward (along the view, the default),
 * backward, up, down, toward_target or away_from_target. The target directions fall back to
 * forward when there is no target. With "glide_animation" the caster shows the elytra gliding pose until they land.
 */
@SuppressWarnings("null")
public class DashAction implements ActionRegistry.RaceAction {
    private enum Direction { FORWARD, BACKWARD, UP, DOWN, TOWARD_TARGET, AWAY_FROM_TARGET }

    private final ScalingValue strength;
    private final Direction direction;
    /** Scales the vertical part of forward and backward dashes; 0 keeps them flat. */
    private final ScalingValue yMultiplier;
    /** Flat vertical boost added on top of any dash. */
    private final ScalingValue yBoost;
    private final boolean resetFall;
    private final boolean glideAnimation;

    private DashAction(ScalingValue strength, Direction direction, ScalingValue yMultiplier, ScalingValue yBoost,
            boolean resetFall, boolean glideAnimation) {
        this.strength = strength;
        this.direction = direction;
        this.yMultiplier = yMultiplier;
        this.yBoost = yBoost;
        this.resetFall = resetFall;
        this.glideAnimation = glideAnimation;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        double power = strength.evaluate(player, target, slot, interactPos);
        double boost = yBoost.evaluate(player, target, slot, interactPos);
        Vec3 look = player.getLookAngle();

        Vec3 dash = switch (direction) {
            case BACKWARD -> look.reverse();
            case UP -> new Vec3(0, 1, 0);
            case DOWN -> new Vec3(0, -1, 0);
            case TOWARD_TARGET -> target != null ? bodyCenter(target).subtract(player.getEyePosition()).normalize() : look;
            case AWAY_FROM_TARGET -> target != null ? player.getEyePosition().subtract(bodyCenter(target)).normalize() : look;
            case FORWARD -> look;
        };
        boolean vertical = direction == Direction.UP || direction == Direction.DOWN;
        double yScale = vertical ? 1.0 : yMultiplier.evaluate(player, target, slot, interactPos);

        player.setDeltaMovement(player.getDeltaMovement().add(dash.x * power, dash.y * power * yScale + boost,
                dash.z * power));
        player.hurtMarked = true;
        if (resetFall) {
            player.fallDistance = 0;
        }
        if (glideAnimation && player instanceof ServerPlayer serverPlayer) {
            GlideTracker.start(serverPlayer);
        }
        return true;
    }

    private static Vec3 bodyCenter(LivingEntity entity) {
        return entity.position().add(0, entity.getBbHeight() * 0.5, 0);
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("dash"), json -> {
            String directionName = GsonHelper.getAsString(json, "direction", "forward");
            Direction direction;
            try {
                direction = Direction.valueOf(directionName.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("unknown direction '" + directionName
                        + "' (forward, backward, up, down, toward_target, away_from_target)");
            }
            return new DashAction(
                    ScalingValue.fromJson(json, "strength", 1.0),
                    direction,
                    ScalingValue.fromJson(json, "y_multiplier", 0.0),
                    ScalingValue.fromJson(json, "y_boost", 0.0),
                    GsonHelper.getAsBoolean(json, "reset_fall", false),
                    GsonHelper.getAsBoolean(json, "glide_animation", false));
        });
    }
}
