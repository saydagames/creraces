package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Frozen effect - used by Troll's sunlight mechanic.
 * Roots the entity in place (speed -100%) and disarms it (no attacking).
 * Checked alongside DISARMED in PlayerMixin for the disarm behaviour.
 */
public class FrozenEffect extends MobEffect {
    public FrozenEffect(MobEffectCategory category, int color) {
        super(category, color);
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                "a3b2c1d0-ee48-11ec-8ea0-0242ac120002",
                -1.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
