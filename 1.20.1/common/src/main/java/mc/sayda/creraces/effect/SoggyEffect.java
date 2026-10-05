package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffectCategory;

/** Marker for a waterlogged fairy: FlightTrait grounds it and Fairy Dust stops working until it dries off. */
public class SoggyEffect extends SimpleEffect {
    public SoggyEffect() {
        super(MobEffectCategory.HARMFUL, 0x4682B4);
    }
}
