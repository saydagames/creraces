package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class TalonStrikeEffect extends MobEffect {
    public TalonStrikeEffect(MobEffectCategory category, int color) {
        super(category, color);
        // Slowness (Stun-like)
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("creraces", "talon_strike_speed"),
                -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        // Additional logic if needed (e.g. inability to jump)
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
