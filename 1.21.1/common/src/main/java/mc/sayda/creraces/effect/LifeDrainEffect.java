package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Life Drain marker effect, applied and cleared by the on_tick logic in the fruitful
 * harvest/sacrifice ability JSONs via required_effect/remove_effect. No Java-side logic.
 */
public class LifeDrainEffect extends MobEffect {
    public LifeDrainEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
