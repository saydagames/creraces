package mc.sayda.creraces.global;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.util.CombatUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entry points from the vanilla/Architectury events IncidentResolver already listens to.
 * Permissive by design: nothing thrown out of a handler reaches the caller.
 */
public final class GlobalDispatcher {
    private static final int MAX_DEPTH = 8;
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final Set<String> LOGGED_FAILURES = new HashSet<>();

    private GlobalDispatcher() {
    }

    public static void onLivingDeath(LivingEntity victim, @Nullable DamageSource source) {
        if (victim.level().isClientSide() || GlobalRegistry.isEmpty()) {
            return;
        }
        Player killer = CombatUtils.getRootOwner(source != null ? source.getEntity() : null);
        dispatch(GlobalTrigger.ON_ENTITY_DEATH, victim, killer);
        if (victim instanceof Player) {
            dispatch(GlobalTrigger.ON_PLAYER_DEATH, victim, killer);
        }
    }

    public static void onLivingHurt(LivingEntity victim, @Nullable DamageSource source) {
        if (victim.level().isClientSide() || GlobalRegistry.isEmpty()) {
            return;
        }
        Player attacker = CombatUtils.getRootOwner(source != null ? source.getEntity() : null);
        dispatch(GlobalTrigger.ON_ENTITY_HURT, victim, attacker);
    }

    public static void onEntitySpawn(Entity entity) {
        if (entity.level().isClientSide() || GlobalRegistry.isEmpty()) {
            return;
        }
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        dispatch(GlobalTrigger.ON_ENTITY_SPAWN, living, null);
    }

    public static void onWorldTick(MinecraftServer server) {
        if (GlobalRegistry.isEmpty() || GlobalRegistry.byTrigger(GlobalTrigger.ON_WORLD_TICK).isEmpty()) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            dispatch(GlobalTrigger.ON_WORLD_TICK, player, null);
        }
    }

    private static void dispatch(GlobalTrigger trigger, LivingEntity subject, @Nullable Player killer) {
        List<GlobalHandler> handlers = GlobalRegistry.byTrigger(trigger);
        // EntityEvent.ADD fires for every entity on every chunk load, so bail before the
        // depth bookkeeping when nothing listens for this trigger.
        if (handlers.isEmpty()) {
            return;
        }

        int depth = DEPTH.get();
        if (depth >= MAX_DEPTH) {
            CreRaces.LOGGER.warn("GlobalDispatcher: recursion depth limit reached for {}, aborting", trigger.id());
            return;
        }
        DEPTH.set(depth + 1);
        try {
            for (GlobalHandler handler : handlers) {
                handler.run(trigger, subject, killer);
            }
        } finally {
            DEPTH.set(depth);
        }
    }

    static void reportFailure(ResourceLocation handlerId, GlobalTrigger trigger, String actionName, Exception e) {
        String signature = handlerId + "|" + trigger.id() + "|" + actionName + "|" + e.getClass().getSimpleName();
        if (LOGGED_FAILURES.add(signature)) {
            CreRaces.LOGGER.error("[globals/{}] {}: action {} failed - {}", handlerId, trigger.id(), actionName,
                    e.getMessage(), e);
        }
    }

    public static void clearFailureLog() {
        LOGGED_FAILURES.clear();
    }
}
