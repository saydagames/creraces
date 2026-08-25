package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.PocketManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class PocketEntryAction implements ActionRegistry.RaceAction {
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
    @javax.annotation.Nullable
    private final mc.sayda.creraces.engine.condition.Condition condition;
    @javax.annotation.Nullable
    private final String blockedMessage;

    public PocketEntryAction(ResourceLocation dimension, ResourceLocation structure,
            ScalingValue spawnOffsetX, ScalingValue spawnOffsetY, ScalingValue spawnOffsetZ,
            ScalingValue structureOffsetX, ScalingValue structureOffsetY, ScalingValue structureOffsetZ,
            ScalingValue returnOffsetX, ScalingValue returnOffsetY, ScalingValue returnOffsetZ,
            @javax.annotation.Nullable mc.sayda.creraces.engine.condition.Condition condition,
            @javax.annotation.Nullable String blockedMessage) {
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
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return true;

        ServerLevel pocketWorld = serverPlayer.server.getLevel(ResourceKey.create(Registries.DIMENSION, java.util.Objects.requireNonNull(dimension)));
        if (pocketWorld == null) {
            CreRaces.LOGGER.error("Could not find pocket dimension: {}", dimension);
            return true;
        }

        if (structure == null) {
            CreRaces.LOGGER.error("Pocket structure is null!");
            return true;
        }

        DataUtils.getVariables(serverPlayer).ifPresent(vars -> {
            if (serverPlayer.level().dimension().location().equals(dimension)) {
                // Return to Entry Dimension
                double rx = vars.getReturnX() + returnOffsetX.evaluate(player, target, slot);
                double ry = vars.getReturnY() + returnOffsetY.evaluate(player, target, slot);
                double rz = vars.getReturnZ() + returnOffsetZ.evaluate(player, target, slot);
                teleport(serverPlayer, vars.getReturnDim(), rx, ry, rz);
                return;
            }

            // Entry restriction (race-agnostic; any race defines its own prerequisite via JSON)
            if (condition != null && !condition.evaluate(player, target, slot, interact_pos)) {
                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        blockedMessage != null ? blockedMessage : "msg.creraces.pocket.entry_blocked"), true);
                return;
            }

            // Enter Pocket
            boolean firstTime = !vars.hasPocket();

            // Store return point
            vars.setReturnX(serverPlayer.getX());
            vars.setReturnY(serverPlayer.getY());
            vars.setReturnZ(serverPlayer.getZ());
            vars.setReturnDim(java.util.Objects.requireNonNull(serverPlayer.level().dimension().location()).toString());

            double tx = vars.getPocketX();
            double ty = vars.getPocketY();
            double tz = vars.getPocketZ();

            if (firstTime) {
                // If this is a fresh acquisition (e.g. after race reset),
                // get a brand new index even if we had one before.
                vars.setPocketIndex(PocketManager.getNextIndex());

                int index = vars.getPocketIndex();
                tx = 1000 * (index % 1000);
                ty = 128;
                tz = 1000 * (index / 1000);
                vars.setPocketX(tx);
                vars.setPocketY(ty);
                vars.setPocketZ(tz);

                final double finalTx = tx;
                final double finalTy = ty;
                final double finalTz = tz;
                double sOffX = structureOffsetX.evaluate(player, target, slot);
                double sOffY = structureOffsetY.evaluate(player, target, slot);
                double sOffZ = structureOffsetZ.evaluate(player, target, slot);

                StructureTemplate template = pocketWorld.getStructureManager().getOrCreate(structure);
                if (template != null) {
                    template.placeInWorld(pocketWorld,
                            BlockPos.containing(finalTx + sOffX, finalTy + sOffY, finalTz + sOffZ),
                            BlockPos.containing(finalTx + sOffX, finalTy + sOffY, finalTz + sOffZ),
                            new StructurePlaceSettings(), pocketWorld.random, 3);
                    vars.setHasPocket(true);

                    // Initialize stable spawn point for this host's pocket
                    vars.setPocketSpawnX(tx + spawnOffsetX.evaluate(player, target, slot) + sOffX);
                    vars.setPocketSpawnY(ty + spawnOffsetY.evaluate(player, target, slot) + sOffY);
                    vars.setPocketSpawnZ(tz + spawnOffsetZ.evaluate(player, target, slot) + sOffZ);
                } else {
                    CreRaces.LOGGER.warn("Pocket structure not found: {}", structure);
                }
            }

            // Teleport to the saved stable spawn point
            serverPlayer.teleportTo(pocketWorld,
                    vars.getPocketSpawnX(),
                    vars.getPocketSpawnY(),
                    vars.getPocketSpawnZ(), 0, 0);
        });
        return true;
    }

    public static void teleport(ServerPlayer player, String dimension, double x, double y, double z) {
        String dimName = (dimension == null || dimension.isEmpty()) ? "minecraft:overworld" : dimension;
        ResourceLocation dimLoc = ResourceLocation.tryParse(dimName);
        if (dimLoc == null)
            dimLoc = new ResourceLocation("minecraft:overworld");
        
        ResourceKey<net.minecraft.world.level.Level> dimKey = ResourceKey
                .create(net.minecraft.core.registries.Registries.DIMENSION, dimLoc);
        ServerLevel level = player.server.getLevel(dimKey);
        if (level != null) {
            player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());
        } else {
            ServerLevel ow = player.server.overworld();
            BlockPos spawn = ow.getSharedSpawnPos();
            player.teleportTo(ow, spawn.getX(), spawn.getY(), spawn.getZ(), 0, 0);
        }
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation("creraces:enter_pocket"), json -> {
            ResourceLocation dimension = new ResourceLocation(
                    GsonHelper.getAsString(json, "dimension", "creraces:pocket"));
            ResourceLocation structure = new ResourceLocation(
                    GsonHelper.getAsString(json, "structure", "creraces:box"));
            ScalingValue spawnX = ScalingValue.fromJson(json, "spawn_x", 6);
            ScalingValue spawnY = ScalingValue.fromJson(json, "spawn_y", 2.0);
            ScalingValue spawnZ = ScalingValue.fromJson(json, "spawn_z", 6);
            ScalingValue structX = ScalingValue.fromJson(json, "structure_x", 0.0);
            ScalingValue structY = ScalingValue.fromJson(json, "structure_y", 0.0);
            ScalingValue structZ = ScalingValue.fromJson(json, "structure_z", 0.0);
            ScalingValue returnX = ScalingValue.fromJson(json, "return_offset_x", 0.0);
            ScalingValue returnY = ScalingValue.fromJson(json, "return_offset_y", 0.0);
            ScalingValue returnZ = ScalingValue.fromJson(json, "return_offset_z", 0.0);
            mc.sayda.creraces.engine.condition.Condition condition = json.has("condition")
                    ? mc.sayda.creraces.engine.condition.Condition.fromJson(json.getAsJsonObject("condition"))
                    : null;
            String blockedMessage = GsonHelper.getNullableString(json, "blocked_message", null);
            return new PocketEntryAction(dimension, structure, spawnX, spawnY, spawnZ, structX, structY, structZ,
                    returnX, returnY, returnZ, condition, blockedMessage);
        });
    }
}
