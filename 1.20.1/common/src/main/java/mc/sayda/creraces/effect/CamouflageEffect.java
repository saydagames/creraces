package mc.sayda.creraces.effect;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.util.WorldUtils;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;
import java.util.Objects;

/** True invisibility plus a speed boost. Aquatic races lose it as soon as they are out of water and rain. */
public class CamouflageEffect extends TrueInvisibilityEffect {
    public CamouflageEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x1E511E);
        @SuppressWarnings("null")
        Attribute speedAttr = Objects.requireNonNull(Attributes.MOVEMENT_SPEED);
        // +20% speed compensates for the reduced threat detection while camouflaged
        this.addAttributeModifier(speedAttr, "6b6e7061-0016-4680-b3be-c6a4c37a0265", 0.2D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(@Nonnull LivingEntity entity, int amplifier) {
        super.applyEffectTick(entity, amplifier);
        if (entity.level().isClientSide() || !(entity instanceof Player player)) return;

        DataUtils.getVariables(player).ifPresent(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race != null && race.isAquatic() && !player.isInWater() && !WorldUtils.isExposedToRain(player)) {
                player.removeEffect(this);
            }
        });
    }
}
