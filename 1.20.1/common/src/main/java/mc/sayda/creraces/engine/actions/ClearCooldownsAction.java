package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Action that clears all ability cooldowns for the player.
 */
public class ClearCooldownsAction implements ActionRegistry.RaceAction {

    @Override
    public boolean execute(Player player, @Nullable net.minecraft.world.entity.LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @Nullable net.minecraft.core.BlockPos interactionPos) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            vars.getCooldowns().clear();
            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player);
        });
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "clear_cooldowns"),
                json -> new ClearCooldownsAction());
    }
}
