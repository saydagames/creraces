package mc.sayda.creraces.effect;

import mc.sayda.creraces.util.SpiritRealmUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/** Instant effect that moves a player into (banishment) or out of (revealing) the spirit realm. */
public class SpiritRealmTransitionEffect extends MobEffect {
    private final boolean entersRealm;

    public SpiritRealmTransitionEffect(boolean entersRealm) {
        super(MobEffectCategory.HARMFUL, 0x00D8FF);
        this.entersRealm = entersRealm;
    }

    @Override
    public boolean isInstantenous() {
        return true;
    }

    @Override
    public void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity indirectSource,
            LivingEntity entity, int amplifier, double health) {
        if (entity instanceof ServerPlayer player)
            SpiritRealmUtils.setInSpiritRealm(player, entersRealm);
    }
}
