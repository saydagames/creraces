package mc.sayda.creraces.ability;

import net.minecraft.server.level.ServerPlayer;

/**
 * Logic for executing an ability.
 */
@FunctionalInterface
public interface AbilityExecutor {
    /**
     * Executes the ability logic on the server.
     * 
     * @param player  The player casting the ability.
     * @param ability The ability being cast.
     * @param slot    The slot containing the ability.
     * @return true if the ability was successfully executed, false if cancelled.
     */
    boolean execute(ServerPlayer player, Ability ability, AbilitySlot slot);
}
