package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import mc.sayda.creraces.ability.AbilitySlot;
import javax.annotation.Nullable;

/**
 * Action to toggle the player's small build mode.
 */
public class ToggleMinibuildAction implements ActionRegistry.RaceAction {

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interact_pos) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            net.minecraft.resources.ResourceLocation dim = player.level().dimension().location();
            boolean blacklisted = mc.sayda.creraces.config.CreRacesConfig.MINI_BUILD_DIMENSION_BLACKLIST
                    .get().contains(dim.toString());
            if (blacklisted) {
                if (vars.isSmallBuild()) {
                    vars.setSmallBuild(false);
                    mc.sayda.creraces.network.BoundaryHandler.resyncForAllTrackers(player);
                }
                return;
            }

            boolean newState = !vars.isSmallBuild();
            mc.sayda.creraces.CreRaces.LOGGER.info("ToggleMinibuildAction: Toggling smallBuild for {} to {}",
                    player.getName().getString(), newState);

            vars.setSmallBuild(newState);

            if (player instanceof mc.sayda.creraces.util.IPersistentDataAccessor accessor) {
                accessor.creraces$getPersistentData().putInt("minibuild", newState ? 1 : 0);
            }

            mc.sayda.creraces.CreRaces.LOGGER.info("ToggleMinibuildAction: [TRACE] Toggling smallBuild for {} to {}",
                    player.getName().getString(), newState);

            mc.sayda.creraces.network.BoundaryHandler.resyncForAllTrackers(player);

            mc.sayda.creraces.CreRaces.LOGGER.info("ToggleMinibuildAction: [TRACE] resyncForAllTrackers called for {}",
                    player.getName().getString());
        });
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "toggle_minibuild"),
                json -> new ToggleMinibuildAction());
    }
}
