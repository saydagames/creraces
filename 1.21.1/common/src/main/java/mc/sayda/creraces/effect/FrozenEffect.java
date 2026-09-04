package mc.sayda.creraces.effect;

import net.minecraft.resources.ResourceLocation;
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
                ResourceLocation.fromNamespaceAndPath("creraces", "frozen_speed"),
                -1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
