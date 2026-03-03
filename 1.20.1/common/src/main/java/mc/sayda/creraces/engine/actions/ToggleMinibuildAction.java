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
            @Nullable BlockPos interactionPos) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            vars.setSmallBuild(!vars.isSmallBuild());

            // Sync variables to client
            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player);

            // Optional: Message to player
            if (player.level().isClientSide) {
                // Feedback is usually handled client-side anyway if triggered by UI,
                // but for abilities we might want a message or sound.
            }
        });
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "toggle_minibuild"),
                json -> new ToggleMinibuildAction());
    }
}
