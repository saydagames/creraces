package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.registry.ModGameRules;
import net.minecraft.world.phys.AABB;

public class PlaceBlockAction implements ActionRegistry.RaceAction {
    private final ResourceLocation block;
    private final boolean useTarget;
    private final boolean useTargetBlock;
    private final ScalingValue offsetX;
    private final ScalingValue offsetY;
    private final ScalingValue offsetZ;
    private final boolean overwrite;
    private final boolean absolute;
    private final ScalingValue.MathOp coordinateMath;

    public PlaceBlockAction(ResourceLocation block, boolean useTarget, boolean useTargetBlock, ScalingValue offsetX,
            ScalingValue offsetY, ScalingValue offsetZ, boolean overwrite, boolean absolute,
            ScalingValue.MathOp coordinateMath) {
        this.block = block;
        this.useTarget = useTarget;
        this.useTargetBlock = useTargetBlock;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.overwrite = overwrite;
        this.absolute = absolute;
        this.coordinateMath = coordinateMath != null ? coordinateMath : ScalingValue.MathOp.ROUND;
    }

    @SuppressWarnings("null")
    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {

        if (!player.level().getGameRules().getBoolean(ModGameRules.RULE_RACEGRIEFING)) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("msg.creraces.race_griefing_disabled"), true);
            return false;
        }

        BlockPos targetPos;
        if (absolute) {
            targetPos = BlockPos.ZERO;
        } else if (useTarget && target != null) {
            targetPos = target.blockPosition();
        } else if (useTargetBlock && interactionPos != null) {
            targetPos = interactionPos;
        } else {
            double tx = player.getX();
            double ty = player.getY();
            double tz = player.getZ();

            // Apply coordinate rounding mode
            int x = (int) Math.floor(tx);
            int y = (int) Math.floor(ty);
            int z = (int) Math.floor(tz);

            if (coordinateMath == ScalingValue.MathOp.ROUND) {
                x = (int) Math.round(tx);
                y = (int) Math.round(ty);
                z = (int) Math.round(tz);
            } else if (coordinateMath == ScalingValue.MathOp.CEIL) {
                x = (int) Math.ceil(tx);
                y = (int) Math.ceil(ty);
                z = (int) Math.ceil(tz);
            }
            targetPos = new BlockPos(x, y, z);
        }

        int ox = (int) offsetX.evaluate(player, target, slot);
        int oy = (int) offsetY.evaluate(player, target, slot);
        int oz = (int) offsetZ.evaluate(player, target, slot);
        BlockPos finalPos = targetPos.offset(ox, oy, oz);

        net.minecraft.world.level.block.Block resolvedBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .get(block);

        // Respect the micro-block whitelist
        if (!mc.sayda.creraces.engine.MicroBlockWhitelist.isAllowed(resolvedBlock)) {
            CreRaces.LOGGER.warn("PlaceBlockAction blocked: {} is not whitelisted", block);
            return false;
        }

        // Check if block is replaceable
        boolean canPlace = overwrite || player.level().getBlockState(finalPos).isAir()
                || player.level().getBlockState(finalPos).canBeReplaced();

        // Check for entity obstruction (vanilla behavior)
        if (canPlace && !overwrite) {
            if (!player.level().getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new AABB(finalPos), e -> e != player).isEmpty()) {
                canPlace = false;
            }
        }

        if (canPlace) {
            // Protection for RootBlock: only owner (or creative) can replace it.
            net.minecraft.world.level.block.state.BlockState existingState = player.level().getBlockState(finalPos);
            if (existingState.getBlock() instanceof mc.sayda.creraces.block.RootBlock) {
                if (!player.isCreative() && !mc.sayda.creraces.block.RootBlock.isOwner(player, finalPos)) {
                    return false;
                }
            }

            player.level().setBlockAndUpdate(finalPos, resolvedBlock.defaultBlockState());
            return true;
        } else {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("msg.creraces.place_block_failed"), true);
        }

        return false;
    }

    @SuppressWarnings("null")
    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "place_block"), json -> {
            @SuppressWarnings("null")
            String blockId = GsonHelper.getAsString(json, "block", "minecraft:air");
            ResourceLocation block = new ResourceLocation(blockId);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            boolean useTargetBlock = GsonHelper.getAsBoolean(json, "use_target_block", false);
            ScalingValue offsetX = ScalingValue.fromJson(json, "offset_x", 0.0);
            ScalingValue offsetY = ScalingValue.fromJson(json, "offset_y", 0.0);
            ScalingValue offsetZ = ScalingValue.fromJson(json, "offset_z", 0.0);
            boolean overwrite = GsonHelper.getAsBoolean(json, "overwrite", false);
            boolean absolute = GsonHelper.getAsBoolean(json, "absolute", false);

            ScalingValue.MathOp coordinateMath = ScalingValue.MathOp.ROUND;
            if (json.has("math")) {
                try {
                    coordinateMath = ScalingValue.MathOp.valueOf(json.get("math").getAsString().toUpperCase());
                } catch (Exception e) {
                    CreRaces.LOGGER.warn("Invalid math mode in PlaceBlockAction: {}", json.get("math").getAsString());
                }
            }

            return new PlaceBlockAction(block, useTarget, useTargetBlock, offsetX, offsetY, offsetZ, overwrite, absolute,
                    coordinateMath);
        });
    }
}
