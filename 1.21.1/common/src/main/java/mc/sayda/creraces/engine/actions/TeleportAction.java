package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.ReturnPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Teleports the caster to x/y/z, optionally in another dimension given directly ("dimension") or read
 * from a customization variable ("dimension_key", which wins when set). With "save_return_point" it
 * also remembers where the caster stood, so a way out of the destination can send them back.
 */
public class TeleportAction implements ActionRegistry.RaceAction {
    private final ScalingValue x;
    private final ScalingValue y;
    private final ScalingValue z;
    @Nullable
    private final ResourceLocation dimension;
    @Nullable
    private final String dimensionKey;
    /** False when the JSON set no coordinate or dimension at all, so an empty action can't warp to (0, 0, 0). */
    private final boolean configured;
    private final boolean saveReturnPoint;

    public TeleportAction(ScalingValue x, ScalingValue y, ScalingValue z, @Nullable ResourceLocation dimension,
            @Nullable String dimensionKey, boolean configured, boolean saveReturnPoint) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.dimensionKey = dimensionKey;
        this.configured = configured;
        this.saveReturnPoint = saveReturnPoint;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!(player instanceof ServerPlayer serverPlayer) || !configured) {
            return true;
        }

        double targetX = x.evaluate(player, target, slot);
        double targetY = y.evaluate(player, target, slot);
        double targetZ = z.evaluate(player, target, slot);

        ResourceLocation targetDimension = dimension;
        if (dimensionKey != null) {
            String stored = DataUtils.getVariables(serverPlayer).map(vars -> vars.getCustomization(dimensionKey))
                    .orElse(null);
            if (stored != null && !stored.isEmpty()) {
                ResourceLocation parsed = ResourceLocation.tryParse(stored);
                if (parsed != null) {
                    targetDimension = parsed;
                } else {
                    CreRaces.LOGGER.error("TeleportAction: Invalid dimension in key {}: {}", dimensionKey, stored);
                }
            }
        }

        if (targetDimension == null) {
            if (saveReturnPoint) {
                ReturnPoint.save(serverPlayer, serverPlayer.level().dimension());
            }
            serverPlayer.teleportTo(targetX, targetY, targetZ);
            return true;
        }
        ServerLevel targetLevel = serverPlayer.server.getLevel(ResourceKey.create(Registries.DIMENSION,
                targetDimension));
        if (targetLevel == null) {
            return false;
        }
        if (saveReturnPoint) {
            ReturnPoint.save(serverPlayer, targetLevel.dimension());
        }
        serverPlayer.teleportTo(targetLevel, targetX, targetY, targetZ, serverPlayer.getYRot(),
                serverPlayer.getXRot());
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "teleport"), json -> {
            boolean configured = json.has("x") || json.has("y") || json.has("z") || json.has("dimension")
                    || json.has("dimension_key");
            String dimension = GsonHelper.getNullableString(json, "dimension", null);
            return new TeleportAction(
                    ScalingValue.fromJson(json, "x", 0.0),
                    ScalingValue.fromJson(json, "y", 0.0),
                    ScalingValue.fromJson(json, "z", 0.0),
                    dimension != null ? ResourceLocation.parse(dimension) : null,
                    GsonHelper.getNullableString(json, "dimension_key", null),
                    configured,
                    GsonHelper.getAsBoolean(json, "save_return_point", false));
        });
    }
}
