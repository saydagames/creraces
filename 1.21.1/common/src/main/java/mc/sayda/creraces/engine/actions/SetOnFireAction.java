package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class SetOnFireAction implements ActionRegistry.RaceAction {
    private final ScalingValue duration;

    public SetOnFireAction(ScalingValue duration) {
        this.duration = duration;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @Nullable net.minecraft.core.BlockPos interact_pos) {
        LivingEntity actualTarget = target != null ? target : player;
        int seconds = (int) duration.evaluate(player, actualTarget, slot);
        if (seconds > 0) {
            actualTarget.igniteForSeconds(seconds);
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "set_on_fire"), json -> {
            ScalingValue duration = ScalingValue.fromJson(json, "duration", 5.0);
            return new SetOnFireAction(duration);
        });
    }
}
