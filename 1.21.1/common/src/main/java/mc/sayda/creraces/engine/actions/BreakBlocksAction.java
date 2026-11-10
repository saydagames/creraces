package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.Position;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.registry.ModGameRules;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;

/**
 * Breaks the block at "position" (the caster's feet by default), or every block in a cube of
 * "radius" around it. "drop_items" (default true) drops them as mining would. Unbreakable blocks
 * and blocks harder than the configured limit are left alone unless "bypass" is set.
 */
@SuppressWarnings("null")
public class BreakBlocksAction implements ActionRegistry.RaceAction {
    private final Position position;
    private final ScalingValue radius;
    private final boolean dropItems;
    private final boolean bypass;

    public BreakBlocksAction(Position position, ScalingValue radius, boolean dropItems, boolean bypass) {
        this.position = position;
        this.radius = radius;
        this.dropItems = dropItems;
        this.bypass = bypass;
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
        BlockPos center = position.resolveBlock(player, target, slot, interactPos);
        if (center == null) {
            return false;
        }

        double r = radius.evaluate(player, target, slot, interactPos);
        int maxRadius = CreRacesConfig.BREAK_BLOCKS_MAX_RADIUS.get();
        if (maxRadius > 0) {
            r = Math.min(r, maxRadius);
        }
        int reach = (int) Math.max(0, r);
        boolean brokeAny = false;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-reach, -reach, -reach), center.offset(reach, reach, reach))) {
            if (canBreak(level, pos)) {
                brokeAny |= dropItems ? level.destroyBlock(pos, true, player)
                        : level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
        return brokeAny;
    }

    /** Air never counts; a negative hardness limit in the config turns the limit off. */
    private boolean canBreak(Level level, BlockPos pos) {
        if (level.getBlockState(pos).isAir()) {
            return false;
        }
        float hardness = level.getBlockState(pos).getDestroySpeed(level, pos);
        if (bypass) {
            return true;
        }
        float limit = CreRacesConfig.REMOVE_BLOCK_HARDNESS_LIMIT.get().floatValue();
        return hardness >= 0 && (limit < 0 || hardness <= limit);
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("break_blocks"), json -> new BreakBlocksAction(
                Position.fromJson(json, "position", Position.of(Position.Source.SELF)),
                ScalingValue.fromJson(json, "radius", 0.0),
                GsonHelper.getAsBoolean(json, "drop_items", true),
                GsonHelper.getAsBoolean(json, "bypass", false)));
    }
}
