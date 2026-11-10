package mc.sayda.creraces.engine;

import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.util.DamageGuard;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Runs a launched projectile's "on_hit" actions when it hurts something, with the victim as the
 * target and the hit's damage as the event. Projectiles are held weakly, so one that despawns or
 * is unloaded simply drops its actions.
 */
public final class ProjectileHooks {
    private record Hook(List<ActionRegistry.RaceAction> actions, @Nullable AbilitySlot slot) {
    }

    private static final Map<Entity, Hook> HOOKS = Collections.synchronizedMap(new WeakHashMap<>());

    private ProjectileHooks() {
    }

    public static void attach(Projectile projectile, List<ActionRegistry.RaceAction> actions, @Nullable AbilitySlot slot) {
        if (!actions.isEmpty()) {
            HOOKS.put(projectile, new Hook(actions, slot));
        }
    }

    /** Called from the living-hurt event. */
    public static void onHurt(LivingEntity victim, DamageSource source, float amount) {
        Entity direct = source.getDirectEntity();
        Hook hook = direct != null ? HOOKS.get(direct) : null;
        if (hook == null || !(direct instanceof Projectile projectile) || !(projectile.getOwner() instanceof Player owner)
                || DamageGuard.isProcessing()) {
            return;
        }
        // Damage dealt by the actions themselves must not count as another hit.
        DamageGuard.setProcessing(true);
        try {
            EventContext.ofDamage(source, amount)
                    .run(() -> ActionRegistry.runChain(hook.actions(), owner, victim, hook.slot(), null));
        } finally {
            DamageGuard.setProcessing(false);
        }
    }
}
