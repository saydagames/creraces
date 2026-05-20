package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Action that explicitly aborts the rest of the ability execution pipeline
 * gracefully.
 * Returning false from an executor suppresses resource costs and cooldowns from
 * applying.
 */
public class CancelAction implements ActionRegistry.RaceAction {

    @Override
    public boolean execute(Player player, @Nullable net.minecraft.world.entity.LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @Nullable net.minecraft.core.BlockPos interact_pos) {
        return false;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "cancel"),
                json -> new CancelAction());
    }
}
