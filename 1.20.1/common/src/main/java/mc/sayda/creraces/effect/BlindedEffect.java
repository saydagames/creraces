package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Objects;

/** Cuts follow range by 90%, so an affected mob loses track of anything that isn't close. */
public class BlindedEffect extends SimpleEffect {
    private static final String FOLLOW_RANGE_MODIFIER_UUID = "b2c3d4e5-0001-4000-8000-000000000010";

    public BlindedEffect() {
        super(MobEffectCategory.HARMFUL, 0x000000);
        addAttributeModifier(Objects.requireNonNull(Attributes.FOLLOW_RANGE), FOLLOW_RANGE_MODIFIER_UUID, -0.9D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
