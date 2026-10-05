package mc.sayda.creraces.capability;

import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for interacting with player variable data.
 */
public class DataUtils {
    private static final ResourceLocation MINI_BUILD_ABILITY = new ResourceLocation("creraces", "mini_build");

    /**
     * Obtains the player variables via the IPlayerVariables mixin applied to Player.
     */
    public static Optional<IPlayerVariables> getVariables(Player player) {
        if (player instanceof IPlayerVariables vars) {
            return Optional.of(vars);
        }
        return Optional.empty();
    }

    /**
     * Checks if the player can interact with the mini-build system.
     */
    public static boolean canInteractWithMiniBuild(Player player) {
        if (!CreRacesConfig.MINI_BUILD_REQUIRES_LEARNED.get())
            return true;

        return getVariables(player).map(vars -> vars.isAbilityUnlocked(MINI_BUILD_ABILITY)).orElse(false);
    }

    /**
     * Reads a UUID stored either the standard way (int array) or, as some older data did, as a
     * string. Returns null if the key is missing or unreadable.
     */
    @Nullable
    public static UUID loadUUID(CompoundTag nbt, String key) {
        if (!nbt.contains(key))
            return null;

        byte type = nbt.getTagType(key);
        if (type == Tag.TAG_STRING) {
            try {
                return UUID.fromString(nbt.getString(key));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        } else if (type == Tag.TAG_INT_ARRAY) {
            return nbt.getUUID(key);
        }

        return null;
    }
}
