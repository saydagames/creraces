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

    public TeleportAction(ScalingValue x, ScalingValue y, ScalingValue z, ResourceLocation dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
    }

    @Override
    public boolean execute(@javax.annotation.Nonnull Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer))
            return true;

        double targetX = x.evaluate(player, target);
        double targetY = y.evaluate(player, target);
        double targetZ = z.evaluate(player, target);

        if (targetX == 0 && targetY == 0 && targetZ == 0)
            return true;

        if (dimension != null) {
            net.minecraft.server.level.ServerLevel targetLevel = serverPlayer.server
                    .getLevel(net.minecraft.resources.ResourceKey
                            .create(net.minecraft.core.registries.Registries.DIMENSION, dimension));
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
            ScalingValue x = ScalingValue.fromJson(json, "x", 0.0);
            ScalingValue y = ScalingValue.fromJson(json, "y", 0.0);
            ScalingValue z = ScalingValue.fromJson(json, "z", 0.0);
            ResourceLocation dimension = json.has("dimension")
                    ? new ResourceLocation(json.get("dimension").getAsString())
                    : null;
            return new TeleportAction(x, y, z, dimension);
        });
    }
}
