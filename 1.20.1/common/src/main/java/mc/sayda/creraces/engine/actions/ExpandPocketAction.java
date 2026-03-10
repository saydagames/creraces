package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class ExpandPocketAction implements ActionRegistry.RaceAction {
    private final ScalingValue cost;
    private final ScalingValue limit;
    private final Map<Direction, ExpansionRule> rules;
    private final boolean defaultDoorwayClearance;
    private final ResourceLocation doorBlock;
    private final int doorWidth;
    private final int doorHeight;
    private final int doorDepth;

    public ExpandPocketAction(ScalingValue cost, ScalingValue limit, Map<Direction, ExpansionRule> rules,
            boolean defaultDoorwayClearance, ResourceLocation doorBlock, int doorWidth, int doorHeight, int doorDepth) {
        this.cost = cost;
        this.limit = limit;
        this.rules = rules;
        this.defaultDoorwayClearance = defaultDoorwayClearance;
        this.doorBlock = doorBlock;
        this.doorWidth = doorWidth;
        this.doorHeight = doorHeight;
        this.doorDepth = doorDepth;
    }

    private static class ExpansionRule {
        final ResourceLocation structure;
        final ScalingValue offsetX;
        final ScalingValue offsetY;
        final ScalingValue offsetZ;
        final String mode; // "STRUCTURE" or "SHELL"
        final ResourceLocation shellBlock;
        final int shellRadius;
        final int shellHeight;
        /**
         * Optional: block type to check at a specific offset from the panel.
         * If set and the block at (checkX, checkY, checkZ) is NOT this type, the room
         * already exists — only the door is removed, no cost, no counter increment.
         */
        @Nullable
        final ResourceLocation checkBlock;

        /**
         * Optional: explicit offset from panel to the position to check for checkBlock.
         * When set, overrides the default "1 step into the wall" derivation.
         */
        @Nullable
        final Integer checkX;
        @Nullable
        final Integer checkY;
        @Nullable
        final Integer checkZ;

        ExpansionRule(ResourceLocation structure, ScalingValue offsetX, ScalingValue offsetY, ScalingValue offsetZ,
                String mode, ResourceLocation shellBlock, int shellRadius, int shellHeight,
                @Nullable ResourceLocation checkBlock,
                @Nullable Integer checkX, @Nullable Integer checkY, @Nullable Integer checkZ) {
            this.structure = structure;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.mode = mode;
            this.shellBlock = shellBlock;
            this.shellRadius = shellRadius;
            this.shellHeight = shellHeight;
            this.checkBlock = checkBlock;
            this.checkX = checkX;
            this.checkY = checkY;
            this.checkZ = checkZ;
        }
    }

    @Override
    public boolean execute(Player player, @Nullable net.minecraft.world.entity.LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @Nullable BlockPos interactionPos) {
        if (player.level().isClientSide() || interactionPos == null)
            return true;

        ServerLevel world = (ServerLevel) player.level();
        BlockState state = world.getBlockState(interactionPos);

        Direction facing;
        if (state.hasProperty(BlockStateProperties.FACING)) {
            facing = state.getValue(BlockStateProperties.FACING);
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        } else {
            CreRaces.LOGGER.warn("ExpandPocketAction: Block at {} does not have FACING property.", interactionPos);
            return false;
        }

        ExpansionRule rule = rules.get(facing);
        if (rule == null) {
            CreRaces.LOGGER.warn("ExpandPocketAction: No expansion rule defined for face {}.", facing);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("You feel a strange tingling sensation..."), true);
            return false;
        }

        DataUtils.getVariables(player).ifPresentOrElse(vars -> {
            double currentCost = cost.evaluate(player, target);
            int maxLimit = (int) Math.round(limit.evaluate(player, target));

            Block doorMatchBlock = BuiltInRegistries.BLOCK.get(doorBlock);

            // -----------------------------------------------------------------------
            // Pre-check: is there already a room on the other side of this panel?
            // Uses the floor-of-new-room check position (matching legacy logic):
            // floor IS petrified_wood → room already placed → door-only mode
            // floor IS AIR / void → no room yet → full expansion
            // -----------------------------------------------------------------------
            boolean roomAlreadyExists = false;
            if (!"SHELL".equalsIgnoreCase(rule.mode) && rule.checkBlock != null) {
                Block expectedBlock = BuiltInRegistries.BLOCK.get(rule.checkBlock);
                BlockPos checkPos;
                if (rule.checkX != null || rule.checkY != null || rule.checkZ != null) {
                    int cx = rule.checkX != null ? rule.checkX : 0;
                    int cy = rule.checkY != null ? rule.checkY : 0;
                    int cz = rule.checkZ != null ? rule.checkZ : 0;
                    checkPos = interactionPos.offset(cx, cy, cz);
                } else {
                    checkPos = interactionPos.relative(facing.getOpposite(), 1);
                }
                // Room exists when the floor block IS the expected wall material
                roomAlreadyExists = world.getBlockState(checkPos).getBlock() == expectedBlock;
            }

            // If room already exists, open the door for free regardless of limit/coins
            if (roomAlreadyExists) {
                if (defaultDoorwayClearance) {
                    WorldUtils.removeDoor(world, interactionPos,
                            doorMatchBlock.defaultBlockState(), facing, doorWidth, doorHeight, doorDepth);
                }
                world.playSound(null, interactionPos,
                        net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value(),
                        net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Connected to existing room!"), true);
                return; // no cost, no counter
            }

            // Normal expansion: apply limit and coin gates
            if (vars.getPocketSize() >= maxLimit) {
                world.playSound(null, interactionPos, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value(),
                        net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Max room expansion reached! ("
                                + (int) vars.getPocketSize() + " / " + maxLimit + ")"),
                        true);
                return;
            }

            if (vars.getCoins() < currentCost) {
                player.displayClientMessage(net.minecraft.network.chat.Component
                        .literal("You don't have enough Coins to expand! (Required: " + (int) currentCost + ")"),
                        false);
                return;
            }

            if ("SHELL".equalsIgnoreCase(rule.mode)) {
                // Legacy "fill" logic
                Block shellBlock = BuiltInRegistries.BLOCK.get(rule.shellBlock);
                if (shellBlock == Blocks.AIR && !rule.shellBlock.equals(new ResourceLocation("minecraft:air"))) {
                    CreRaces.LOGGER.error("ExpandPocketAction: Shell block not found: {}", rule.shellBlock);
                    return;
                }

                int r = rule.shellRadius;
                BlockPos p1 = interactionPos.offset(r, 5, r);
                BlockPos p2 = interactionPos.offset(-r, -2, -r);

                // Fill logic: box outline
                for (BlockPos p : BlockPos.betweenClosed(p1, p2)) {
                    boolean edge = p.getX() == p1.getX() || p.getX() == p2.getX() ||
                            p.getY() == p1.getY() || p.getY() == p2.getY() ||
                            p.getZ() == p1.getZ() || p.getZ() == p2.getZ();
                    if (edge) {
                        world.setBlock(p, shellBlock.defaultBlockState(), 3);
                    }
                }
            } else {
                // Default STRUCTURE logic
                int dx = (int) Math.round(rule.offsetX.evaluate(player, target));
                int dy = (int) Math.round(rule.offsetY.evaluate(player, target));
                int dz = (int) Math.round(rule.offsetZ.evaluate(player, target));

                // Step 1: Remove the door panel BEFORE placing the structure (matches legacy
                // RemoveDoorProcedure first call)
                if (defaultDoorwayClearance) {
                    WorldUtils.removeDoor(world, interactionPos, doorMatchBlock.defaultBlockState(),
                            facing, doorWidth, doorHeight, doorDepth);
                }

                BlockPos targetPos = interactionPos.offset(dx, dy, dz);
                StructureTemplate template = world.getStructureManager().getOrCreate(rule.structure);
                if (template != null) {
                    template.placeInWorld(world, targetPos, targetPos, new StructurePlaceSettings(), world.random, 3);
                } else {
                    CreRaces.LOGGER.error("ExpandPocketAction: Structure not found: {}", rule.structure);
                    return;
                }

                // Step 2: Remove door again after placement to clear the new structure's shared
                // wall
                // (matches legacy RemoveDoorProcedure second call)
                if (defaultDoorwayClearance) {
                    WorldUtils.removeDoor(world, interactionPos, doorMatchBlock.defaultBlockState(),
                            facing, doorWidth, doorHeight, doorDepth);
                }
            }

            world.playSound(null, interactionPos, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value(),
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);

            vars.setCoins(vars.getCoins() - currentCost);
            vars.setPocketSize(vars.getPocketSize() + 1);

            player.displayClientMessage(net.minecraft.network.chat.Component
                    .literal("Expansions remaining: (" + (int) vars.getPocketSize() + " / " + maxLimit + ")"), true);
        }, () -> {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("You feel a strange tingling sensation..."), true);
        });

        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation("creraces:expand_pocket"), json -> {
            ScalingValue cost = ScalingValue.fromJson(json, "cost", 200.0);
            ScalingValue limit = ScalingValue.fromJson(json, "limit", 9.0);
            boolean doorwayClearance = GsonHelper.getAsBoolean(json, "doorway_clearance", true);
            ResourceLocation doorwayMatch = new ResourceLocation(
                    GsonHelper.getAsString(json, "door_block", "creraces:dryad_expansion_panel"));
            int doorWidth = GsonHelper.getAsInt(json, "door_width", 3);
            int doorHeight = GsonHelper.getAsInt(json, "door_height", 3);
            int doorDepth = GsonHelper.getAsInt(json, "door_depth", 2);

            Map<Direction, ExpansionRule> rules = new HashMap<>();
            if (json.has("faces") && json.get("faces").isJsonObject()) {
                JsonObject facesObj = json.getAsJsonObject("faces");
                for (Direction dir : Direction.values()) {
                    String key = dir.getName().toLowerCase();
                    if (facesObj.has(key)) {
                        JsonObject faceJson = facesObj.getAsJsonObject(key);
                        ResourceLocation structure = new ResourceLocation(
                                GsonHelper.getAsString(faceJson, "structure", "creraces:pocket"));
                        ScalingValue ox = ScalingValue.fromJson(faceJson, "offset_x", 0.0);
                        ScalingValue oy = ScalingValue.fromJson(faceJson, "offset_y", 0.0);
                        ScalingValue oz = ScalingValue.fromJson(faceJson, "offset_z", 0.0);
                        String mode = GsonHelper.getAsString(faceJson, "mode", "STRUCTURE");
                        ResourceLocation shellBlock = new ResourceLocation(
                                GsonHelper.getAsString(faceJson, "shell_block", "creraces:dryad_petrified_wood"));
                        int shellRadius = GsonHelper.getAsInt(faceJson, "shell_radius", 14);
                        int shellHeight = GsonHelper.getAsInt(faceJson, "shell_height", 7);
                        String checkBlockStr = GsonHelper.getAsString(faceJson, "check_block", null);
                        ResourceLocation checkBlock = checkBlockStr != null ? new ResourceLocation(checkBlockStr)
                                : null;
                        Integer checkX = faceJson.has("check_x") ? faceJson.get("check_x").getAsInt()
                                : null;
                        Integer checkY = faceJson.has("check_y") ? faceJson.get("check_y").getAsInt()
                                : null;
                        Integer checkZ = faceJson.has("check_z") ? faceJson.get("check_z").getAsInt()
                                : null;

                        rules.put(dir,
                                new ExpansionRule(structure, ox, oy, oz, mode, shellBlock, shellRadius, shellHeight,
                                        checkBlock, checkX, checkY, checkZ));
                    }
                }
            } else {
                // Fallback for legacy JSON format
                ResourceLocation structure = new ResourceLocation(
                        GsonHelper.getAsString(json, "structure", "creraces:pocket"));
                ScalingValue ox = ScalingValue.fromJson(json, "offset_x", 0.0);
                ScalingValue oy = ScalingValue.fromJson(json, "offset_y", 0.0);
                ScalingValue oz = ScalingValue.fromJson(json, "offset_z", 0.0);
                ExpansionRule defaultRule = new ExpansionRule(structure, ox, oy, oz, "STRUCTURE",
                        new ResourceLocation("minecraft:air"), 0, 0, null, null, null, null);
                for (Direction dir : Direction.values()) {
                    if (dir.getAxis().isHorizontal()) {
                        rules.put(dir, defaultRule);
                    }
                }
            }

            return new ExpandPocketAction(cost, limit, rules, doorwayClearance, doorwayMatch, doorWidth, doorHeight,
                    doorDepth);
        });
    }
}
