package mc.sayda.creraces.mixin;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    protected abstract int decreaseAirSupply(int air);

    @Inject(method = "baseTick", at = @At("TAIL"))
    private void creraces$landSuffocation(CallbackInfo ci) {
        if ((Object) this instanceof Player player && player.isAlive() && !player.level().isClientSide()) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null && race.passives() != null) {
                    // Land Suffocation (Aquatic Races)
                    if (!race.passives().canBreatheOnLand() && !player.isInWaterRainOrBubble()
                            && !player.hasEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING)) {
                        int air = player.getAirSupply();
                        player.setAirSupply(this.decreaseAirSupply(air));
                        if (player.getAirSupply() <= -20) {
                            player.setAirSupply(0);
                            player.hurt(player.damageSources().drown(), 2.0F);
                        }
                    }
                }
            });
        }
    }

    @Inject(method = "canBreatheUnderwater", at = @At("HEAD"), cancellable = true)
    private void creraces$canBreatheUnderwater(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null && race.passives() != null && race.passives().canBreatheUnderwater()) {
                    cir.setReturnValue(true);
                }
            });
        }
    }

    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    private void creraces$knockbackPassive(double strength, double x, double z, CallbackInfo ci) {
        if ((Object) this instanceof Player player) {
            Optional<IPlayerVariables> varsOpt = DataUtils.getVariables(player);
            if (varsOpt.isPresent()) {
                Race race = RaceRegistry.get(varsOpt.get().getRace());
                if (race != null && race.passives() != null && race.passives().immuneToKnockback()) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void creraces$fallDamagePassive(float distance, float multiplier, DamageSource source,
            CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof Player player) {
            Optional<IPlayerVariables> varsOpt = DataUtils.getVariables(player);
            if (varsOpt.isPresent()) {
                Race race = RaceRegistry.get(varsOpt.get().getRace());
                if (race != null && race.passives() != null) {
                    double fallMult = race.passives().fallDamageMultiplier();
                    if (fallMult == 0.0) {
                        cir.setReturnValue(0);
                    }
                }
            }
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void creraces$spawnOnDeath(DamageSource source, CallbackInfo ci) {
        if ((Object) this instanceof Player player && !player.level().isClientSide()) {
            Optional<IPlayerVariables> varsOpt = DataUtils.getVariables(player);
            if (varsOpt.isPresent()) {
                Race race = RaceRegistry.get(varsOpt.get().getRace());
                if (race != null && race.passives() != null && race.passives().spawnOnDeath() != null) {
                    Race.EntitySpawnData data = race.passives().spawnOnDeath();
                    for (int i = 0; i < data.count(); i++) {
                        net.minecraft.world.entity.Entity entity = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                                .get(new net.minecraft.resources.ResourceLocation(data.entityType()))
                                .create(player.level());
                        if (entity != null) {
                            entity.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(),
                                    player.getXRot());
                            player.level().addFreshEntity(entity);
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void creraces$onHitTrait(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getEntity() instanceof Player attacker && (Object) this instanceof Player victim) {
            if (!mc.sayda.creraces.team.RaceTeamManager.canHurt(victim, attacker)) {
                cir.setReturnValue(false);
                return;
            }
        }

        if (source.getEntity() instanceof Player player && !player.level().isClientSide()) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                boolean shouldSync = false;
                if (vars.getResourceTimer() <= 0)
                    shouldSync = true;

                // Reset combat timer on hit
                vars.setResourceTimer(100); // 5 seconds of combat status

                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    // Gain Rage on hit if the race uses it
                    if (race.resourceType() == mc.sayda.creraces.race.ResourceType.RAGE) {
                        double maxRage = player
                                .getAttributeValue(mc.sayda.creraces.registry.ModAttributes.MAX_RAGE.get());
                        if (vars.getRage() < maxRage) {
                            vars.setRage(Math.min(maxRage, vars.getRage() + 5.0));
                            shouldSync = true;
                        }
                    }
                }

                if (shouldSync) {
                    mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player);
                }
            });
        }

        // Handle being hit (Victim is Player)
        if ((Object) this instanceof Player player && !player.level().isClientSide()) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                boolean shouldSync = false;
                if (vars.getResourceTimer() <= 0)
                    shouldSync = true;

                // Reset combat timer when hit
                vars.setResourceTimer(100);

                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    // Gain Grit on being hit if the race uses it
                    if (race.resourceType() == mc.sayda.creraces.race.ResourceType.GRIT) {
                        double maxGrit = player
                                .getAttributeValue(mc.sayda.creraces.registry.ModAttributes.MAX_GRIT.get());
                        if (vars.getGrit() < maxGrit) {
                            vars.setGrit(Math.min(maxGrit, vars.getGrit() + 5.0));
                            shouldSync = true;
                        }
                    }

                    // Trigger data-driven on_hurt traits
                    if (race.traits() != null) {
                        for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                            trait.onHurt(player, source, amount);
                        }
                    }
                }

                if (shouldSync) {
                    mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player);
                }
            });
        }
    }
}
