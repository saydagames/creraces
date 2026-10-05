package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Block position resolution shared by the actions that read or edit blocks. */
final class BlockTargeting {
    /** Reach used when a coordinate falls back to the block under the crosshair. */
    private static final double PICK_RANGE = 5.0;

    private BlockTargeting() {
    }

    /**
     * The block an action works from before its offsets: the world origin when absolute, else the
     * target's block, else the interacted block, else the caster's position rounded with {@code math}.
     */
    static BlockPos resolveAnchor(Player player, @Nullable LivingEntity target, @Nullable BlockPos interactPos,
            boolean absolute, boolean useTarget, boolean useTargetBlock, ScalingValue.MathOp math) {
        if (absolute) {
            return BlockPos.ZERO;
        }
        if (useTarget && target != null) {
            return target.blockPosition();
        }
        if (useTargetBlock && interactPos != null) {
            return interactPos;
        }
        return new BlockPos(round(player.getX(), math), round(player.getY(), math), round(player.getZ(), math));
    }

    private static int round(double coordinate, ScalingValue.MathOp math) {
        return switch (math) {
            case ROUND -> (int) Math.round(coordinate);
            case CEIL -> (int) Math.ceil(coordinate);
            default -> (int) Math.floor(coordinate);
        };
    }

    /** Ray-traces block outlines (fluids ignored) along the caster's view, up to {@code range} blocks. */
    @SuppressWarnings("null")
    static BlockHitResult raycast(Level level, Player player, double range) {
        Vec3 eye = player.getEyePosition(1f);
        return level.clip(new ClipContext(eye, eye.add(player.getViewVector(1f).scale(range)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    /** The interacted block if there is one, else the block under the crosshair, else null. */
    @Nullable
    static BlockPos interactedOrLookedAt(Player player, @Nullable BlockPos interactPos) {
        if (interactPos != null) {
            return interactPos;
        }
        HitResult hit = player.pick(PICK_RANGE, 0f, false);
        return hit.getType() == HitResult.Type.BLOCK ? ((BlockHitResult) hit).getBlockPos() : null;
    }

    /** Reads the optional "math" rounding mode; a missing or unknown value falls back to FLOOR. */
    static ScalingValue.MathOp parseMathOp(JsonObject json, String actionName) {
        String name = GsonHelper.getAsString(json, "math", "FLOOR");
        try {
            return ScalingValue.MathOp.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            CreRaces.LOGGER.warn("{}: unknown math mode '{}', using FLOOR", actionName, name);
            return ScalingValue.MathOp.FLOOR;
        }
    }
}
