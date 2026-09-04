package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class TrueInvisibilityEffect extends MobEffect {
    public TrueInvisibilityEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(@javax.annotation.Nonnull net.minecraft.world.entity.LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide() || !(entity instanceof net.minecraft.world.entity.player.Player player)) return true;

        mc.sayda.creraces.capability.DataUtils.getVariables(player).ifPresent(vars -> {
            // Drains mana so this isn't a free permanent buff.
            double drain = 2.0;
            double current = vars.getMana();

            if (current < drain) {
                // wrapAsHolder resolves to whichever concrete effect "this" actually is
                // (TrueInvisibilityEffect or a subclass like CamouflageEffect).
                player.removeEffect(entity.level().registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.MOB_EFFECT).wrapAsHolder(this));
            } else {
                vars.setMana(current - drain);
            }
        });
        return true;
    }
}
