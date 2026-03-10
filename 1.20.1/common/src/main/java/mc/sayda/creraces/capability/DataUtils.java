package mc.sayda.creraces.capability;

import net.minecraft.world.entity.player.Player;
import mc.sayda.creraces.registry.ModAttributes;
import java.util.Optional;

/**
 * Utility class for interacting with player variable data.
 */
public class DataUtils {

    /**
     * Obtains the player variables.
     * Forge/Fabric specific implementations will be injected here.
     */
    public static Optional<IPlayerVariables> getVariables(Player player) {
        if (player instanceof IPlayerVariables vars) {
            return Optional.of(vars);
        }
        return Optional.empty();
    }

    /**
     * Sakuya's Time Leap: Advances the state of player cooldowns.
     * Should be called every tick on the server.
     */
    public static void performSakuyaTimeLeap(Player player) {
        getVariables(player).ifPresent(IPlayerVariables::sakuyaTimeLeap);
    }

    /**
     * Eiki's Judgment: Updates karma based on player actions.
     */
    public static void applyEikiJudgment(Player player, double delta) {
        getVariables(player).ifPresent(vars -> {
            vars.setKarma(vars.getKarma() + delta);
        });
    }

    /**
     * Reimu's Fantasy Seal: Fully resets the player's race-related data.
     */
    public static void performFantasySeal(Player player) {
        mc.sayda.creraces.race.CosmeticIncidents.clearAllRacialAddons(player);
        getVariables(player).ifPresent(IPlayerVariables::fantasySealReset);
    }

    /**
     * Checks if the player can interact with the mini-build system.
     */
    public static boolean canInteractWithMiniBuild(Player player) {
        if (!mc.sayda.creraces.config.CreRacesConfig.MINIBUILD_REQUIRES_LEARNED.get())
            return true;

        return getVariables(player).map(vars -> {
            return vars.isAbilityUnlocked(new net.minecraft.resources.ResourceLocation("creraces:mini_build"));
        }).orElse(false);
    }

    /**
     * Gets the player's Ability Power.
     * TEMPORARILY REVERTED TO AVOID TRANSFORMER ERROR
     */
    public static double getAbilityPower(Player player) {
        return player.getAttributeValue(ModAttributes.ABILITY_POWER.get());
    }

    /**
     * Gets the player's Attack Damage (bonus).
     */
    public static double getAttackDamage(Player player) {
        return player.getAttributeValue(ModAttributes.ATTACK_DAMAGE.get());
    }
}
