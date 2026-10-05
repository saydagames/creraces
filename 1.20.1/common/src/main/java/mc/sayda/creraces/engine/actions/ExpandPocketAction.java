package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Grows a pocket dimension room through the expansion panel the player interacted with. Each panel
 * face has its own rule: place a structure template behind it, or fill a hollow shell of blocks.
 * Expansions cost coins, count towards a limit and must stay inside the owner's pocket plot.
 */
@SuppressWarnings("null")
public class ExpandPocketAction implements ActionRegistry.RaceAction {
    private final ScalingValue cost;
    private final ScalingValue limit;
    private final Map<Direction, ExpansionRule> rules;
    private final boolean defaultDoorwayClearance;
    @Nullable
    private final ResourceLocation doorBlock;
    private final int doorWidth;
    private final int doorHeight;
    private final int doorDepth;

    public ExpandPocketAction(ScalingValue cost, ScalingValue limit, Map<Direction, ExpansionRule> rules,
            boolean defaultDoorwayClearance, @Nullable ResourceLocation doorBlock, int doorWidth, int doorHeight,
            int doorDepth) {
        this.cost = cost;
        this.limit = limit;
        this.rules = rules;
        this.defaultDoorwayClearance = defaultDoorwayClearance;
        this.doorBlock = doorBlock;
        this.doorWidth = doorWidth;
        this.doorHeight = doorHeight;
        this.doorDepth = doorDepth;
    }

    /**
     * How one panel face expands. {@code mode} is "STRUCTURE" (the default) or "SHELL".
     * When {@code checkBlock} is set and found at the check position, the room behind the panel
     * already exists, so only the door is opened: free of charge and not counted towards the limit.
     * The check position is the explicit check offset if any axis is given, else one block through the panel.
     */
    private record ExpansionRule(ResourceLocation structure, ScalingValue offsetX, ScalingValue offsetY,
            ScalingValue offsetZ, String mode, @Nullable ResourceLocation shellBlock, int shellRadius,
            int shellHeight, @Nullable ResourceLocation checkBlock, @Nullable Integer checkX,
            @Nullable Integer checkY, @Nullable Integer checkZ) {

        boolean isShell() {
            return "SHELL".equalsIgnoreCase(mode);
        }

        BlockPos checkPos(BlockPos panelPos, Direction facing) {
            if (checkX == null && checkY == null && checkZ == null) {
                return panelPos.relative(facing.getOpposite(), 1);
            }
            return panelPos.offset(checkX != null ? checkX : 0, checkY != null ? checkY : 0,
                    checkZ != null ? checkZ : 0);
        }
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player.level().isClientSide() || interactPos == null) {
            return true;
        }
        ServerLevel level = (ServerLevel) player.level();

        BlockState panel = level.getBlockState(interactPos);
        Direction facing;
        if (panel.hasProperty(BlockStateProperties.FACING)) {
            facing = panel.getValue(BlockStateProperties.FACING);
        } else if (panel.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = panel.getValue(BlockStateProperties.HORIZONTAL_FACING);
        } else {
            CreRaces.LOGGER.warn("ExpandPocketAction: Block at {} does not have FACING property.", interactPos);
            return false;
        }

        ExpansionRule rule = rules.get(facing);
        if (rule == null) {
            CreRaces.LOGGER.warn("ExpandPocketAction: No expansion rule defined for face {}.", facing);
            showNotSupported(player);
            return false;
        }
        if (doorBlock == null) {
            CreRaces.LOGGER.warn("ExpandPocketAction: No door_block configured; expansion cannot proceed.");
            showNotSupported(player);
            return false;
        }

