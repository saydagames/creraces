package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.registry.ModGameRules;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** Breaks every breakable block in a cube around the resolved position, like a player would. */
@SuppressWarnings("null")
public class BreakBlocksAction implements ActionRegistry.RaceAction {
    private final ScalingValue radius;
    private final boolean dropItems;
    private final ScalingValue offsetX;
    private final ScalingValue offsetY;
    private final ScalingValue offsetZ;
    private final boolean useTarget;
    private final boolean useTargetBlock;
    private final boolean absolute;
    private final ScalingValue.MathOp coordinateMath;

    public BreakBlocksAction(ScalingValue radius, boolean dropItems, ScalingValue offsetX, ScalingValue offsetY,
            ScalingValue offsetZ, boolean useTarget, boolean useTargetBlock, boolean absolute,
            @Nullable ScalingValue.MathOp coordinateMath) {
        this.radius = radius;
        this.dropItems = dropItems;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.useTarget = useTarget;
        this.useTargetBlock = useTargetBlock;
        this.absolute = absolute;
        this.coordinateMath = coordinateMath != null ? coordinateMath : ScalingValue.MathOp.FLOOR;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        Level level = player.level();
        if (level.isClientSide()) {
            return true;
        }
        if (!level.getGameRules().getBoolean(ModGameRules.RULE_RACEGRIEFING)) {
            player.displayClientMessage(Component.translatable("msg.creraces.race_griefing_disabled"), true);
            return false;
        }

        BlockPos center = BlockTargeting.resolveAnchor(player, target, interactPos, absolute, useTarget,
                useTargetBlock, coordinateMath).offset(
                        (int) offsetX.evaluate(player, target, slot),
                        (int) offsetY.evaluate(player, target, slot),
                        (int) offsetZ.evaluate(player, target, slot));

        double r = radius.evaluate(player, target, slot);
        int maxRadius = CreRacesConfig.BREAK_BLOCKS_MAX_RADIUS.get();
        if (maxRadius > 0) {
            r = Math.min(r, maxRadius);
        }
        for (int x = -(int) r; x <= r; x++) {
            for (int y = -(int) r; y <= r; y++) {
                for (int z = -(int) r; z <= r; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    // A destroy speed of -1 marks unbreakable blocks (bedrock, command blocks, ...).
                    if (level.getBlockState(pos).getDestroySpeed(level, pos) >= 0) {
                        level.destroyBlock(pos, dropItems, player);
                    }
                }
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "break_blocks"), json -> new BreakBlocksAction(
                ScalingValue.fromJson(json, "radius", 1.0),
                GsonHelper.getAsBoolean(json, "drop_items", true),
                ScalingValue.fromJson(json, "offset_x", 0.0),
                ScalingValue.fromJson(json, "offset_y", 0.0),
                ScalingValue.fromJson(json, "offset_z", 0.0),
                GsonHelper.getAsBoolean(json, "use_target", false),
                GsonHelper.getAsBoolean(json, "use_target_block", false),
                GsonHelper.getAsBoolean(json, "absolute", false),
                BlockTargeting.parseMathOp(json, "BreakBlocksAction")));
    }
}
