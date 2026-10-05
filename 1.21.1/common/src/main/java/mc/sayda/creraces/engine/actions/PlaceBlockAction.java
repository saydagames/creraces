package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.block.RootBlock;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.registry.ModGameRules;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Places a block at the resolved position (a raycast hit, the target, the interacted block or the
 * caster), optionally writing block entity data and playing particle/sound feedback.
 */
public class PlaceBlockAction implements ActionRegistry.RaceAction {
    private final ResourceLocation block;
    private final boolean useTarget;
    private final boolean useTargetBlock;
    private final boolean useRaycast;
    private final ScalingValue rayRange;
    private final ScalingValue offsetX;
    private final ScalingValue offsetY;
    private final ScalingValue offsetZ;
    private final boolean overwrite;
    private final boolean absolute;
    private final ScalingValue.MathOp coordinateMath;
    private final List<BlockDataEdit> dataEdits;
    private final String particle;
    private final int particleCount;
    private final String sound;

    private PlaceBlockAction(ResourceLocation block, boolean useTarget, boolean useTargetBlock, boolean useRaycast,
            ScalingValue rayRange, ScalingValue offsetX, ScalingValue offsetY, ScalingValue offsetZ,
            boolean overwrite, boolean absolute, ScalingValue.MathOp coordinateMath, List<BlockDataEdit> dataEdits,
            String particle, int particleCount, String sound) {
        this.block = block;
        this.useTarget = useTarget;
        this.useTargetBlock = useTargetBlock;
        this.useRaycast = useRaycast;
        this.rayRange = rayRange;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.overwrite = overwrite;
        this.absolute = absolute;
        this.coordinateMath = coordinateMath;
        this.dataEdits = dataEdits;
        this.particle = particle;
        this.particleCount = particleCount;
        this.sound = sound;
    }

    @SuppressWarnings("null")
    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        Level level = player.level();
        if (!level.getGameRules().getBoolean(ModGameRules.RULE_RACEGRIEFING)) {
            player.displayClientMessage(Component.translatable("msg.creraces.race_griefing_disabled"), true);
            return false;
        }

        BlockPos anchor;
        if (useRaycast && !absolute) {
            BlockHitResult hit = BlockTargeting.raycast(level, player, rayRange.evaluate(player, target, slot));
            if (hit.getType() == HitResult.Type.MISS) {
                return false;
            }
            anchor = hit.getBlockPos().relative(hit.getDirection());
        } else {
            anchor = BlockTargeting.resolveAnchor(player, target, interactPos, absolute, useTarget, useTargetBlock,
                    coordinateMath);
        }
        BlockPos pos = anchor.offset(
                (int) offsetX.evaluate(player, target, slot),
                (int) offsetY.evaluate(player, target, slot),
                (int) offsetZ.evaluate(player, target, slot));

        // The block registry falls back to air for unknown ids, so check the id itself.
        if (!BuiltInRegistries.BLOCK.containsKey(block)) {
            CreRaces.LOGGER.error("PlaceBlockAction: block '{}' not found in registry", block);
            return false;
        }
        Block resolvedBlock = BuiltInRegistries.BLOCK.get(block);

        BlockState existing = level.getBlockState(pos);
        // Placing onto the same block is a no-op, so fail and let the ability skip its costs and state changes.
        if (!overwrite && existing.is(resolvedBlock)) {
            return false;
        }
        if (!canPlaceAt(level, pos, existing, player)) {
            player.displayClientMessage(Component.translatable("msg.creraces.place_block_failed"), true);
            return false;
        }
        // A territory root block can only be replaced by its owner (or in creative).
        if (existing.getBlock() instanceof RootBlock && !player.isCreative() && !RootBlock.isOwner(player, pos)) {
            return false;
        }

        level.setBlockAndUpdate(pos, resolvedBlock.defaultBlockState());

        if (!dataEdits.isEmpty()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null
                    && BlockDataEdit.applyAll(dataEdits, blockEntity, player, target, slot, interactPos)) {
                BlockState placed = level.getBlockState(pos);
                level.sendBlockUpdated(pos, placed, placed, Block.UPDATE_ALL);
            }
        }

        BlockActionEffects.play(player, pos, particle, sound, particleCount);
        return true;
    }

    /** Overwrite ignores everything; otherwise the spot must be replaceable and, like vanilla, free of entities. */
    private boolean canPlaceAt(Level level, BlockPos pos, BlockState existing, Player player) {
        if (overwrite) {
            return true;
        }
        if (!existing.isAir() && !existing.canBeReplaced()) {
            return false;
        }
        return level.getEntitiesOfClass(Entity.class, new AABB(pos), e -> e != player).isEmpty();
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "place_block"), json -> new PlaceBlockAction(
                ResourceLocation.parse(GsonHelper.getAsString(json, "block", "minecraft:air")),
                GsonHelper.getAsBoolean(json, "use_target", false),
                GsonHelper.getAsBoolean(json, "use_target_block", false),
                GsonHelper.getAsBoolean(json, "use_raycast", false),
                ScalingValue.fromJson(json, "ray_range", 10.0),
                ScalingValue.fromJson(json, "offset_x", 0.0),
                ScalingValue.fromJson(json, "offset_y", 0.0),
                ScalingValue.fromJson(json, "offset_z", 0.0),
                GsonHelper.getAsBoolean(json, "overwrite", false),
                GsonHelper.getAsBoolean(json, "absolute", false),
                BlockTargeting.parseMathOp(json, "PlaceBlockAction"),
                BlockDataEdit.parseList(json),
                GsonHelper.getAsString(json, "particle", ""),
                GsonHelper.getAsInt(json, "particle_count", 10),
                GsonHelper.getAsString(json, "sound", "")));
    }
}
