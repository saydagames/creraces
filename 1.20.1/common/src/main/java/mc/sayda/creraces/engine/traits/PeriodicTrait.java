package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * Base class for server-side traits that run every {@code interval} ticks. The countdown lives in
 * the player's trait timers under the trait id, so every player has their own.
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
            }
            // Reset the timer regardless of whether execution happened so that
            // shouldExecute is only called every interval ticks, not every tick.
            int intVal = (int) interval.evaluate(player, null);
            vars.setTraitTimer(traitId, Math.max(0, intVal - 1));
        } else {
            vars.setTraitTimer(traitId, currentTimer - 1);
        }
    }

    /** Checked each time the timer runs out; returning false skips this cycle. */
    protected abstract boolean shouldExecute(Player player, IPlayerVariables vars);

    protected abstract void execute(Player player, IPlayerVariables vars);
}
