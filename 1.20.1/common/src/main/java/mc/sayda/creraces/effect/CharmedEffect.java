package mc.sayda.creraces.effect;

import mc.sayda.creraces.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import javax.annotation.Nullable;

/**
 * Keeps its target looking at whoever applied it, unable to hurt them or walk away from them. A
 * charmed mob walks up to the source and waits there; a player's view and movement are handled on
 * their own client by CharmClient. While the source is gone the effect does nothing.
 */
public class CharmedEffect extends MobEffect implements SourceTrackedEffect {
    /** Kept apart from the shared key, so a later bleed from someone else doesn't move the charm. */
    public static final String CHARM_SOURCE_KEY = "creraces:charm_source";
    private static final double FOLLOW_DISTANCE = 2.5;

    public CharmedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF69B4);
    }

    @Override
    public String sourceKey() {
        return CHARM_SOURCE_KEY;
    }

    public static boolean isCharmed(LivingEntity entity) {
        return entity.hasEffect(ModMobEffects.CHARMED.get());
    }

    /** Server only: whoever charmed the entity, or null if it isn't charmed or they are gone. */
    @Nullable
    private static Entity sourceOf(LivingEntity entity) {
        if (!isCharmed(entity)) {
            return null;
        }
        Entity source = SourcedEffectDamage.findSource(entity, CHARM_SOURCE_KEY);
        return source != entity && source != null && source.isAlive() ? source : null;
    }

    public static boolean isCharmedBy(LivingEntity entity, Entity source) {
        return source != null && sourceOf(entity) == source;
    }

    /** Runs after a mob's goals have picked their moves, so the charm overrides them. */
    public static void steer(Mob mob) {
        Entity source = sourceOf(mob);
        if (source == null) {
            return;
        }
        mob.getLookControl().setLookAt(source, 180.0F, 180.0F);
        if (mob.getTarget() == source) {
            mob.setTarget(null);
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
        if (mob.distanceToSqr(source) > FOLLOW_DISTANCE * FOLLOW_DISTANCE) {
            mob.getMoveControl().setWantedPosition(source.getX(), source.getY(), source.getZ(), 1.0);
        } else {
            mob.getNavigation().stop();
            mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0.0);
        }
    }
}
