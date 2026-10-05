package mc.sayda.creraces.util;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.item.CommandingStaffItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import javax.annotation.Nullable;
import java.util.UUID;

public class CombatUtils {
    /**
     * Resolves the player ultimately responsible for an entity: the entity itself if it is a
     * player, otherwise the owner of a tame, a projectile or a claimed servant, followed through
     * nested owners (e.g. an arrow shot by a tame wolf).
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

        if (entity instanceof Projectile projectile) {
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
            CompoundTag data = accessor.creraces$getPersistentData();
            if (data.contains(CommandingStaffItem.SERVANT_OF)) {
                try {
                    UUID ownerUuid = DataUtils.loadUUID(data, CommandingStaffItem.SERVANT_OF);
                    if (ownerUuid != null) {
                        return le.level().getPlayerByUUID(ownerUuid);
                    }
                } catch (IllegalArgumentException ignored) {
                    // Wrong-length UUID array: the servant just has no resolvable owner.
                }
            }
        }

        return null;
    }
}
