package mc.sayda.creraces.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Thorny aura. LivingEntityMixin hits each attacker back for {@link #retaliationDamage}; this class
 * holds that formula and draws the particles.
 */
public class ThornsEffect extends MobEffect {

    public ThornsEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x006400);
    }

    /** A quarter of the damage taken per level: 25% at level I, 50% at level II, and so on. */
    public static float retaliationDamage(float damageTaken, int amplifier) {
        return damageTaken * 0.25f * (1 + amplifier);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 8 == 0;
    }

    @Override
    public boolean applyEffectTick(@Nonnull LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return true;
        double x = entity.getX();
        double y = entity.getY() + entity.getBbHeight() * 0.5;
        double z = entity.getZ();

        serverLevel.sendParticles(Objects.requireNonNull(ParticleTypes.HAPPY_VILLAGER),    x, y,             z, 3, 1.0, 1.2, 1.0, 0.02);
        serverLevel.sendParticles(Objects.requireNonNull(ParticleTypes.COMPOSTER),         x, entity.getY(), z, 2, 0.7, 0.35, 0.7, 0.01);
        serverLevel.sendParticles(Objects.requireNonNull(ParticleTypes.SPORE_BLOSSOM_AIR), x, y + 0.4,      z, 4, 0.9, 0.8, 0.9, 0.01);
        return true;
    }
}
