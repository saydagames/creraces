package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ChannelingManager;
import mc.sayda.creraces.engine.EventContext;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Channels for "duration" ticks, running "actions" every "interval" ticks (default 1), then "on_complete".
 * "cancelable_by_damage" / "cancelable_by_movement" make damage or moving run "on_interrupt" instead.
 * Casting again while channeling runs "on_already_channeling", or with "panic_cast" finishes the
 * channel early with "on_panic_complete" (on_complete when that is empty).
 */
public class ChannelAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = CreRaces.id("channel");

    private final ScalingValue duration;
    private final boolean cancelableByDamage;
    private final boolean cancelableByMovement;
    private final boolean allowPanicCast;
    private final List<ActionRegistry.RaceAction> onComplete;
    private final List<ActionRegistry.RaceAction> onPanicComplete;
    private final List<ActionRegistry.RaceAction> onInterrupt;
    private final List<ActionRegistry.RaceAction> actions;
    private final ScalingValue interval;
    private final List<ActionRegistry.RaceAction> onAlreadyChanneling;

    public ChannelAction(ScalingValue duration, boolean cancelableByDamage, boolean cancelableByMovement,
            boolean allowPanicCast, List<ActionRegistry.RaceAction> onComplete,
            List<ActionRegistry.RaceAction> onPanicComplete, List<ActionRegistry.RaceAction> onInterrupt,
            List<ActionRegistry.RaceAction> actions, ScalingValue interval,
            List<ActionRegistry.RaceAction> onAlreadyChanneling) {
        this.duration = duration;
        this.cancelableByDamage = cancelableByDamage;
        this.cancelableByMovement = cancelableByMovement;
        this.allowPanicCast = allowPanicCast;
        this.onComplete = onComplete;
        this.onPanicComplete = onPanicComplete;
        this.onInterrupt = onInterrupt;
        this.actions = actions;
        this.interval = interval;
        this.onAlreadyChanneling = onAlreadyChanneling;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        if (ChannelingManager.isChanneling(player.getUUID())) {
            if (allowPanicCast) {
                ChannelingManager.panicCast(serverPlayer);
            } else {
                ActionRegistry.runChain(onAlreadyChanneling, player, target, slot, interactPos);
            }
            return false;
        }
        ChannelingManager.start(player, (int) duration.evaluate(player, target, slot, interactPos),
                new ChannelingManager.Channel(onComplete, onPanicComplete, onInterrupt, actions,
                        (int) interval.evaluate(player, target, slot, interactPos),
                        cancelableByDamage, cancelableByMovement, target, slot, interactPos, EventContext.current()));
        return true;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> new ChannelAction(
                ScalingValue.fromJson(json, "duration", 60.0),
                GsonHelper.getAsBoolean(json, "cancelable_by_damage", false),
                GsonHelper.getAsBoolean(json, "cancelable_by_movement", false),
                GsonHelper.getAsBoolean(json, "panic_cast", false),
                ActionRegistry.listFromJson(json, "on_complete"),
                ActionRegistry.listFromJson(json, "on_panic_complete"),
                ActionRegistry.listFromJson(json, "on_interrupt"),
                ActionRegistry.listFromJson(json, "actions"),
                ScalingValue.fromJson(json, "interval", 1.0),
                ActionRegistry.listFromJson(json, "on_already_channeling")));
    }
}
