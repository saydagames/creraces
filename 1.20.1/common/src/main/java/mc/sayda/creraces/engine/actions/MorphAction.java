package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.race.CosmeticIncidents;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.network.NetworkHandler;
import mc.sayda.twilight_lib.network.SyncMorphPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Morphs the caster into an entity through Twilight Lib, optionally rescaling them with Pehkui.
 * Without an entity_type it clears the morph and resets the scale. The type may contain
 * customization placeholders.
 */
public class MorphAction implements ActionRegistry.RaceAction {

    @Nullable
    private final String entityType;
    private final ScalingValue scale;

    public MorphAction(@Nullable String entityType, ScalingValue scale) {
        this.entityType = entityType;
        this.scale = scale;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            IMorph morphData = mc.sayda.twilight_lib.capabilities.DataUtils.getMorphData(player);

            if (entityType == null || entityType.isEmpty()) {
                vars.setMorphed(false);
                morphData.setEntityType(Optional.empty());
                NetworkHandler.sendMorphToAll(SyncMorphPacket.of(player.getUUID(), Optional.empty()));
                runScaleCommand(player, "scale reset @s");
            } else {
                ResourceLocation entityId = new ResourceLocation(
                        CosmeticIncidents.resolvePlaceholders(entityType, vars.getCustomizations()));
                vars.setMorphed(true);
                morphData.setEntityType(Optional.of(entityId));
                NetworkHandler.sendMorphToAll(SyncMorphPacket.of(player.getUUID(), Optional.of(entityId)));

                double s = scale.evaluate(player, target, slot);
                if (s > 0 && s != 1.0) {
                    runScaleCommand(player, "scale set pehkui:base " + s + " @s");
                }
            }
            BoundaryHandler.resyncVariables(player, player);
        });
        return true;
    }

    /** Pehkui's scale API isn't directly accessible here, so it is driven through its command instead. */
    private static void runScaleCommand(Player player, String command) {
        if (player.getServer() != null && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getServer().getCommands().performPrefixedCommand(
                    serverPlayer.createCommandSourceStack().withPermission(4).withSuppressedOutput(), command);
        }
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "morph"), json -> new MorphAction(
                GsonHelper.getNullableString(json, "entity_type", null),
                ScalingValue.fromJson(json, "scale", 1.0)));
    }
}
