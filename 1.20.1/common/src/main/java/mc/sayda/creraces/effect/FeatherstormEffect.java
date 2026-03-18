package mc.sayda.creraces.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Featherstorm - Harpy beneficial effect.
 * Granted to the player on hit.
 * Provides Attack Speed, Movement Speed, and Armor.
 * Spawns feather-like particles.
 */
public class FeatherstormEffect extends MobEffect {
    public FeatherstormEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700);
        this.addAttributeModifier(Attributes.ATTACK_SPEED, "051381d0-7fb7-34ec-a441-c62c605093b1", 0.5,
                AttributeModifier.Operation.ADDITION);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, "8693812a-34fa-352d-8518-022570ddcbb1", 0.1,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        this.addAttributeModifier(Attributes.ARMOR, "e7e6e689-5c85-3147-858e-7ceaea5ea121", 4.0,
                AttributeModifier.Operation.ADDITION);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            // Spawn cloud/poof particles as representative of "feathers"
            level.sendParticles(ParticleTypes.CLOUD,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                    2, 0.3, 0.5, 0.3, 0.02);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Apply particles every tick
        return true;
    }
}
