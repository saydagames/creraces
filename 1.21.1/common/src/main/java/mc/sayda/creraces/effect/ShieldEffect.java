package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marker for the tiered shields. LivingEntityMixin absorbs incoming damage with it, treating
 * amplifier + 1 as the shield's remaining HP: SHIELD blocks everything, AP_SHIELD only magic
 * damage and AD_SHIELD only physical damage.
 */
public class ShieldEffect extends SimpleEffect {
    public ShieldEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
