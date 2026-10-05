package mc.sayda.creraces.effect;

import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

/** Damages every half second, credited to the source; each amplifier level adds the configured scaling. */
public class RatVenomEffect extends MobEffect implements SourceTrackedEffect {
    private static final float BASE_DAMAGE = 0.2f;

    public RatVenomEffect() {
        super(MobEffectCategory.HARMFUL, 0x55FF55);
    }

    @Override
    public void applyEffectTick(@Nonnull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide())
            return;

        float scaling = CreRacesConfig.RAT_VENOM_SCALING.get().floatValue();
        SourcedEffectDamage.hurt(entity, "ratvenom", SourcedEffectDamage.findSource(entity),
                BASE_DAMAGE + amplifier * scaling);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 10 == 0;
    }
}
