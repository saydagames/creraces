package mc.sayda.creraces.engine;

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
        final mc.sayda.creraces.ability.AbilitySlot slot;
        final BlockPos interactPos;
        int ticksRemaining;
        Vec3 lastPos;

        ActiveChannel(int duration, boolean cancelableByDamage, boolean cancelableByMovement,
                boolean allowPanicCast, List<DuringEffect> duringEffects,
                List<ActionRegistry.RaceAction> onComplete,
                List<ActionRegistry.RaceAction> onPanicComplete,
                List<ActionRegistry.RaceAction> onInterrupt,
                List<ActionRegistry.RaceAction> duringActions,
                Vec3 startPos, LivingEntity target,
                mc.sayda.creraces.ability.AbilitySlot slot, BlockPos interactPos) {
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
    // 0.15 blocks of movement triggers cancel
    private static final double MOVEMENT_THRESHOLD_SQ = 0.0225;

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
            LivingEntity target, mc.sayda.creraces.ability.AbilitySlot slot, BlockPos interactPos) {
        CHANNELS.put(player.getUUID(), new ActiveChannel(
                duration, cancelableByDamage, cancelableByMovement, allowPanicCast,
                duringEffects, onComplete, onPanicComplete, onInterrupt, duringActions,
                player.position(), target, slot, interactPos));
    }

    public static void panicCast(ServerPlayer player) {
        ActiveChannel ch = CHANNELS.remove(player.getUUID());
        if (ch == null) return;
        DataUtils.getVariables(player).ifPresent(vars -> vars.setMana(0));
        run(ch.onPanicComplete, player, ch.target, ch.slot, ch.interactPos);
    }

    public static void onDamage(Player player) {
        ActiveChannel ch = CHANNELS.get(player.getUUID());
        if (ch == null || !ch.cancelableByDamage) return;
        CHANNELS.remove(player.getUUID());
        if (player instanceof ServerPlayer sp) {
            run(ch.onInterrupt, sp, ch.target, ch.slot, ch.interactPos);
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
                if (pos.distanceToSqr(ch.lastPos) > MOVEMENT_THRESHOLD_SQ) {
                    it.remove();
                    run(ch.onInterrupt, player, ch.target, ch.slot, ch.interactPos);
                    continue;
                }
                ch.lastPos = pos;
            }

            for (DuringEffect de : ch.duringEffects) {
                if (de.effect != null) {
                    player.addEffect(new MobEffectInstance(de.effect, de.duration, de.amplifier, false, true, false));
                }
            }

            for (ActionRegistry.RaceAction action : ch.duringActions) {
                if (!action.execute(player, ch.target, ch.slot, ch.interactPos)) break;
            }

            ch.ticksRemaining--;
            if (ch.ticksRemaining <= 0) {
                it.remove();
                run(ch.onComplete, player, ch.target, ch.slot, ch.interactPos);
            }
        }
    }

    private static void run(List<ActionRegistry.RaceAction> actions, ServerPlayer player,
            LivingEntity target, mc.sayda.creraces.ability.AbilitySlot slot, BlockPos interactPos) {
        for (ActionRegistry.RaceAction action : actions) {
            if (!action.execute(player, target, slot, interactPos)) break;
        }
    }
}
