package mc.sayda.creraces.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Cuts follow range by 90%, so an affected mob loses track of anything that isn't close. */
public class BlindedEffect extends SimpleEffect {
    public BlindedEffect() {
        super(MobEffectCategory.HARMFUL, 0x000000);
        addAttributeModifier(Attributes.FOLLOW_RANGE,
                ResourceLocation.fromNamespaceAndPath("creraces", "blinded_follow_range"), -0.9D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
