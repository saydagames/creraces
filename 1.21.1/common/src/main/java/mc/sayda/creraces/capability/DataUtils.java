package mc.sayda.creraces.capability;

import net.minecraft.world.entity.player.Player;
import java.util.Optional;

/**
 * Utility class for interacting with player variable data.
 */
public class DataUtils {

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
        if (!mc.sayda.creraces.config.CreRacesConfig.MINI_BUILD_REQUIRES_LEARNED.get())
            return true;

        return getVariables(player).map(vars -> {
            return vars.isAbilityUnlocked(net.minecraft.resources.ResourceLocation.parse("creraces:mini_build"));
        }).orElse(false);
    }

    /**
     * Robustly load a UUID from NBT, handling both modern INT[] and legacy/incorrect STRING formats.
     */
    public static java.util.UUID loadUUID(net.minecraft.nbt.CompoundTag nbt, String key) {
        if (!nbt.contains(key)) return null;
        
        // Check tag type (8 = String, 11 = IntArray)
        byte type = nbt.getTagType(key);
        if (type == 8) { // String
            try {
                return java.util.UUID.fromString(nbt.getString(key));
            } catch (Exception ignored) {
                return null;
            }
        } else if (type == 11) { // IntArray (Standard UUID storage)
            return nbt.getUUID(key);
        }
        
        return null;
    }
}
