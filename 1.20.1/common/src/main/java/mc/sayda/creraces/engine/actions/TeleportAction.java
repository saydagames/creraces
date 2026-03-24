package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class TeleportAction implements ActionRegistry.RaceAction {
    private final ScalingValue x;
    private final ScalingValue y;
    private final ScalingValue z;
    private final ResourceLocation dimension;
    private final String dimensionKey; // Generic dimension resolution

    /**
     * True only when at least one coordinate was explicitly written in the JSON.
     * Prevents an unconfigured TeleportAction from silently warping the player to
     * (0,0,0).
     */
    private final boolean configured;

    public TeleportAction(ScalingValue x, ScalingValue y, ScalingValue z, ResourceLocation dimension,
            String dimensionKey, boolean configured) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.dimensionKey = dimensionKey;
        this.configured = configured;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer))
            return true;

        if (!configured)
            return true;

        double targetX = x.evaluate(player, target, slot);
        double targetY = y.evaluate(player, target, slot);
        double targetZ = z.evaluate(player, target, slot);

        ResourceLocation targetDim = dimension;
        if (dimensionKey != null) {
            String customDim = mc.sayda.creraces.capability.DataUtils.getVariables(serverPlayer)
                    .map(vars -> vars.getCustomization(dimensionKey)).orElse(null);
            if (customDim != null && !customDim.isEmpty()) {
                try {
                    targetDim = new ResourceLocation(customDim);
                } catch (Exception e) {
                    mc.sayda.creraces.CreRaces.LOGGER.error("TeleportAction: Invalid dimension in key {}: {}",
                            dimensionKey, customDim);
                }
            }
        }

        if (targetDim != null) {
            net.minecraft.server.level.ServerLevel targetLevel = serverPlayer.server
                    .getLevel(net.minecraft.resources.ResourceKey
                            .create(net.minecraft.core.registries.Registries.DIMENSION, targetDim));
            if (targetLevel != null) {
                serverPlayer.teleportTo(targetLevel, targetX, targetY, targetZ, serverPlayer.getYRot(),
                        serverPlayer.getXRot());
                return true;
            }
        }

        serverPlayer.teleportTo(targetX, targetY, targetZ);
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation("creraces:teleport"), json -> {
            boolean configured = json.has("x") || json.has("y") || json.has("z") || json.has("dimension")
                    || json.has("dimension_key");
            ScalingValue x = ScalingValue.fromJson(json, "x", 0.0);
            ScalingValue y = ScalingValue.fromJson(json, "y", 0.0);
            ScalingValue z = ScalingValue.fromJson(json, "z", 0.0);
            ResourceLocation dimension = json.has("dimension")
                    ? new ResourceLocation(json.get("dimension").getAsString())
                    : null;
            String dimensionKey = json.has("dimension_key") ? json.get("dimension_key").getAsString() : null;

            return new TeleportAction(x, y, z, dimension, dimensionKey, configured);
        });
    }
}
