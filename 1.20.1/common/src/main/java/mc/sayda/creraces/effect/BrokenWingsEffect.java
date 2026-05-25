package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Broken Wings effect — applied by the on_tick broken wings logic in each fairy
 * sub-race JSON when a fairy is exposed to an incompatible biome temperature for
 * too long.  FlightTrait checks for this effect and prevents flight while it is
 * active.
 */
public class BrokenWingsEffect extends MobEffect {

    public BrokenWingsEffect() {
        super(MobEffectCategory.HARMFUL, 0x9B4F96);
    }
}
