package mc.sayda.creraces.engine;

import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-side state for channelled actions: one active channel per player, ticked from the server loop. */
public class ChannelingManager {

    public static final class DuringEffect {
        public final MobEffect effect;
        public final int amplifier;
        public final int duration;

        public DuringEffect(MobEffect effect, int amplifier, int duration) {
            this.effect = effect;
            this.amplifier = amplifier;
            this.duration = duration;
        }
    }

    static final class ActiveChannel {
        final List<ActionRegistry.RaceAction> onComplete;
        final List<ActionRegistry.RaceAction> onPanicComplete;
        final List<ActionRegistry.RaceAction> onInterrupt;
        final List<ActionRegistry.RaceAction> duringActions;
        final boolean cancelableByDamage;
        final boolean cancelableByMovement;
        final boolean allowPanicCast;
        final List<DuringEffect> duringEffects;
        final LivingEntity target;
        final AbilitySlot slot;
        final BlockPos interactPos;
        int ticksRemaining;
        Vec3 lastPos;

        ActiveChannel(int duration, boolean cancelableByDamage, boolean cancelableByMovement,
                boolean allowPanicCast, List<DuringEffect> duringEffects,
                List<ActionRegistry.RaceAction> onComplete,
                List<ActionRegistry.RaceAction> onPanicComplete,
                List<ActionRegistry.RaceAction> onInterrupt,
                List<ActionRegistry.RaceAction> duringActions,
                Vec3 startPos, LivingEntity target, AbilitySlot slot, BlockPos interactPos) {
            this.ticksRemaining = duration;
            this.cancelableByDamage = cancelableByDamage;
            this.cancelableByMovement = cancelableByMovement;
            this.allowPanicCast = allowPanicCast;
            this.duringEffects = duringEffects;
            this.onComplete = onComplete;
            // Fall back to on_complete if no separate on_panic_complete list was provided
            this.onPanicComplete = onPanicComplete.isEmpty() ? onComplete : onPanicComplete;
            this.onInterrupt = onInterrupt;
            this.duringActions = duringActions;
            this.target = target;
            this.slot = slot;
            this.interactPos = interactPos;
            this.lastPos = startPos;
        }
    }

    private static final Map<UUID, ActiveChannel> CHANNELS = new ConcurrentHashMap<>();
    // Moving more than 0.15 blocks within a single tick interrupts a movement-cancelable channel.
    private static final double MAX_MOVE_PER_TICK_SQ = 0.15 * 0.15;

    public static boolean isChanneling(UUID id) {
        return CHANNELS.containsKey(id);
    }

    public static void start(Player player, int duration, boolean cancelableByDamage,
            boolean cancelableByMovement, boolean allowPanicCast,
            List<DuringEffect> duringEffects,
            List<ActionRegistry.RaceAction> onComplete,
            List<ActionRegistry.RaceAction> onPanicComplete,
            List<ActionRegistry.RaceAction> onInterrupt,
            List<ActionRegistry.RaceAction> duringActions,
            LivingEntity target, AbilitySlot slot, BlockPos interactPos) {
        CHANNELS.put(player.getUUID(), new ActiveChannel(
                duration, cancelableByDamage, cancelableByMovement, allowPanicCast,
                duringEffects, onComplete, onPanicComplete, onInterrupt, duringActions,
                player.position(), target, slot, interactPos));
    }

    public static void panicCast(ServerPlayer player) {
        ActiveChannel ch = CHANNELS.remove(player.getUUID());
        if (ch == null) return;
        DataUtils.getVariables(player).ifPresent(vars -> vars.setMana(0));
        run(ch.onPanicComplete, player, ch);
    }

    public static void onDamage(Player player) {
        ActiveChannel ch = CHANNELS.get(player.getUUID());
        if (ch == null || !ch.cancelableByDamage) return;
        CHANNELS.remove(player.getUUID());
        if (player instanceof ServerPlayer sp) {
            run(ch.onInterrupt, sp, ch);
        }
    }

    public static void clear(Player player) {
        CHANNELS.remove(player.getUUID());
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, ActiveChannel>> it = CHANNELS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ActiveChannel> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }

            ActiveChannel ch = entry.getValue();

            if (ch.cancelableByMovement) {
                Vec3 pos = player.position();
                if (pos.distanceToSqr(ch.lastPos) > MAX_MOVE_PER_TICK_SQ) {
                    it.remove();
                    run(ch.onInterrupt, player, ch);
                    continue;
                }
                ch.lastPos = pos;
            }

            for (DuringEffect de : ch.duringEffects) {
                if (de.effect != null) {
                    player.addEffect(new MobEffectInstance(de.effect, de.duration, de.amplifier, false, true, false));
                }
            }

            run(ch.duringActions, player, ch);

            ch.ticksRemaining--;
            if (ch.ticksRemaining <= 0) {
                it.remove();
                run(ch.onComplete, player, ch);
            }
        }
    }

    private static void run(List<ActionRegistry.RaceAction> actions, ServerPlayer player, ActiveChannel ch) {
        ActionRegistry.runChain(actions, player, ch.target, ch.slot, ch.interactPos);
    }
}
