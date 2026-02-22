package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class RatVenomEffect extends MobEffect {
    public RatVenomEffect() {
        super(MobEffectCategory.HARMFUL, 0x55FF55); // Light Green color
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
