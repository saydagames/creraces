package mc.sayda.creraces.effect;

import mc.sayda.creraces.registry.ModDamageTags;
import mc.sayda.creraces.registry.ModMobEffects;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * The tiered shields. A shield's level (amplifier + 1) is its HP, which soaks damage before armor:
 * SHIELD blocks everything, AP_SHIELD only magic damage and AD_SHIELD only physical damage.
 */
public class ShieldEffect extends SimpleEffect {
    public ShieldEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    /**
     * The damage left once the victim's shields have soaked what they can. LivingEntityMixin and
     * PlayerMixin call this as damage is applied, since Player replaces that step with its own.
     */
    public static float absorb(LivingEntity victim, DamageSource source, float amount) {
        if (amount <= 0 || victim.level().isClientSide())
            return amount;

        float damage = soak(victim, ModMobEffects.SHIELD, amount);
        if (source.is(ModDamageTags.IS_MAGIC)) {
            damage = soak(victim, ModMobEffects.AP_SHIELD, damage);
        }
        if (source.is(ModDamageTags.IS_PHYSICAL)) {
            damage = soak(victim, ModMobEffects.AD_SHIELD, damage);
        }
        return damage;
    }

    /** Soaks damage with one shield, which loses that much HP and breaks at 0. */
    private static float soak(LivingEntity victim, @Nullable Holder<MobEffect> shield, float damage) {
        if (damage <= 0 || shield == null)
            return damage;
        MobEffectInstance instance = victim.getEffect(shield);
        if (instance == null)
            return damage;

        float shieldHp = instance.getAmplifier() + 1.0F;
        int levelsLeft = (int) (shieldHp - damage);
        victim.removeEffect(shield);
        if (levelsLeft >= 1) {
            victim.addEffect(new MobEffectInstance(shield, instance.getDuration(), levelsLeft - 1,
                    instance.isAmbient(), instance.isVisible(), instance.showIcon()));
        }
        victim.level().playSound((Player) null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.ITEM_BREAK,
                SoundSource.NEUTRAL, 1.0F, levelsLeft >= 1 ? 0.8F : 0.5F);
        return Math.max(0.0F, damage - shieldHp);
    }
}
