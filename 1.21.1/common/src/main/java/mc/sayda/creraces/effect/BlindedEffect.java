package mc.sayda.creraces.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * BLINDED - Severely reduces the entity's follow-range, simulating blindness by
 * making mobs unable to path to distant targets; reduces mob targeting range.
 * Uses an attribute modifier on FOLLOW_RANGE so it integrates with the vanilla
 * mob AI.
 */
public class BlindedEffect extends MobEffect {
    public BlindedEffect() {
        super(MobEffectCategory.HARMFUL, 0x000000);
        addAttributeModifier(Attributes.FOLLOW_RANGE,
                ResourceLocation.fromNamespaceAndPath("creraces", "blinded_follow_range"), -0.9D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
