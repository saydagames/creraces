package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

/**
 * BLINDED - Severely reduces the entity's follow-range, simulating blindness by
 * making mobs unable to path to distant targets; reduces mob targeting range.
 * Uses an attribute modifier on FOLLOW_RANGE so it integrates with the vanilla
 * mob AI.
 */
public class BlindedEffect extends MobEffect {
    private static final UUID FOLLOW_RANGE_UID = UUID.fromString("b2c3d4e5-0001-4000-8000-000000000010");

    public BlindedEffect() {
        super(MobEffectCategory.HARMFUL, 0x000000);
        addAttributeModifier(java.util.Objects.requireNonNull(Attributes.FOLLOW_RANGE),
                java.util.Objects.requireNonNull(FOLLOW_RANGE_UID.toString()), -0.9D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
