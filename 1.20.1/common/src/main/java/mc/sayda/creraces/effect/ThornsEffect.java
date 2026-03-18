package mc.sayda.creraces.effect;

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
        return false;
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
