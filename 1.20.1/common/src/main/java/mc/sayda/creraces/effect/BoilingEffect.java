package mc.sayda.creraces.effect;

import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

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
            Entity source = null;

            // Try to find the source player and their AP
            if (entity instanceof IPersistentDataAccessor accessor) {
                var data = accessor.creraces$getPersistentData();
                if (data != null && data.contains("creraces:source")) {
                    try {
                        String uuidStr = data.getString("creraces:source");
                        if (uuidStr != null && !uuidStr.isEmpty()) {
                            java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
                            if (entity.level() instanceof ServerLevel serverLevel) {
                                source = serverLevel.getEntity(java.util.Objects.requireNonNull(uuid));
                                if (source instanceof LivingEntity livingSource) {
                                    Attribute apAttr = ModAttributes.resolve(ModAttributes.ABILITY_POWER);
                                    double ap = apAttr != null ? livingSource.getAttributeValue(apAttr) : 0.0;
                                    damage += (float) (ap * CreRacesConfig.BOILING_SCALING.get());
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            var registry = entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
            var resKey = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("creraces", "boiling"));
            var holder = registry.getHolder(java.util.Objects.requireNonNull(resKey)).orElse(null);

            if (holder != null) {
                DamageSource ds = new DamageSource(java.util.Objects.requireNonNull(holder), source, source);
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
                    entity.level().addParticle(ParticleTypes.BUBBLE,
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