        DataUtils.getVariables(player).ifPresentOrElse(
                vars -> expand(level, player, target, slot, interactPos, facing, rule, vars),
                () -> showNotSupported(player));
        return true;
    }

    private void expand(ServerLevel level, Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            BlockPos panelPos, Direction facing, ExpansionRule rule, IPlayerVariables vars) {
        double currentCost = cost.evaluate(player, target, slot);
        int maxLimit = (int) Math.round(limit.evaluate(player, target, slot));

        // Expansions must stay inside this player's own pocket plot.
        double dx = panelPos.getX() - vars.getPocketX();
        double dz = panelPos.getZ() - vars.getPocketZ();
        double boundary = CreRacesConfig.POCKET_BOUNDARY.get();
        if (dx * dx + dz * dz > boundary * boundary) {
            playNote(level, panelPos, SoundEvents.NOTE_BLOCK_BASS);
            player.displayClientMessage(Component.translatable("msg.creraces.pocket.boundary"), true);
            return;
        }

        BlockState door = BuiltInRegistries.BLOCK.get(doorBlock).defaultBlockState();
        if (roomAlreadyExists(level, panelPos, facing, rule)) {
            clearDoorway(level, panelPos, door, facing);
            playNote(level, panelPos, SoundEvents.NOTE_BLOCK_CHIME);
            player.displayClientMessage(Component.translatable("msg.creraces.expand_pocket.existing"), true);
            return;
        }

        if (vars.getPocketSize() >= maxLimit) {
            playNote(level, panelPos, SoundEvents.NOTE_BLOCK_BASS);
            player.displayClientMessage(Component.translatable("msg.creraces.expand_pocket.max_limit",
                    (int) vars.getPocketSize(), maxLimit), true);
            return;
        }
        if (vars.getCoins() < currentCost) {
            player.displayClientMessage(Component.translatable("msg.creraces.expand_pocket.no_coins",
                    (int) vars.getCoins(), (int) currentCost), false);
            return;
        }

        boolean built = rule.isShell()
                ? buildShell(level, player, panelPos, facing, rule)
                : placeStructure(level, player, target, slot, panelPos, facing, rule, door);
        if (!built) {
            return;
        }

        playNote(level, panelPos, SoundEvents.NOTE_BLOCK_CHIME);
        vars.setCoins(vars.getCoins() - currentCost);
        vars.setPocketSize(vars.getPocketSize() + 1);
        player.displayClientMessage(Component.translatable("msg.creraces.expand_pocket.remaining",
                (int) vars.getPocketSize(), maxLimit), true);
    }

    private static boolean roomAlreadyExists(ServerLevel level, BlockPos panelPos, Direction facing,
            ExpansionRule rule) {
        if (rule.isShell() || rule.checkBlock() == null) {
            return false;
        }
        Block expected = BuiltInRegistries.BLOCK.get(rule.checkBlock());
        return level.getBlockState(rule.checkPos(panelPos, facing)).getBlock() == expected;
    }

    /** Fills the faces of a box around the panel with the rule's shell block. */
    private static boolean buildShell(ServerLevel level, Player player, BlockPos panelPos, Direction facing,
            ExpansionRule rule) {
        if (rule.shellBlock() == null) {
            CreRaces.LOGGER.error("ExpandPocketAction: No shell_block configured for face {}.", facing);
            showNotSupported(player);
            return false;
        }
        if (!BuiltInRegistries.BLOCK.containsKey(rule.shellBlock())) {
            CreRaces.LOGGER.error("ExpandPocketAction: Shell block not found: {}", rule.shellBlock());
            return false;
        }
        BlockState shell = BuiltInRegistries.BLOCK.get(rule.shellBlock()).defaultBlockState();

        int r = rule.shellRadius();
        BlockPos corner1 = panelPos.offset(r, rule.shellHeight() - 2, r);
        BlockPos corner2 = panelPos.offset(-r, 5 - rule.shellHeight(), -r);
        for (BlockPos pos : BlockPos.betweenClosed(corner1, corner2)) {
            boolean onFace = pos.getX() == corner1.getX() || pos.getX() == corner2.getX()
                    || pos.getY() == corner1.getY() || pos.getY() == corner2.getY()
                    || pos.getZ() == corner1.getZ() || pos.getZ() == corner2.getZ();
            if (onFace) {
                level.setBlock(pos, shell, Block.UPDATE_ALL);
            }
        }
        return true;
    }

    private boolean placeStructure(ServerLevel level, Player player, @Nullable LivingEntity target,
            @Nullable AbilitySlot slot, BlockPos panelPos, Direction facing, ExpansionRule rule, BlockState door) {
        BlockPos origin = panelPos.offset(
                (int) Math.round(rule.offsetX().evaluate(player, target, slot)),
                (int) Math.round(rule.offsetY().evaluate(player, target, slot)),
                (int) Math.round(rule.offsetZ().evaluate(player, target, slot)));

        // The doorway is cleared twice: once to open the panel, and again after placement because
        // the new structure's own wall covers the opening.
        clearDoorway(level, panelPos, door, facing);
        StructureTemplate template = level.getStructureManager().getOrCreate(rule.structure());
        if (template == null) {
            CreRaces.LOGGER.error("ExpandPocketAction: Structure not found: {}", rule.structure());
            return false;
        }
        template.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.random, Block.UPDATE_ALL);
        clearDoorway(level, panelPos, door, facing);
        return true;
    }

    private void clearDoorway(ServerLevel level, BlockPos panelPos, BlockState door, Direction facing) {
        if (defaultDoorwayClearance) {
            WorldUtils.removeDoor(level, panelPos, door, facing, doorWidth, doorHeight, doorDepth);
        }
    }

    private static void playNote(ServerLevel level, BlockPos pos, Holder<SoundEvent> sound) {
        level.playSound(null, pos, sound.value(), SoundSource.NEUTRAL, 1.0f, 1.0f);
    }

    private static void showNotSupported(Player player) {
        player.displayClientMessage(Component.translatable("msg.creraces.expand_pocket.not_supported"), true);
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "expand_pocket"), json -> {
            String doorBlock = GsonHelper.getNullableString(json, "door_block", null);

            Map<Direction, ExpansionRule> rules = new HashMap<>();
            if (json.has("faces") && json.get("faces").isJsonObject()) {
                JsonObject faces = json.getAsJsonObject("faces");
                for (Direction dir : Direction.values()) {
                    String key = dir.getName().toLowerCase();
                    if (faces.has(key)) {
                        rules.put(dir, parseRule(faces.getAsJsonObject(key)));
                    }
                }
            } else {
                // Older flat format: a single structure rule shared by every horizontal face.
                ExpansionRule shared = new ExpansionRule(
                        new ResourceLocation(GsonHelper.getAsString(json, "structure", "creraces:pocket")),
                        ScalingValue.fromJson(json, "offset_x", 0.0),
                        ScalingValue.fromJson(json, "offset_y", 0.0),
                        ScalingValue.fromJson(json, "offset_z", 0.0),
                        "STRUCTURE", new ResourceLocation("minecraft:air"), 0, 0, null, null, null, null);
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    rules.put(dir, shared);
                }
            }

            return new ExpandPocketAction(
                    ScalingValue.fromJson(json, "cost", 200.0),
                    ScalingValue.fromJson(json, "limit", 9.0),
                    rules,
                    GsonHelper.getAsBoolean(json, "doorway_clearance", true),
                    doorBlock != null ? new ResourceLocation(doorBlock) : null,
                    GsonHelper.getAsInt(json, "door_width", 3),
                    GsonHelper.getAsInt(json, "door_height", 3),
                    GsonHelper.getAsInt(json, "door_depth", 2));
        });
    }

    private static ExpansionRule parseRule(JsonObject face) {
        String shellBlock = GsonHelper.getNullableString(face, "shell_block", null);
        String checkBlock = GsonHelper.getNullableString(face, "check_block", null);
        return new ExpansionRule(
                new ResourceLocation(GsonHelper.getAsString(face, "structure", "creraces:pocket")),
                ScalingValue.fromJson(face, "offset_x", 0.0),
                ScalingValue.fromJson(face, "offset_y", 0.0),
                ScalingValue.fromJson(face, "offset_z", 0.0),
                GsonHelper.getAsString(face, "mode", "STRUCTURE"),
                shellBlock != null ? new ResourceLocation(shellBlock) : null,
                GsonHelper.getAsInt(face, "shell_radius", 14),
                GsonHelper.getAsInt(face, "shell_height", 7),
                checkBlock != null ? new ResourceLocation(checkBlock) : null,
                face.has("check_x") ? face.get("check_x").getAsInt() : null,
                face.has("check_y") ? face.get("check_y").getAsInt() : null,
                face.has("check_z") ? face.get("check_z").getAsInt() : null);
    }
}
