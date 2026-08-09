package mc.sayda.creraces.effect;

import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class CamouflageEffect extends TrueInvisibilityEffect {
    public CamouflageEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x1E511E);
        @SuppressWarnings("null")
        net.minecraft.world.entity.ai.attributes.Attribute speedAttr = java.util.Objects.requireNonNull(Attributes.MOVEMENT_SPEED);
        // +20% speed compensates for the reduced threat detection while camouflaged
        this.addAttributeModifier(speedAttr, "6b6e7061-0016-4680-b3be-c6a4c37a0265", 0.2D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(@javax.annotation.Nonnull net.minecraft.world.entity.LivingEntity entity, int amplifier) {
        super.applyEffectTick(entity, amplifier);
        if (entity.level().isClientSide() || !(entity instanceof Player player)) return;

        DataUtils.getVariables(player).ifPresent(vars -> {
            ResourceLocation raceId = vars.getRace();
            mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(raceId);
            if (race != null && race.isAquatic() && !player.isInWater() && !mc.sayda.creraces.util.WorldUtils.isExposedToRain(player)) {
                player.removeEffect(this);
            }
        });
    }
}
