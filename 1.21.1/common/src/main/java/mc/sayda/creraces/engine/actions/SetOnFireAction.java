package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.engine.ScalingValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/** Sets an entity (the target unless "entity" says "self") on fire for "duration" ticks. */
public class SetOnFireAction implements ActionRegistry.RaceAction {
    private final ScalingValue duration;
    private final EntitySelector entity;

    public SetOnFireAction(ScalingValue duration, EntitySelector entity) {
        this.duration = duration;
        this.entity = entity;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity victim = entity.resolve(player, target);
        if (victim == null) {
            return true;
        }
        // Vanilla's ignite keeps a fire that is already burning longer.
        victim.igniteForTicks((int) duration.evaluate(player, target, slot, interactPos));
        return true;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("set_on_fire"), json -> new SetOnFireAction(
                ScalingValue.fromJson(json, "duration", 100.0),
                EntitySelector.fromJson(json, EntitySelector.TARGET)));
    }
}
