package mc.sayda.creraces.effect;

import mc.sayda.creraces.CreRaces;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public class FairyDustEffect extends MobEffect {

    private static final ResourceLocation FAIRY_REALM =
            ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "fairy_realm");

    public FairyDustEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xCD88D6);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof ServerPlayer player)) return true;
        if (player.isInWater()) {
            player.removeEffect(mc.sayda.creraces.registry.ModMobEffects.FAIRY_DUST_EFFECT);
            return true;
        }
        if (player.hasEffect(mc.sayda.creraces.registry.ModMobEffects.SOGGY)) {
            player.removeEffect(mc.sayda.creraces.registry.ModMobEffects.FAIRY_DUST_EFFECT);
            return true;
        }
        if (player.level().dimension().location().equals(FAIRY_REALM)) return true;
        try {
            virtuoel.pehkui.api.ScaleData data = virtuoel.pehkui.api.ScaleTypes.FLIGHT.getScaleData(player);
            data.setScale(2.0f);
            data.setTargetScale(2.0f);
        } catch (NoClassDefFoundError ignored) {
        } catch (Exception e) { mc.sayda.creraces.CreRaces.LOGGER.debug("Pehkui scale error: {}", e.getMessage()); }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    // removeAttributeModifiers(AttributeMap) no longer receives the owning entity in 1.21+,
    // so the Pehkui flight-scale reset that used to run here has no entity to reset - it needs
    // a different hook (e.g. a tick-based check for the effect disappearing) to be restored.
}
