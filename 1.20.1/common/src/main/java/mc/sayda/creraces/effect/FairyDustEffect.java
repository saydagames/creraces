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
            new ResourceLocation(CreRaces.MODID, "fairy_realm");

    public FairyDustEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xCD88D6);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (player.isInWater()) {
            player.removeEffect(java.util.Objects.requireNonNull(mc.sayda.creraces.registry.ModMobEffects.FAIRY_DUST_EFFECT.get()));
            return;
        }
        if (player.hasEffect(java.util.Objects.requireNonNull(mc.sayda.creraces.registry.ModMobEffects.SOGGY.get()))) {
            player.removeEffect(java.util.Objects.requireNonNull(mc.sayda.creraces.registry.ModMobEffects.FAIRY_DUST_EFFECT.get()));
            return;
        }
        if (player.level().dimension().location().equals(FAIRY_REALM)) return;
        try {
            virtuoel.pehkui.api.ScaleData data = virtuoel.pehkui.api.ScaleTypes.FLIGHT.getScaleData(player);
            data.setScale(2.0f);
            data.setTargetScale(2.0f);
        } catch (Throwable ignored) {}
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        if (!(entity instanceof ServerPlayer player)) return;
        try {
            virtuoel.pehkui.api.ScaleData data = virtuoel.pehkui.api.ScaleTypes.FLIGHT.getScaleData(player);
            data.setScale(1.0f);
            data.setTargetScale(1.0f);
        } catch (Throwable ignored) {}
    }
}
