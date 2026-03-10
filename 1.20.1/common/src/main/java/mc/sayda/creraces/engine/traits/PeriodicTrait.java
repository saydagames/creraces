package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import java.util.Optional;

/**
 * Base class for traits that execute actions periodically.
 * Handles state management via IPlayerVariables to prevent memory leaks and
 * multi-player collisions.
 */
public abstract class PeriodicTrait implements TraitRegistry.RaceTrait {
    protected final ScalingValue interval;
    protected final ResourceLocation traitId;

    public PeriodicTrait(ResourceLocation traitId, ScalingValue interval) {
        this.traitId = traitId;
        this.interval = interval;
    }

    @Override
    public void tick(Player player) {
        if (player.level().isClientSide())
            return;

        Optional<IPlayerVariables> varsOpt = DataUtils.getVariables(player);
        if (varsOpt.isEmpty())
            return;
        IPlayerVariables vars = varsOpt.get();

        int currentTimer = vars.getTraitTimers().getOrDefault(traitId, 0);

        if (currentTimer <= 0) {
            if (shouldExecute(player, vars)) {
                execute(player, vars);
                // Reset timer based on current scaling
                int intVal = (int) interval.evaluate(player, null);
                vars.setTraitTimer(traitId, Math.max(1, intVal));
            }
        } else {
            vars.setTraitTimer(traitId, currentTimer - 1);
        }
    }

    /**
     * @return true if the trait should attempt to execute this tick (if the timer
     *         is 0).
     */
    protected abstract boolean shouldExecute(Player player, IPlayerVariables vars);

    /**
     * The logic to run when the interval completes.
     */
    protected abstract void execute(Player player, IPlayerVariables vars);
}
