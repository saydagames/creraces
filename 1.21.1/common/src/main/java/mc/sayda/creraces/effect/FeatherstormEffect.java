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
        this.addAttributeModifier(Attributes.ATTACK_SPEED,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("creraces", "featherstorm_attack_speed"),
                0.5, AttributeModifier.Operation.ADD_VALUE);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("creraces", "featherstorm_speed"),
                0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.ARMOR,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("creraces", "featherstorm_armor"),
                4.0, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            // Spawn cloud/poof particles as representative of "feathers"
            level.sendParticles(ParticleTypes.CLOUD,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                    2, 0.3, 0.5, 0.3, 0.02);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Apply particles every tick
        return true;
    }
}
