package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffectCategory;

/** Marker toggled and checked by ability and race JSON (fruitful harvest/sacrifice, Undead); no Java logic. */
public class LifeDrainEffect extends SimpleEffect {
    public LifeDrainEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
