package mc.sayda.creraces.effect;

import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.Objects;

/**
 * Stacking bleed debuff. Deals damage over time scaled by the source's Ability Power.
 */
public class BleedingEffect extends SimpleEffect {
    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0xCC0000);
    }

    @Override
    public void applyEffectTick(@javax.annotation.Nonnull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide())
            return;

        float damage = 0.5f; // Base damage (quarter heart)
        net.minecraft.world.entity.Entity source = null;

        // Try to find the source player and their AP
        if (entity instanceof IPersistentDataAccessor accessor) {
            net.minecraft.nbt.CompoundTag data = accessor.creraces$getPersistentData();
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
                                
                                // Scale damage by AP and config factor
                                damage += (float) (ap * CreRacesConfig.BLEEDING_SCALING.get());
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        var registry = entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        var resKey = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("creraces", "bleeding"));
        var holder = registry.getHolder(java.util.Objects.requireNonNull(resKey)).orElse(null);

        if (holder != null) {
            DamageSource ds = new DamageSource(java.util.Objects.requireNonNull(holder), source, source);
            
            // Multiplier based on amplifier (stacking support)
            float finalDamage = damage * (1.0f + amplifier);
            
            entity.hurt(ds, finalDamage);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Run every 20 ticks (1 second)
        return duration % 20 == 0;
    }
}
