package mc.sayda.creraces.effect;

import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

/** Damages once a second while the target is in water, scaled by the source's Ability Power. */
public class BoilingEffect extends MobEffect implements SourceTrackedEffect {
    private static final float BASE_DAMAGE = 1.0f;

    public BoilingEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF4500);
    }

    @Override
    public void applyEffectTick(@Nonnull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide() || !entity.isInWaterOrBubble())
            return;

        Entity source = SourcedEffectDamage.findSource(entity);
        float damage = BASE_DAMAGE
                + (float) (SourcedEffectDamage.abilityPowerOf(source) * CreRacesConfig.BOILING_SCALING.get());
        SourcedEffectDamage.hurt(entity, "boiling", source, damage);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
