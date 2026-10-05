package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Pins the target's movement speed to zero for the duration. */
public class TalonStrikeEffect extends SimpleEffect {
    public TalonStrikeEffect(MobEffectCategory category, int color) {
        super(category, color);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160890", -1.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
