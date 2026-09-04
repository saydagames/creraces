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
        // +20% speed compensates for the reduced threat detection while camouflaged
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath("creraces", "camouflage_speed"),
                0.2D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(@javax.annotation.Nonnull net.minecraft.world.entity.LivingEntity entity, int amplifier) {
        super.applyEffectTick(entity, amplifier);
        if (entity.level().isClientSide() || !(entity instanceof Player player)) return true;

        DataUtils.getVariables(player).ifPresent(vars -> {
            ResourceLocation raceId = vars.getRace();
            mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(raceId);
            if (race != null && race.isAquatic() && !player.isInWater() && !mc.sayda.creraces.util.WorldUtils.isExposedToRain(player)) {
                player.removeEffect(mc.sayda.creraces.registry.ModMobEffects.CAMOUFLAGE);
            }
        });
        return true;
    }
}
