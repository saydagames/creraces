package mc.sayda.creraces.engine;

import mc.sayda.creraces.ability.AbilitySlot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs the channels started by the channel action: their actions every interval, then
 * on_complete, unless damage or movement interrupts them first.
 */
@SuppressWarnings("null")
public class ChannelingManager {

    /** Everything one running channel needs; the event it started from is kept for its later actions. */
    public record Channel(List<ActionRegistry.RaceAction> onComplete, List<ActionRegistry.RaceAction> onPanicComplete,
            List<ActionRegistry.RaceAction> onInterrupt, List<ActionRegistry.RaceAction> actions, int interval,
            boolean cancelableByDamage, boolean cancelableByMovement, @Nullable LivingEntity target,
            @Nullable AbilitySlot slot, @Nullable BlockPos interactPos, EventContext event) {
    }

    private static final class ActiveChannel {
        final Channel channel;
        int ticksRemaining;
        int ticksElapsed;
        Vec3 lastPos;

        ActiveChannel(Channel channel, int duration, Vec3 startPos) {
            this.channel = channel;
            this.ticksRemaining = duration;
            this.lastPos = startPos;
        }
    }

    private static final Map<UUID, ActiveChannel> CHANNELS = new ConcurrentHashMap<>();
    // Moving more than 0.15 blocks within a single tick interrupts a movement-cancelable channel.
    private static final double MAX_MOVE_PER_TICK_SQ = 0.15 * 0.15;

    public static boolean isChanneling(UUID id) {
        return CHANNELS.containsKey(id);
    }

    public static void start(Player player, int duration, Channel channel) {
        CHANNELS.put(player.getUUID(), new ActiveChannel(channel, duration, player.position()));
    }

    /** Finishes the channel at once with on_panic_complete (on_complete when that list is empty). */
    public static void panicCast(ServerPlayer player) {
        ActiveChannel active = CHANNELS.remove(player.getUUID());
        if (active != null) {
            List<ActionRegistry.RaceAction> actions = active.channel.onPanicComplete().isEmpty()
                    ? active.channel.onComplete() : active.channel.onPanicComplete();
            run(actions, player, active.channel);
        }
    }

    public static void onDamage(Player player) {
        ActiveChannel active = CHANNELS.get(player.getUUID());
        if (active == null || !active.channel.cancelableByDamage()) {
            return;
        }
        CHANNELS.remove(player.getUUID());
        if (player instanceof ServerPlayer serverPlayer) {
            run(active.channel.onInterrupt(), serverPlayer, active.channel);
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

            ActiveChannel active = entry.getValue();
            if (active.channel.cancelableByMovement()) {
                Vec3 pos = player.position();
                if (pos.distanceToSqr(active.lastPos) > MAX_MOVE_PER_TICK_SQ) {
                    it.remove();
                    run(active.channel.onInterrupt(), player, active.channel);
                    continue;
                }
                active.lastPos = pos;
            }

            int interval = active.channel.interval();
            if (interval > 0 && active.ticksElapsed % interval == 0) {
                run(active.channel.actions(), player, active.channel);
            }

            active.ticksElapsed++;
            active.ticksRemaining--;
            if (active.ticksRemaining <= 0) {
                it.remove();
                run(active.channel.onComplete(), player, active.channel);
            }
        }
    }

    private static void run(List<ActionRegistry.RaceAction> actions, ServerPlayer player, Channel channel) {
        channel.event().run(() -> ActionRegistry.runChain(actions, player, channel.target(), channel.slot(),
                channel.interactPos()));
    }
}
