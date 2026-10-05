package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Troll sunlight penalty. Pins movement speed to zero here; PlayerMixin also blocks attacking and
 * interacting and LivingEntityMixin blocks jumping while it is active.
 */
public class FrozenEffect extends SimpleEffect {
    public FrozenEffect(MobEffectCategory category, int color) {
        super(category, color);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, "a3b2c1d0-ee48-11ec-8ea0-0242ac120002", -1.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
