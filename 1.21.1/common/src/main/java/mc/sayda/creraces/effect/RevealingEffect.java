package mc.sayda.creraces.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

public class RevealingEffect extends MobEffect {

    public RevealingEffect() {
        super(MobEffectCategory.HARMFUL, 0x00D8FF);
    }

    @Override
    public boolean isInstantenous() {
        return true;
    }

    @Override
    public void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity indirectSource,
            LivingEntity entity, int amplifier, double health) {
        if (!(entity instanceof ServerPlayer player)) return;
        mc.sayda.creraces.capability.DataUtils.getVariables(player).ifPresent(vars -> {
            if (vars.isInSpiritRealm()) {
                vars.setInSpiritRealm(false);
                mc.sayda.creraces.network.BoundaryHandler.resyncForAllTrackers(player);
                mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player);
            }
        });
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
