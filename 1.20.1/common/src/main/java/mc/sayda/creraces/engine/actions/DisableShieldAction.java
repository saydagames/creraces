package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class DisableShieldAction implements ActionRegistry.RaceAction {

    private final mc.sayda.creraces.engine.ScalingValue duration;

    public DisableShieldAction(mc.sayda.creraces.engine.ScalingValue duration) {
        this.duration = duration;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        if (target instanceof Player targetPlayer) {
            int ticks = (int) duration.evaluate(player, target);
            if (ticks > 0) {
                targetPlayer.getCooldowns().addCooldown(net.minecraft.world.item.Items.SHIELD, ticks);
                targetPlayer.stopUsingItem();
                targetPlayer.level().broadcastEntityEvent(targetPlayer, (byte) 30); // Shield break sound/particles
            } else {
                targetPlayer.disableShield(true);
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "disable_shield"),
                json -> {
                    mc.sayda.creraces.engine.ScalingValue duration = mc.sayda.creraces.engine.ScalingValue.fromJson(
                            json,
                            "duration", 0.0);
                    return new DisableShieldAction(duration);
                });
    }
}
