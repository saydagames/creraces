package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.engine.Position;
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
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Teleports an entity (the caster by default) to "position", optionally into another "dimension".
 * Only players change dimension. "save_return_point" remembers where a player left from.
 */
@SuppressWarnings("null")
public class TeleportAction implements ActionRegistry.RaceAction {
    private final Position position;
    @Nullable
    private final ResourceLocation dimension;
    private final boolean saveReturnPoint;
    private final EntitySelector entity;

    public TeleportAction(Position position, @Nullable ResourceLocation dimension, boolean saveReturnPoint,
            EntitySelector entity) {
        this.position = position;
        this.dimension = dimension;
        this.saveReturnPoint = saveReturnPoint;
        this.entity = entity;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity subject = entity.resolve(player, target);
        if (subject == null || !(subject.level() instanceof ServerLevel currentLevel)) {
            return true;
        }
        Vec3 destination = position.resolve(player, target, slot, interactPos);
        if (destination == null) {
            return false;
        }

        ServerLevel targetLevel = currentLevel;
        if (dimension != null) {
            targetLevel = currentLevel.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
            if (targetLevel == null) {
                CreRaces.LOGGER.error("TeleportAction: unknown dimension '{}'", dimension);
                return false;
            }
        }

        if (subject instanceof ServerPlayer serverPlayer) {
            if (saveReturnPoint) {
                ReturnPoint.save(serverPlayer, targetLevel.dimension());
            }
            serverPlayer.teleportTo(targetLevel, destination.x, destination.y, destination.z,
                    serverPlayer.getYRot(), serverPlayer.getXRot());
        } else if (targetLevel == currentLevel) {
            subject.teleportTo(destination.x, destination.y, destination.z);
        } else {
            return false;
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("teleport"), json -> {
            if (!json.has("position")) {
                throw new IllegalArgumentException("teleport needs a \"position\"");
            }
            String dimensionText = GsonHelper.getNullableString(json, "dimension", null);
            ResourceLocation dimension = dimensionText != null ? ResourceLocation.tryParse(dimensionText) : null;
            if (dimensionText != null && dimension == null) {
                throw new IllegalArgumentException("malformed dimension '" + dimensionText + "'");
            }
            return new TeleportAction(
                    Position.fromJson(json, "position", Position.of(Position.Source.SELF)),
                    dimension,
                    GsonHelper.getAsBoolean(json, "save_return_point", false),
                    EntitySelector.fromJson(json, EntitySelector.SELF));
        });
    }
}
