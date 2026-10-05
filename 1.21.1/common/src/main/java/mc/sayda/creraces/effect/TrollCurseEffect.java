package mc.sayda.creraces.effect;

import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;

/**
 * Aura from the Troll Pillar. Troll players get a short Speed I pulse each tick; everyone else gets
 * Slowness I and Weakness I.
 */
public class TrollCurseEffect extends MobEffect {
    private static final ResourceLocation TROLL_RACE = ResourceLocation.fromNamespaceAndPath("creraces", "troll");

    public TrollCurseEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFF5500);
    }

    @Override
    public boolean applyEffectTick(@Nonnull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide())
            return true;

        boolean isTroll = entity instanceof Player player && DataUtils.getVariables(player)
                .map(vars -> TROLL_RACE.equals(vars.getRace()))
                .orElse(false);

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
