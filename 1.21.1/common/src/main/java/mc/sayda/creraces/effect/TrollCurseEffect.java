package mc.sayda.creraces.effect;

import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * Troll's Curse - applied by the Troll Pillar entity to nearby entities.
 * Troll players get a friendly Speed I pulse; everyone else gets Slowness I + Weakness I.
 */
public class TrollCurseEffect extends MobEffect {

    public TrollCurseEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFF5500); // dark orange
    }

    @Override
    public boolean applyEffectTick(@Nonnull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide())
            return true;

        boolean isTroll = false;
        if (entity instanceof Player player) {
            isTroll = DataUtils.getVariables(player)
                    .map(vars -> ResourceLocation.fromNamespaceAndPath("creraces", "troll").equals(vars.getRace()))
                    .orElse(false);
        }

        if (isTroll) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2, 0, false, false));
        } else {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2, 0, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2, 0, false, false));
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
