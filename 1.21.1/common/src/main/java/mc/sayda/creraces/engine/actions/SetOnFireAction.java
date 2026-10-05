package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/** Sets the target (or the caster when there is none) on fire for a number of seconds. */
public class SetOnFireAction implements ActionRegistry.RaceAction {
    private final ScalingValue duration;

    public SetOnFireAction(ScalingValue duration) {
        this.duration = duration;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity victim = target != null ? target : player;
        int seconds = (int) duration.evaluate(player, victim, slot);
        if (seconds > 0) {
            victim.igniteForSeconds(seconds);
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "set_on_fire"),
                json -> new SetOnFireAction(ScalingValue.fromJson(json, "duration", 5.0)));
    }
}
