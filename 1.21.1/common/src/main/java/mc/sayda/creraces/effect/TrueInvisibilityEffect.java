package mc.sayda.creraces.effect;

import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;

/**
 * Full invisibility (hidden model and name tag, zero mob detection; see ModMobEffects.isInvisible).
 * Players pay for it with mana every tick and drop out of it when they run dry.
 */
public class TrueInvisibilityEffect extends MobEffect {
    private static final double MANA_PER_TICK = 2.0;

    public TrueInvisibilityEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(@Nonnull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide() || !(entity instanceof Player player)) return true;

        DataUtils.getVariables(player).ifPresent(vars -> {
            double mana = vars.getMana();
            if (mana < MANA_PER_TICK) {
                // wrapAsHolder resolves to whichever effect this instance is registered as
                // (true invisibility, or camouflage for the subclass).
                player.removeEffect(entity.level().registryAccess()
                        .registryOrThrow(Registries.MOB_EFFECT).wrapAsHolder(this));
            } else {
                vars.setMana(mana - MANA_PER_TICK);
            }
        });
        return true;
    }
}
