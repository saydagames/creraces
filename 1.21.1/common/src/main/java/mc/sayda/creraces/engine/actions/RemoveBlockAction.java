package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.registry.ModGameRules;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;

/** Replaces a single block with air, without drops, subject to the configured hardness limit. */
public class RemoveBlockAction implements ActionRegistry.RaceAction {
    private final ScalingValue x;
    private final ScalingValue y;
    private final ScalingValue z;
    private final boolean useTarget;
    private final boolean useTargetBlock;
    private final boolean absolute;
    private final ScalingValue.MathOp coordinateMath;
    private final String particle;
    private final String sound;
    private final int particleCount;
    private final boolean bypass;
    private final boolean useRaycast;
    private final ScalingValue rayRange;

    public RemoveBlockAction(ScalingValue x, ScalingValue y, ScalingValue z, boolean useTarget, boolean useTargetBlock,
            boolean absolute, @Nullable ScalingValue.MathOp coordinateMath, String particle, String sound,
            int particleCount, boolean bypass, boolean useRaycast, ScalingValue rayRange) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.useTarget = useTarget;
        this.useTargetBlock = useTargetBlock;
        this.absolute = absolute;
        this.coordinateMath = coordinateMath != null ? coordinateMath : ScalingValue.MathOp.FLOOR;
        this.particle = particle;
        this.sound = sound;
        this.particleCount = particleCount;
        this.bypass = bypass;
        this.useRaycast = useRaycast;
        this.rayRange = rayRange;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        Level level = player.level();
        if (!level.getGameRules().getBoolean(ModGameRules.RULE_RACEGRIEFING)) {
            player.displayClientMessage(Component.translatable("msg.creraces.race_griefing_disabled")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        BlockPos anchor;
        if (useRaycast) {
            BlockHitResult hit = BlockTargeting.raycast(level, player, rayRange.evaluate(player, target, slot));
            if (hit.getType() == HitResult.Type.MISS) {
                return false;
            }
            anchor = hit.getBlockPos();
        } else {
            anchor = BlockTargeting.resolveAnchor(player, target, interactPos, absolute, useTarget, useTargetBlock,
                    coordinateMath);
        }
        BlockPos pos = anchor.offset(
                (int) x.evaluate(player, target, slot),
                (int) y.evaluate(player, target, slot),
                (int) z.evaluate(player, target, slot));

        float hardness = level.getBlockState(pos).getDestroySpeed(level, pos);
        float limit = CreRacesConfig.REMOVE_BLOCK_HARDNESS_LIMIT.get().floatValue();
        // A negative limit disables the check; otherwise unbreakable blocks (negative hardness) stay protected.
        boolean canRemove = bypass || limit < 0 || (hardness >= 0 && hardness <= limit);
        if (!canRemove) {
            return false;
        }

        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        BlockActionEffects.play(player, pos, particle, sound, particleCount);
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "remove_block"), json -> new RemoveBlockAction(
                ScalingValue.fromJson(json, "x", 0.0),
                ScalingValue.fromJson(json, "y", 0.0),
                ScalingValue.fromJson(json, "z", 0.0),
                GsonHelper.getAsBoolean(json, "use_target", false),
                GsonHelper.getAsBoolean(json, "use_target_block", false),
                GsonHelper.getAsBoolean(json, "absolute", false),
                BlockTargeting.parseMathOp(json, "RemoveBlockAction"),
                GsonHelper.getAsString(json, "particle", ""),
                GsonHelper.getAsString(json, "sound", ""),
                GsonHelper.getAsInt(json, "particle_count", 10),
                GsonHelper.getAsBoolean(json, "bypass", false),
                GsonHelper.getAsBoolean(json, "use_raycast", false),
                ScalingValue.fromJson(json, "ray_range", 10.0)));
    }
}
