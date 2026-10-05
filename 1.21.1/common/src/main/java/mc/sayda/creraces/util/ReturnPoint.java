package mc.sayda.creraces.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Where a player stood before a teleport took them into another dimension, so a way out of that
 * dimension (the tree gateways) can put them back. It lives in the player's persistent data, which a
 * respawn does not carry over, so dying forgets it.
 */
public final class ReturnPoint {
    private static final String KEY = "creraces:return_point";

    private ReturnPoint() {}

    /** Remembers the player's current position as the way back out of {@code destination}. */
    public static void save(ServerPlayer player, ResourceKey<Level> destination) {
        CompoundTag point = new CompoundTag();
        point.putString("dimension", player.level().dimension().location().toString());
        point.putDouble("x", player.getX());
        point.putDouble("y", player.getY());
        point.putDouble("z", player.getZ());
        point.putFloat("yaw", player.getYRot());
        point.putFloat("pitch", player.getXRot());
        point.putString("destination", destination.location().toString());
        data(player).put(KEY, point);
    }

    /** Sends the player back to their saved point and forgets it. Returns false if there was none to use. */
    public static boolean sendBack(ServerPlayer player) {
        CompoundTag data = data(player);
        if (!data.contains(KEY, Tag.TAG_COMPOUND)) {
            return false;
        }
        CompoundTag point = data.getCompound(KEY);
        data.remove(KEY);
        ServerLevel level = levelOf(player, point.getString("dimension"));
        if (level == null) {
            return false;
        }
        player.teleportTo(level, point.getDouble("x"), point.getDouble("y"), point.getDouble("z"),
                point.getFloat("yaw"), point.getFloat("pitch"));
        return true;
    }

    /** Forgets the saved point once the player ends up anywhere other than the dimension it leads out of. */
    public static void forgetUnlessIn(ServerPlayer player, ResourceKey<Level> current) {
        CompoundTag data = data(player);
        if (data.contains(KEY, Tag.TAG_COMPOUND)
                && !data.getCompound(KEY).getString("destination").equals(current.location().toString())) {
            data.remove(KEY);
        }
    }

    private static CompoundTag data(ServerPlayer player) {
        return ((IPersistentDataAccessor) player).creraces$getPersistentData();
    }

    @Nullable
    private static ServerLevel levelOf(ServerPlayer player, String dimensionId) {
        ResourceLocation id = ResourceLocation.tryParse(dimensionId);
        return id == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }
}
