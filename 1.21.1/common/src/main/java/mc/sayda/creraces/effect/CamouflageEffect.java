package mc.sayda.creraces.effect;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.registry.ModMobEffects;
import mc.sayda.creraces.util.WorldUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;

/** True invisibility plus a speed boost. Aquatic races lose it as soon as they are out of water and rain. */
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
    public boolean applyEffectTick(@Nonnull LivingEntity entity, int amplifier) {
        super.applyEffectTick(entity, amplifier);
        if (entity.level().isClientSide() || !(entity instanceof Player player)) return true;

        DataUtils.getVariables(player).ifPresent(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race != null && race.isAquatic() && !player.isInWater() && !WorldUtils.isExposedToRain(player)) {
                player.removeEffect(ModMobEffects.CAMOUFLAGE);
            }
        });
        return true;
    }
}
