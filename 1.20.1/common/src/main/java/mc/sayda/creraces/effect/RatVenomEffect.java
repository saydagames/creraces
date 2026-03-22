package mc.sayda.creraces.effect;

import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.Registries;
import java.util.UUID;

@SuppressWarnings("null")
public class RatVenomEffect extends MobEffect {
    public RatVenomEffect() {
        super(MobEffectCategory.HARMFUL, 0x55FF55); // Light Green color
    }

    @Override
    public void applyEffectTick(@javax.annotation.Nonnull LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide())
            return;

        Entity source = null;
        if (entity instanceof IPersistentDataAccessor accessor) {
            var data = accessor.creraces$getPersistentData();
            if (data.contains("creraces:venom_source")) {
                String uuidStr = data.getString("creraces:venom_source");
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    if (entity.level() instanceof ServerLevel serverLevel) {
                        source = serverLevel.getEntity(uuid);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        var registry = entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        var holder = registry.getHolderOrThrow(
                ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("creraces", "ratvenom")));

        DamageSource ds = new DamageSource(holder, source, source);
        float scaling = mc.sayda.creraces.config.CreRacesConfig.RAT_VENOM_SCALING.get().floatValue();
        entity.hurt(ds, 0.2f + (amplifier * scaling));
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 10 == 0;
    }

}
