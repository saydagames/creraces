package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffectCategory;

/**
 * Applied by the fairy sub-race JSONs after too long in a biome of the wrong temperature. FlightTrait
 * refuses flight while it is active; standing in fairy source clears it.
 */
public class BrokenWingsEffect extends SimpleEffect {
    public BrokenWingsEffect() {
        super(MobEffectCategory.HARMFUL, 0x9B4F96);
    }
}
