package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Deals damage over time to entities while they are in water.
 * Used by the Mermaid's Spicy Whirlpool ability.
 */
public class BoilingEffect extends SimpleEffect {
    public BoilingEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF4500);
    }

    @Override
    public void applyEffectTick(@javax.annotation.Nonnull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return;
        if (entity.isInWaterOrBubble()) {
            float damage = 1.0F; // Base damage (half heart)
            net.minecraft.world.entity.Entity source = null;
            
            // Try to find the source player and their AP
            if (entity instanceof mc.sayda.creraces.util.IPersistentDataAccessor accessor) {
                var data = accessor.creraces$getPersistentData();
                if (data != null && data.contains("creraces:source")) {
                    try {
                        String uuidStr = data.getString("creraces:source");
                        if (uuidStr != null && !uuidStr.isEmpty()) {
                            java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
                            if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                source = serverLevel.getEntity(java.util.Objects.requireNonNull(uuid));
                                if (source instanceof net.minecraft.world.entity.LivingEntity livingSource) {
                                    net.minecraft.world.entity.ai.attributes.Attribute apAttr = mc.sayda.creraces.registry.ModAttributes.resolve(mc.sayda.creraces.registry.ModAttributes.ABILITY_POWER);
                                    double ap = apAttr != null ? livingSource.getAttributeValue(apAttr) : 0.0;
                                    damage += (float) (ap * mc.sayda.creraces.config.CreRacesConfig.BOILING_SCALING.get());
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            var registry = entity.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
            var resKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, new net.minecraft.resources.ResourceLocation("creraces", "boiling"));
            var holder = registry.getHolder(java.util.Objects.requireNonNull(resKey)).orElse(null);

            if (holder != null) {
                net.minecraft.world.damagesource.DamageSource ds = new net.minecraft.world.damagesource.DamageSource(java.util.Objects.requireNonNull(holder), source, source);
                mc.sayda.creraces.util.DamageGuard.setProcessing(true);
                try {
                    entity.hurt(ds, damage);
                } finally {
                    mc.sayda.creraces.util.DamageGuard.setProcessing(false);
                }
            }
            
            // Client-side aesthetic particles
            if (entity.level().isClientSide) {
                for (int i = 0; i < 3; i++) {
                    entity.level().addParticle(net.minecraft.core.particles.ParticleTypes.BUBBLE, 
                        entity.getRandomX(0.5D), entity.getRandomY(), entity.getRandomZ(0.5D), 
                        0, 0.1, 0);
                }
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Run every 20 ticks (1 second)
        return duration % 20 == 0;
    }
}
