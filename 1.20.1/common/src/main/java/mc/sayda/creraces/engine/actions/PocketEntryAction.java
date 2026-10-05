package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.PocketManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;

/**
 * Moves the caster into their pocket dimension plot, or back to where they came from when already
 * inside. The first entry claims a new plot and builds its starting structure.
 */
public class PocketEntryAction implements ActionRegistry.RaceAction {
    /** Plots are laid out on a grid this many blocks apart, this many plots per row, at this height. */
    private static final int PLOT_SPACING = 1000;
    private static final int PLOTS_PER_ROW = 1000;
    private static final int PLOT_Y = 128;

    private final ResourceLocation dimension;
    private final ResourceLocation structure;
    private final ScalingValue spawnOffsetX;
    private final ScalingValue spawnOffsetY;
    private final ScalingValue spawnOffsetZ;
    private final ScalingValue structureOffsetX;
    private final ScalingValue structureOffsetY;
    private final ScalingValue structureOffsetZ;
    private final ScalingValue returnOffsetX;
    private final ScalingValue returnOffsetY;
    private final ScalingValue returnOffsetZ;
    @Nullable
    private final Condition condition;
    @Nullable
    private final String blockedMessage;

    public PocketEntryAction(ResourceLocation dimension, ResourceLocation structure,
            ScalingValue spawnOffsetX, ScalingValue spawnOffsetY, ScalingValue spawnOffsetZ,
            ScalingValue structureOffsetX, ScalingValue structureOffsetY, ScalingValue structureOffsetZ,
            ScalingValue returnOffsetX, ScalingValue returnOffsetY, ScalingValue returnOffsetZ,
            @Nullable Condition condition, @Nullable String blockedMessage) {
        this.dimension = dimension;
        this.structure = structure;
        this.spawnOffsetX = spawnOffsetX;
        this.spawnOffsetY = spawnOffsetY;
        this.spawnOffsetZ = spawnOffsetZ;
        this.structureOffsetX = structureOffsetX;
        this.structureOffsetY = structureOffsetY;
        this.structureOffsetZ = structureOffsetZ;
        this.returnOffsetX = returnOffsetX;
        this.returnOffsetY = returnOffsetY;
        this.returnOffsetZ = returnOffsetZ;
        this.condition = condition;
        this.blockedMessage = blockedMessage;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        ServerLevel pocketWorld = serverPlayer.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (pocketWorld == null) {
            CreRaces.LOGGER.error("Could not find pocket dimension: {}", dimension);
            return true;
        }

        DataUtils.getVariables(serverPlayer).ifPresent(vars -> {
            if (serverPlayer.level().dimension().location().equals(dimension)) {
                double x = vars.getReturnX() + returnOffsetX.evaluate(player, target, slot);
                double y = vars.getReturnY() + returnOffsetY.evaluate(player, target, slot);
                double z = vars.getReturnZ() + returnOffsetZ.evaluate(player, target, slot);
                teleport(serverPlayer, vars.getReturnDim(), x, y, z);
                return;
            }

            // Any entry prerequisite comes from the JSON condition, not from race-specific code.
            if (condition != null && !condition.evaluate(player, target, slot, interactPos)) {
                serverPlayer.displayClientMessage(Component.translatable(
                        blockedMessage != null ? blockedMessage : "msg.creraces.pocket.entry_blocked"), true);
                return;
            }

            vars.setReturnX(serverPlayer.getX());
            vars.setReturnY(serverPlayer.getY());
            vars.setReturnZ(serverPlayer.getZ());
            vars.setReturnDim(serverPlayer.level().dimension().location().toString());

            if (!vars.hasPocket()) {
                createPocket(serverPlayer, pocketWorld, vars, target, slot);
            }
            serverPlayer.teleportTo(pocketWorld, vars.getPocketSpawnX(), vars.getPocketSpawnY(),
                    vars.getPocketSpawnZ(), 0, 0);
        });
        return true;
    }

    private void createPocket(ServerPlayer player, ServerLevel pocketWorld, IPlayerVariables vars,
            @Nullable LivingEntity target, @Nullable AbilitySlot slot) {
        // A fresh index every time, even for a player who had a pocket before a race reset.
        vars.setPocketIndex(PocketManager.getNextIndex());
        int index = vars.getPocketIndex();
        double x = PLOT_SPACING * (index % PLOTS_PER_ROW);
        double y = PLOT_Y;
        double z = PLOT_SPACING * (index / PLOTS_PER_ROW);
        vars.setPocketX(x);
        vars.setPocketY(y);
        vars.setPocketZ(z);

        double structureX = structureOffsetX.evaluate(player, target, slot);
        double structureY = structureOffsetY.evaluate(player, target, slot);
        double structureZ = structureOffsetZ.evaluate(player, target, slot);
        StructureTemplate template = pocketWorld.getStructureManager().getOrCreate(structure);
        if (template == null) {
            CreRaces.LOGGER.warn("Pocket structure not found: {}", structure);
            return;
        }
        BlockPos origin = BlockPos.containing(x + structureX, y + structureY, z + structureZ);
        template.placeInWorld(pocketWorld, origin, origin, new StructurePlaceSettings(), pocketWorld.random,
                Block.UPDATE_ALL);
        vars.setHasPocket(true);

        // The spawn point is fixed once, so later entries always land in the same place.
        vars.setPocketSpawnX(x + spawnOffsetX.evaluate(player, target, slot) + structureX);
        vars.setPocketSpawnY(y + spawnOffsetY.evaluate(player, target, slot) + structureY);
        vars.setPocketSpawnZ(z + spawnOffsetZ.evaluate(player, target, slot) + structureZ);
    }

    /** Teleports to a stored dimension, or to the overworld spawn when that dimension no longer exists. */
    private static void teleport(ServerPlayer player, String dimension, double x, double y, double z) {
        ResourceLocation dimensionId = ResourceLocation.tryParse(
                dimension == null || dimension.isEmpty() ? "minecraft:overworld" : dimension);
        ServerLevel level = dimensionId != null
                ? player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId))
                : player.server.getLevel(Level.OVERWORLD);
        if (level != null) {
            player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());
        } else {
            ServerLevel overworld = player.server.overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();
            player.teleportTo(overworld, spawn.getX(), spawn.getY(), spawn.getZ(), 0, 0);
        }
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "enter_pocket"), json -> new PocketEntryAction(
                new ResourceLocation(GsonHelper.getAsString(json, "dimension", "creraces:pocket")),
                new ResourceLocation(GsonHelper.getAsString(json, "structure", "creraces:box")),
                ScalingValue.fromJson(json, "spawn_x", 6),
                ScalingValue.fromJson(json, "spawn_y", 2.0),
                ScalingValue.fromJson(json, "spawn_z", 6),
                ScalingValue.fromJson(json, "structure_x", 0.0),
                ScalingValue.fromJson(json, "structure_y", 0.0),
                ScalingValue.fromJson(json, "structure_z", 0.0),
                ScalingValue.fromJson(json, "return_offset_x", 0.0),
                ScalingValue.fromJson(json, "return_offset_y", 0.0),
                ScalingValue.fromJson(json, "return_offset_z", 0.0),
                json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null,
                GsonHelper.getNullableString(json, "blocked_message", null)));
    }
}
