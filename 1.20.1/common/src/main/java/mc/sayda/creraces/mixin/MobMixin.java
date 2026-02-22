package mc.sayda.creraces.mixin;

import mc.sayda.creraces.race.SocialPassivesHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to implement social interaction passives at the base Mob level.
 */
@Mixin(Mob.class)
public abstract class MobMixin {

    @Shadow
    public abstract void setTarget(LivingEntity target);

    @Shadow
    public abstract LivingEntity getTarget();

    /**
     * Prevent targeting of respected players.
     * This handles "respectedByEntities" by blocking the setTarget call.
     */
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void creraces$preventRespectedTargeting(LivingEntity target, CallbackInfo ci) {
        if (target instanceof Player player) {
            if (SocialPassivesHelper.isRespectedBy(player, (Mob) (Object) this)) {
                // If it was already targeting this player, clear it.
                // Otherwise, just prevent setting it.
                ci.cancel();
            }
        }
    }

    /**
     * Periodically check for hated players and force attack them.
     * This handles "hatedByEntities".
     */
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void creraces$hateRaces(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;

        // Only check occasionally to avoid performance issues
        if (mob.level().getGameTime() % 20 != 0)
            return;

        // If already has a target, don't force a new one
        if (this.getTarget() != null)
            return;

        // Find nearby players that this entity type hates
        Player nearestHatedPlayer = mob.level().getNearestPlayer(
                mob.getX(), mob.getY(), mob.getZ(),
                16.0, // Range
                entity -> entity instanceof Player player && SocialPassivesHelper.isHatedBy(player, mob));

        // If we found a hated player, attack them
        if (nearestHatedPlayer != null) {
            this.setTarget(nearestHatedPlayer);
        }
    }
}
