package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import mc.sayda.creraces.registry.ModGameRules;

public class RemoveBlockAction implements ActionRegistry.RaceAction {
    private final ScalingValue x;
    private final ScalingValue y;
    private final ScalingValue z;
    private final boolean useTarget;
    private final boolean useTargetBlock;
    private final boolean absolute;
    private final ScalingValue.MathOp coordinateMath;

    public RemoveBlockAction(ScalingValue x, ScalingValue y, ScalingValue z, boolean useTarget,
            boolean useTargetBlock, boolean absolute, ScalingValue.MathOp coordinateMath) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.useTarget = useTarget;
        this.useTargetBlock = useTargetBlock;
        this.absolute = absolute;
        this.coordinateMath = coordinateMath != null ? coordinateMath : ScalingValue.MathOp.ROUND;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {

        if (!player.level().getGameRules().getBoolean(ModGameRules.RULE_RACEGRIEFING)) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("msg.creraces.race_griefing_disabled"), true);
            return false;
        }

        BlockPos basePos;
        if (absolute) {
            basePos = BlockPos.ZERO;
        } else if (useTarget && target != null) {
            basePos = target.blockPosition();
        } else if (useTargetBlock && interactionPos != null) {
            basePos = interactionPos;
        } else {
            double tx = player.getX();
            double ty = player.getY();
            double tz = player.getZ();

            int bx = (int) Math.floor(tx);
            int by = (int) Math.floor(ty);
            int bz = (int) Math.floor(tz);

            if (coordinateMath == ScalingValue.MathOp.ROUND) {
                bx = (int) Math.round(tx);
                by = (int) Math.round(ty);
                bz = (int) Math.round(tz);
            } else if (coordinateMath == ScalingValue.MathOp.CEIL) {
                bx = (int) Math.ceil(tx);
                by = (int) Math.ceil(ty);
                bz = (int) Math.ceil(tz);
            }
            basePos = new BlockPos(bx, by, bz);
        }

        int ox = (int) x.evaluate(player, target, slot);
        int oy = (int) y.evaluate(player, target, slot);
        int oz = (int) z.evaluate(player, target, slot);

        BlockPos finalPos = basePos.offset(ox, oy, oz);

        // Protection: only remove if it's not bedrock or other unbreakable stuff?
        // Actually, Rat Tunnels use this to remove their own blocks.
        if (java.util.Objects.requireNonNull(player.level().getBlockState(finalPos)).getDestroySpeed(player.level(),
                finalPos) >= 0) {
            player.level().setBlockAndUpdate(finalPos, Blocks.AIR.defaultBlockState());
            return true;
        }

        return false;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "remove_block"), json -> {
            ScalingValue x = ScalingValue.fromJson(json, "x", 0.0);
            ScalingValue y = ScalingValue.fromJson(json, "y", 0.0);
            ScalingValue z = ScalingValue.fromJson(json, "z", 0.0);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            boolean useTargetBlock = GsonHelper.getAsBoolean(json, "use_target_block", false);
            boolean absolute = GsonHelper.getAsBoolean(json, "absolute", false);

            ScalingValue.MathOp coordinateMath = ScalingValue.MathOp.ROUND;
            if (json.has("math")) {
                try {
                    coordinateMath = ScalingValue.MathOp.valueOf(json.get("math").getAsString().toUpperCase());
                } catch (Exception e) {
                    mc.sayda.creraces.CreRaces.LOGGER.warn("Invalid math mode in RemoveBlockAction: {}",
                            json.get("math").getAsString());
                }
            }

            return new RemoveBlockAction(x, y, z, useTarget, useTargetBlock, absolute, coordinateMath);
        });
    }
}
