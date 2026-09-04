package mc.sayda.creraces.util;

import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.UUID;

public class CombatUtils {
    /**
     * Resolves the "root" player owner of an entity.
     * Handles:
     * 1. If entity is a player, returns the player.
     * 2. If entity is Ownable (e.g. Wolf), returns the owner if it's a player.
     * 3. If entity is a CreRaces servant, returns the owner from NBT.
     */
    @Nullable
    public static Player getRootOwner(@Nullable Entity entity) {
        return getRootOwner(entity, 0);
    }

    @Nullable
    private static Player getRootOwner(@Nullable Entity entity, int depth) {
        if (entity == null || depth > 8) return null;

        if (entity instanceof Player player) {
            return player;
        }

        if (entity instanceof OwnableEntity ownable) {
            Entity owner = ownable.getOwner();
            if (owner instanceof Player player) {
                return player;
            }
            // Recurse in case of nested owners (e.g. projectile shot by a tame)
            if (owner != null && owner != entity) {
                return getRootOwner(owner, depth + 1);
            }
        }

        if (entity instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof Player player) {
                return player;
            }
            // Recurse for projectiles shot by tames/servants
            if (owner != null && owner != entity) {
                return getRootOwner(owner, depth + 1);
            }
        }

        if (entity instanceof LivingEntity le && entity instanceof IPersistentDataAccessor accessor) {
            if (accessor.creraces$getPersistentData().contains("creraces:servant_of")) {
                try {
                    UUID ownerUuid = DataUtils.loadUUID(accessor.creraces$getPersistentData(), "creraces:servant_of");
                    if (ownerUuid != null) {
                        return le.level().getPlayerByUUID(ownerUuid);
                    }
                } catch (Exception ignored) {}
            }
        }

        return null;
    }
}
