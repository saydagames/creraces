package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/** Removes the target from the world without killing it. Players are never removed. */
public class RemoveEntityAction implements ActionRegistry.RaceAction {

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player.level().isClientSide()) {
            return true;
        }
        if (target == null || target instanceof Player) {
            return false;
        }
        target.discard();
        return true;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("remove_entity"), json -> new RemoveEntityAction());
    }
}
