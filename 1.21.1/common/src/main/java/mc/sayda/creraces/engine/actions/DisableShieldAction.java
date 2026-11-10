package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.engine.ScalingValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;

/** Knocks a player's shield down for "duration" ticks, or for vanilla's axe-hit time when it is 0. */
@SuppressWarnings("null")
public class DisableShieldAction implements ActionRegistry.RaceAction {
    /** Entity event that plays the shield-break sound and particles, as vanilla's disableShield does. */
    private static final byte SHIELD_BREAK_EVENT = 30;

    private final ScalingValue duration;
    private final EntitySelector entity;

    public DisableShieldAction(ScalingValue duration, EntitySelector entity) {
        this.duration = duration;
        this.entity = entity;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!(entity.resolve(player, target) instanceof Player victim)) {
            return true;
        }
        int ticks = (int) duration.evaluate(player, target, slot, interactPos);
        if (ticks > 0) {
            victim.getCooldowns().addCooldown(Items.SHIELD, ticks);
            victim.stopUsingItem();
            victim.level().broadcastEntityEvent(victim, SHIELD_BREAK_EVENT);
        } else {
            victim.disableShield();
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("disable_shield"), json -> new DisableShieldAction(
                ScalingValue.fromJson(json, "duration", 60.0),
                EntitySelector.fromJson(json, EntitySelector.TARGET)));
    }
}
