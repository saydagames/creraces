package mc.sayda.creraces.mixin;

import mc.sayda.creraces.engine.SpiritMobilityHandler;
import mc.sayda.creraces.race.SocialPassivesHelper;
import mc.sayda.creraces.registry.ModMobEffects;
import mc.sayda.creraces.util.CombatUtils;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mob targeting rules for the social passives, the spirit realm and servants, plus Sun Resistance. */
@Mixin(Mob.class)
public abstract class MobMixin {

    @Unique
    private static final double CRERACES_HATE_SCAN_RANGE = 16.0;

    @Shadow
    public abstract void setTarget(LivingEntity target);

    @Shadow
    public abstract LivingEntity getTarget();

    /**
     * Refuses targets the mob may not pick: players it respects ("respectedByEntities"), spirit-realm
     * entities when the mob isn't a spirit itself, and anything sharing its servant commander.
     */
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void creraces$preventRespectedTargeting(LivingEntity target, CallbackInfo ci) {
        if (target instanceof Player player && SocialPassivesHelper.isRespectedBy(player, (Mob) (Object) this)) {
            ci.cancel();
            return;
        }
        if (target != null
                && SpiritMobilityHandler.isOnSpiritPlane(target)
                && !SpiritMobilityHandler.isOnSpiritPlane((LivingEntity) (Object) this)) {
            ci.cancel();
            return;
        }

        // Blocking this at the source means a servant never acquires a fellow servant (or its
        // commander) as a target, rather than getting stuck on one it can never hurt.
        if (target != null && creraces$sharesServantOwner((Mob) (Object) this, target)) {
            ci.cancel();
        }
    }

    @Unique
    private boolean creraces$sharesServantOwner(Mob mob, LivingEntity target) {
        Player mobOwner = CombatUtils.getRootOwner(mob);
        if (mobOwner == null)
            return false;
        Player targetOwner = CombatUtils.getRootOwner(target);
        return targetOwner != null && mobOwner.getUUID().equals(targetOwner.getUUID());
    }

    /** Once a second, an idle mob picks a fight with the nearest player whose race it hates ("hatedByEntities"). */
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void creraces$hateRaces(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;

        // Staggered by entity id so every mob doesn't scan on the same tick.
        if (mob.level().getGameTime() % 20 != mob.getId() % 20)
            return;
        if (this.getTarget() != null)
            return;

        // Creative and spectator players are skipped, as in vanilla targeting.
        Player nearestHatedPlayer = mob.level().getNearestPlayer(
                mob.getX(), mob.getY(), mob.getZ(), CRERACES_HATE_SCAN_RANGE,
                entity -> entity instanceof Player player
                        && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)
                        && SocialPassivesHelper.isHatedBy(player, mob));

        if (nearestHatedPlayer != null) {
            this.setTarget(nearestHatedPlayer);
        }
    }

    /**
     * Sun Resistance stops daylight burning, no helmet needed. Covers every mob whose burn check goes
     * through Mob.isSunBurnTick: zombies (except husks), skeletons and phantoms.
     */
    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void creraces$blockSunBurnWithResistance(CallbackInfoReturnable<Boolean> cir) {
        if (((Mob) (Object) this).hasEffect(ModMobEffects.SUN_RESISTANCE)) {
            cir.setReturnValue(false);
        }
    }
}
