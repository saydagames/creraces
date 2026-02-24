package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A simple mob effect that can be used as a marker or for basic attribute
 * modification.
 */
public class SimpleEffect extends MobEffect {
    public SimpleEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
