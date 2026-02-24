package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class PocketEntryAction implements ActionRegistry.RaceAction {
    private static final ResourceLocation POCKET_DIM = new ResourceLocation(CreRaces.MODID, "pocket");
    private static final ResourceLocation POCKET_STRUCTURE = new ResourceLocation(CreRaces.MODID, "dryad_box_1");

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return true;

        ServerLevel pocketWorld = serverPlayer.server.getLevel(ResourceKey.create(Registries.DIMENSION, POCKET_DIM));
        if (pocketWorld == null) {
            CreRaces.LOGGER.error("Could not find pocket dimension: {}", POCKET_DIM);
            return true;
        }

        DataUtils.getVariables(serverPlayer).ifPresent(vars -> {
            if (serverPlayer.level().dimension().location().equals(POCKET_DIM)) {
                // Return to Overworld
                ResourceLocation returnDim = new ResourceLocation(vars.getReturnDim());
                ServerLevel overworld = serverPlayer.server
                        .getLevel(ResourceKey.create(Registries.DIMENSION, returnDim));
                if (overworld != null) {
                    serverPlayer.teleportTo(overworld, vars.getReturnX(), vars.getReturnY(), vars.getReturnZ(),
                            serverPlayer.getYRot(), serverPlayer.getXRot());
                } else {
                    // Fallback to Overworld spawn if return dim is invalid
                    ServerLevel ow = serverPlayer.server.overworld();
                    BlockPos spawn = ow.getSharedSpawnPos();
                    serverPlayer.teleportTo(ow, spawn.getX(), spawn.getY(), spawn.getZ(), 0, 0);
                }
                return;
            }

            // Enter Pocket
            boolean firstTime = !vars.hasPocket();

            // Store return point
            vars.setReturnX(serverPlayer.getX());
            vars.setReturnY(serverPlayer.getY());
            vars.setReturnZ(serverPlayer.getZ());
            vars.setReturnDim(serverPlayer.level().dimension().location().toString());

            double tx = vars.getPocketX();
            double ty = vars.getPocketY();
            double tz = vars.getPocketZ();

            // Default fallback if not set
            if (tx == 0 && ty == 0 && tz == 0) {
                tx = 1000.0 * (serverPlayer.getId() % 100); // Larger offset to avoid collisions
                ty = 100.0;
                tz = 1000.0 * (serverPlayer.getId() / 100);
                vars.setPocketX(tx);
                vars.setPocketY(ty);
                vars.setPocketZ(tz);
            }

            if (firstTime) {
                final double finalTx = tx;
                final double finalTy = ty;
                final double finalTz = tz;

                StructureTemplate template = pocketWorld.getStructureManager().getOrCreate(POCKET_STRUCTURE);
                if (template != null) {
                    template.placeInWorld(pocketWorld,
                            BlockPos.containing(finalTx, finalTy, finalTz),
                            BlockPos.containing(finalTx, finalTy, finalTz),
                            new StructurePlaceSettings(), pocketWorld.random, 3);
                } else {
                    CreRaces.LOGGER.warn("Pocket structure not found: {}", POCKET_STRUCTURE);
                }
                vars.setHasPocket(true);
            }

            // Teleport to the pocket spawn point
            serverPlayer.teleportTo(pocketWorld, tx + 4.5, ty + 1.0, tz + 4.5, 0, 0);
        });
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation("creraces:enter_pocket"), json -> new PocketEntryAction());
    }
}
