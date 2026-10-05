package mc.sayda.creraces.effect;

import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

/** Stacking bleed: damages once a second, scaled by the source's Ability Power and by the amplifier. */
public class BleedingEffect extends MobEffect implements SourceTrackedEffect {
    private static final float BASE_DAMAGE = 0.5f;

    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0xCC0000);
    }

    @Override
    public void applyEffectTick(@Nonnull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide())
            return;

        Entity source = SourcedEffectDamage.findSource(entity);
        float damage = BASE_DAMAGE
                + (float) (SourcedEffectDamage.abilityPowerOf(source) * CreRacesConfig.BLEEDING_SCALING.get());
        SourcedEffectDamage.hurt(entity, "bleeding", source, damage * (1.0f + amplifier));
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
