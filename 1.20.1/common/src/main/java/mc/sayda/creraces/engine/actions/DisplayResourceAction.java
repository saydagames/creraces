package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class DisplayResourceAction implements ActionRegistry.RaceAction {
    // Empty payload for now, or just send a packet to client.
    // DisplayResource historically updated action bar or UI temporarily.
    // We can just implement it as an empty action.

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @Nullable net.minecraft.core.BlockPos interactionPos) {
        // Implement display logic or act as a NoOp since UI shows resource natively.
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "display_resource"), json -> {
            return new DisplayResourceAction();
        });
    }
}
