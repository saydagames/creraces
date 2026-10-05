package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;

/** Knocks a target player's shield down, for a custom number of ticks or vanilla's axe-hit cooldown. */
public class DisableShieldAction implements ActionRegistry.RaceAction {

    /** Entity event that plays the shield-break sound and particles, as vanilla's disableShield does. */
    private static final byte SHIELD_BREAK_EVENT = 30;

    private final ScalingValue duration;

    public DisableShieldAction(ScalingValue duration) {
        this.duration = duration;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (target instanceof Player targetPlayer) {
            int ticks = (int) duration.evaluate(player, target, slot);
            if (ticks > 0) {
                targetPlayer.getCooldowns().addCooldown(Items.SHIELD, ticks);
                targetPlayer.stopUsingItem();
                targetPlayer.level().broadcastEntityEvent(targetPlayer, SHIELD_BREAK_EVENT);
            } else {
                targetPlayer.disableShield(true);
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "disable_shield"),
                json -> new DisableShieldAction(ScalingValue.fromJson(json, "duration", 60.0)));
    }
}
