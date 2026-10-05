package mc.sayda.creraces.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Pins the target's movement speed to zero for the duration. */
public class TalonStrikeEffect extends SimpleEffect {
    public TalonStrikeEffect(MobEffectCategory category, int color) {
        super(category, color);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath("creraces", "talon_strike_speed"), -1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
