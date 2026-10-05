package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Toggles the caster's small-build mode. In a blacklisted dimension it can only be switched off.
 * The state is mirrored into the "minibuild" entity data key.
 */
public class ToggleMinibuildAction implements ActionRegistry.RaceAction {

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            String dimension = player.level().dimension().location().toString();
            if (CreRacesConfig.MINI_BUILD_DIMENSION_BLACKLIST.get().contains(dimension)) {
                if (vars.isSmallBuild()) {
                    vars.setSmallBuild(false);
                    BoundaryHandler.resyncForAllTrackers(player);
                }
                return;
            }

            boolean enabled = !vars.isSmallBuild();
            vars.setSmallBuild(enabled);
            if (player instanceof IPersistentDataAccessor accessor) {
                accessor.creraces$getPersistentData().putInt("minibuild", enabled ? 1 : 0);
            }
            BoundaryHandler.resyncForAllTrackers(player);
            CreRaces.LOGGER.debug("ToggleMinibuildAction: smallBuild for {} is now {}", player.getName().getString(),
                    enabled);
        });
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "toggle_minibuild"),
                json -> new ToggleMinibuildAction());
    }
}
