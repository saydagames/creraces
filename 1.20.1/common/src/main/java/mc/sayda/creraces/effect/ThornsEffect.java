package mc.sayda.creraces.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * THORNS - Retaliates a portion of damage received back at the attacker.
 * The actual retaliation logic fires from LivingEntityMixin.hurt() after the
 * damage is applied. This class just holds category/colour and provides the
 * retaliation formula.
 *
 * Retaliation = (baseDamage × 0.25) × (1 + amplifier)
 */
public class ThornsEffect extends MobEffect {

    public ThornsEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x006400);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 8 == 0;
    }

    @Override
    public void applyEffectTick(@javax.annotation.Nonnull LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        double x = entity.getX();
        double y = entity.getY() + entity.getBbHeight() * 0.5;
        double z = entity.getZ();

        serverLevel.sendParticles(java.util.Objects.requireNonNull(ParticleTypes.HAPPY_VILLAGER),    x, y,             z, 3, 1.0, 1.2, 1.0, 0.02);
        serverLevel.sendParticles(java.util.Objects.requireNonNull(ParticleTypes.COMPOSTER),         x, entity.getY(), z, 2, 0.7, 0.35, 0.7, 0.01);
        serverLevel.sendParticles(java.util.Objects.requireNonNull(ParticleTypes.SPORE_BLOSSOM_AIR), x, y + 0.4,      z, 4, 0.9, 0.8, 0.9, 0.01);
    }

    /**
     * Called from damage event handling. Returns the amount of damage to reflect
     * back, 0 if the entity doesn't have the effect.
     */
    public static float getRetaliationDamage(LivingEntity entity, float damageAmount) {
        var effect = entity
                .getEffect(java.util.Objects.requireNonNull(mc.sayda.creraces.registry.ModMobEffects.THORNS.get()));
        if (effect == null)
            return 0f;
        int amp = effect.getAmplifier(); // 0-based
        return damageAmount * 0.25f * (1 + amp);
    }

    /**
     * Convenience: apply retaliation damage to the attacker if the defender has
     * THORNS.
     */
    public static void applyRetaliation(LivingEntity defender, LivingEntity attacker, float originalDamage) {
        float reflect = getRetaliationDamage(defender, originalDamage);
        if (reflect > 0) {
            net.minecraft.world.damagesource.DamageSource src = java.util.Objects
                    .requireNonNull(defender.level().damageSources().thorns(defender));
            attacker.hurt(src, reflect);
        }
    }
}
