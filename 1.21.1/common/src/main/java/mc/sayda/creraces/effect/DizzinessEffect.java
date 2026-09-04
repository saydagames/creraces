package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Dizziness effect - ported from legacy CreRaces.
 * Causes the entity to move randomly in horizontal directions.
 */
public class DizzinessEffect extends MobEffect {
    public DizzinessEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide())
            return true;

        double randomX = (Math.random() - Math.random()) / 6.0;
        double randomZ = (Math.random() - Math.random()) / 6.0;

        Vec3 currentMovement = entity.getDeltaMovement();
        entity.setDeltaMovement(randomX, currentMovement.y, randomZ);
        entity.hurtMarked = true; // Force sync to client
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
