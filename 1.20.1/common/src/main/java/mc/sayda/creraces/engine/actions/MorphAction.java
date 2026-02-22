package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * Action to morph the player into an entity using Twilight Lib.
 */
public class MorphAction implements ActionRegistry.RaceAction {

    private final String entityType; // null to clear morph
    private final double scale;

    public MorphAction(String entityType, double scale) {
        this.entityType = entityType;
        this.scale = scale;
    }

    @Override
    public void execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            // Get Twilight Lib morph data
            mc.sayda.twilight_lib.capabilities.IMorph morphData = mc.sayda.twilight_lib.capabilities.DataUtils
                    .getMorphData(player);

            if (entityType == null || entityType.isEmpty()) {
                // Clear morph
                vars.setMorphed(false);
                morphData.setEntityType(Optional.empty());

                // Sync to Twilight Lib
                mc.sayda.twilight_lib.network.NetworkHandler.sendMorphToAll(
                        mc.sayda.twilight_lib.network.SyncMorphPacket.of(
                                player.getUUID(), Optional.empty()));

                // Reset scale
                if (player.getServer() != null && player instanceof ServerPlayer) {
                    player.getServer().getCommands().performPrefixedCommand(
                            player.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                            "scale reset @s");
                }
            } else {
                // Apply morph
                ResourceLocation entityId = new ResourceLocation(entityType);
                vars.setMorphed(true);
                morphData.setEntityType(Optional.of(entityId));

                // Sync to Twilight Lib
                mc.sayda.twilight_lib.network.NetworkHandler.sendMorphToAll(
                        mc.sayda.twilight_lib.network.SyncMorphPacket.of(
                                player.getUUID(), Optional.of(entityId)));

                // Apply scale if specified
                if (scale > 0 && scale != 1.0 && player.getServer() != null && player instanceof ServerPlayer) {
                    player.getServer().getCommands().performPrefixedCommand(
                            player.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                            "scale set pehkui:base " + scale + " @s");
                }
            }

            // Sync variables
            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player);
        });
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "morph"), json -> {
            String entityType = GsonHelper.getAsString(json, "entity_type", null);
            double scale = GsonHelper.getAsDouble(json, "scale", 1.0);
            return new MorphAction(entityType, scale);
        });
    }
}
