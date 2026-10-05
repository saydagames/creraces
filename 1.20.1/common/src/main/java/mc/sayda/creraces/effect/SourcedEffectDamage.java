package mc.sayda.creraces.effect;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.util.DamageGuard;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared plumbing for {@link SourceTrackedEffect}s. The applier's UUID is written to the target's
 * persistent data under {@link SourceTrackedEffect#SOURCE_KEY} when the effect is applied
 * (ApplyEffectAction, the poison emitters).
 */
@SuppressWarnings("null")
final class SourcedEffectDamage {
    private SourcedEffectDamage() {
    }

    /** The recorded applier, or null if none was recorded or it is no longer loaded. */
    @Nullable
    static Entity findSource(LivingEntity target) {
        if (!(target instanceof IPersistentDataAccessor accessor) || !(target.level() instanceof ServerLevel level))
            return null;
        String uuid = accessor.creraces$getPersistentData().getString(SourceTrackedEffect.SOURCE_KEY);
        if (uuid.isEmpty())
            return null;
        try {
            return level.getEntity(UUID.fromString(uuid));
        } catch (IllegalArgumentException malformedUuid) {
            return null;
        }
    }

    /** The source's Ability Power, or 0 when it has none. */
    static double abilityPowerOf(@Nullable Entity source) {
        if (!(source instanceof LivingEntity living))
            return 0.0;
        Attribute abilityPower = ModAttributes.resolve(ModAttributes.ABILITY_POWER);
        if (abilityPower == null)
            return 0.0;
        try {
            return living.getAttributeValue(abilityPower);
        } catch (IllegalArgumentException missingAttribute) {
            // Only players are given Ability Power, so a mob source has none.
            return 0.0;
        }
    }

    /**
     * Hurts the target with a creraces damage type, credited to the source. Runs under DamageGuard so
     * a damage tick doesn't fire the source's on-hit traits. Does nothing if the damage type isn't loaded.
     */
    static void hurt(LivingEntity target, String damageTypePath, @Nullable Entity source, float amount) {
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE,
                new ResourceLocation(CreRaces.MODID, damageTypePath));
        Optional<Holder.Reference<DamageType>> damageType = target.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolder(key);
        if (damageType.isEmpty())
            return;

        DamageGuard.setProcessing(true);
        try {
            target.hurt(new DamageSource(damageType.get(), source, source), amount);
        } finally {
            DamageGuard.setProcessing(false);
        }
    }
}
