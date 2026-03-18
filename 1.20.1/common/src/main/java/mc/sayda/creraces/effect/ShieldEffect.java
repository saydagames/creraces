package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Shield Effect - a permanent shield effect.
 * Absorbs damage until the shield amount (amplifier) is depleted.
 * Variants include AP (Magic), AD (Physical), and Normal (All).
 * Logic is handled in LivingEntityMixin.
 */
public class ShieldEffect extends MobEffect {
    public ShieldEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
