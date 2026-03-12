package mc.sayda.creraces.mixin;

import mc.sayda.creraces.entity.RemainsEntity;
import mc.sayda.creraces.entity.UndeadRemainsEntity;
import mc.sayda.creraces.registry.ModEntities;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import mc.sayda.creraces.util.ISleepSlotTracker;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements ISleepSlotTracker {

    public LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Unique
    private int creraces$sleepSlot = -1;

    @Override
    public int creraces$getSleepSlot() {
        return creraces$sleepSlot;
    }

    @Override
    public void creraces$setSleepSlot(int slot) {
        this.creraces$sleepSlot = slot;
    }

    @Shadow
    protected abstract int decreaseAirSupply(int air);

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void creraces$cancelJump(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.hasEffect(mc.sayda.creraces.registry.ModMobEffects.STUNNED.get()) ||
                entity.hasEffect(mc.sayda.creraces.registry.ModMobEffects.ROOTED.get())) {
            ci.cancel();
        }
    }

    @Inject(method = "baseTick", at = @At("TAIL"))
    private void creraces$landSuffocation(CallbackInfo ci) {
        mc.sayda.creraces.engine.SpiritMobilityHandler.tick((LivingEntity) (Object) this);
        mc.sayda.creraces.engine.AquaticMovementHandler.tick((LivingEntity) (Object) this);
        if ((Object) this instanceof Player player && player.isAlive() && !player.level().isClientSide()) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    mc.sayda.creraces.race.Race.Passives passives = race.passives();
                    if (passives != null) {
                        // Land Suffocation (Aquatic Races)
                        if (!passives.canBreatheOnLand() && !player.isInWaterRainOrBubble()
                                && !player.hasEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING)) {
                            int air = player.getAirSupply();
                            player.setAirSupply(this.decreaseAirSupply(air));
                            if (player.getAirSupply() <= -20) {
                                player.setAirSupply(0);
                                player.hurt(player.damageSources().drown(), 2.0F);
                            }
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
                if (race != null) {
                    mc.sayda.creraces.race.Race.Passives passives = race.passives();
                    if (passives != null && passives.canBreatheUnderwater()) {
                        cir.setReturnValue(true);
                    }
                }
            });
        }
    }

    /**
     * Suppress vanilla's automatic air refill when the player is not in water but
     * their
     * race cannot breathe on land. Without this, vanilla fills air back up on the
     * same
     * tick that our baseTick drain runs, creating the oscillation bubble effect.
     */
    @Inject(method = "increaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void creraces$suppressLandAirRefill(int air, CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof Player player && !player.level().isClientSide()) {
            var varsOpt = DataUtils.getVariables(player);
            if (varsOpt.isPresent()) {
                Race race = RaceRegistry.get(varsOpt.get().getRace());
                if (race != null) {
                    mc.sayda.creraces.race.Race.Passives passives = race.passives();
                    if (passives != null
                            && !passives.canBreatheOnLand()
                            && !player.isInWaterRainOrBubble()
                            && !player.hasEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING)) {
                        // Return air unchanged — do not let vanilla refill it while we're draining on
                        // land
                        cir.setReturnValue(air);
                    }
                }
            }
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
            CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player) {
            Optional<IPlayerVariables> varsOpt = DataUtils.getVariables(player);
            if (varsOpt.isPresent()) {
                Race race = RaceRegistry.get(varsOpt.get().getRace());
                if (race != null) {
                    mc.sayda.creraces.race.Race.Passives passives = race.passives();
                    if (passives != null) {
                        double fallMult = passives.fallDamageMultiplier().evaluate(player);
                        if (fallMult == 0.0) {
                            cir.setReturnValue(false);
                            return;
                        }
                    }
                }
            }
        }

        // Global Spirit Realm fall damage immunity
        if (mc.sayda.creraces.engine.SpiritMobilityHandler.isSpirit((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void creraces$onDeath(DamageSource source, CallbackInfo ci) {
        if (this.level().isClientSide()) return;

        LivingEntity victim = (LivingEntity) (Object) this;
        Entity killer = source.getEntity();

        // 1. SERVANT PRIORITY
        if (creraces$isServant(victim)) {
            creraces$spawnUndeadRemains(victim);
            return; // Servant remains take priority
        }

        // 2. UNDEAD KILLER LOGIC
        if (victim.getType().getCategory() != net.minecraft.world.entity.MobCategory.MISC) {
            Player rootOwner = creraces$getRootUndeadOwner(killer);
            if (rootOwner != null) {
                creraces$spawnNormalRemains(victim);
            }
        }

        // 3. Existing spawnOnDeath logic for Players
        if ((Object) this instanceof Player player) {
            Optional<IPlayerVariables> varsOpt = DataUtils.getVariables(player);
            if (varsOpt.isPresent()) {
                Race race = RaceRegistry.get(varsOpt.get().getRace());
                if (race != null) {
                    mc.sayda.creraces.race.Race.Passives passives = race.passives();
                    if (passives != null && passives.spawnOnDeath() != null) {
                        Race.EntitySpawnData data = passives.spawnOnDeath();
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
    }

    /**
     * Cancel applying a mob effect if the player's race negates it.
     * Covers both "negate_effects" JSON keys.
     */
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void creraces$blockNegatedEffect(
            net.minecraft.world.effect.MobEffectInstance effectInstance,
            @javax.annotation.Nullable net.minecraft.world.entity.Entity source,
            CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof Player player))
            return;
        DataUtils.getVariables(player).ifPresent(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race == null || race.passives() == null)
                return;
            java.util.List<String> negated = race.passives().immuneToPotionEffects();
            if (negated == null || negated.isEmpty())
                return;
            @SuppressWarnings("null")
            net.minecraft.resources.ResourceLocation effectId = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                    .getKey(effectInstance.getEffect());
            if (effectId == null)
                return;
            String idStr = effectId.toString();
            String path = effectId.getPath();
            for (String blocked : negated) {
                if (blocked.equals(idStr) || blocked.equals(path)) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        });
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void creraces$onHitTrait(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getEntity() instanceof Player attacker && (Object) this instanceof Player victim) {
            if (!mc.sayda.creraces.team.RaceTeamManager.canHurt(victim, attacker)) {
                cir.setReturnValue(false);
                return;
            }
        }

        // unaffectedByLava: immune to lava/fire damage
        if ((Object) this instanceof Player player) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    mc.sayda.creraces.race.Race.Passives passives = race.passives();
                    if (passives != null) {
                        if (passives.unaffectedByLava()) {
                            String dmgType = source.typeHolder().unwrapKey()
                                    .map(k -> k.location().getPath()).orElse("");
                            if (dmgType.equals("in_fire") || dmgType.equals("on_fire") ||
                                    dmgType.equals("lava") || dmgType.equals("hot_floor")) {
                                cir.setReturnValue(false);
                            }
                        }
                        if (passives.unaffectedByWater()) {
                            String dmgType = source.typeHolder().unwrapKey()
                                    .map(k -> k.location().getPath()).orElse("");
                            if (dmgType.equals("drown") || dmgType.equals("drowned")) {
                                cir.setReturnValue(false);
                            }
                        }
                    }
                }
            });
            if (cir.isCancelled())
                return;
        }

        // Apply damage modifiers from traits
        final float[] finalAmount = { amount };
        if ((Object) this instanceof Player player) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null && race.traits() != null) {
                    for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                        finalAmount[0] = trait.modifyDamageTaken(player, source, finalAmount[0]);
                    }
                }
            });
        }

        if (finalAmount[0] <= 0) {
            cir.setReturnValue(false);
            return;
        }

        if (source.getEntity() instanceof Player player && !player.level().isClientSide()) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    // Gain Rage on hit if the race uses it
                    if (race.resourceType() == mc.sayda.creraces.race.ResourceType.RAGE) {
                        double maxRage = player
                                .getAttributeValue(mc.sayda.creraces.registry.ModAttributes.MAX_RAGE.get());
                        if (vars.getRage() < maxRage) {
                            vars.setRage(Math.min(maxRage, vars.getRage() + 5.0));
                            // Full sync: resource changed by a discrete event
                            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player, true);
                        }
                    }
                }
            });
        }

        // Handle being hit (Victim is Player)
        if ((Object) this instanceof Player player && !player.level().isClientSide()) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    // Gain Grit on being hit if the race uses it
                    if (race.resourceType() == mc.sayda.creraces.race.ResourceType.GRIT) {
                        double maxGrit = player
                                .getAttributeValue(mc.sayda.creraces.registry.ModAttributes.MAX_GRIT.get());
                        if (vars.getGrit() < maxGrit) {
                            vars.setGrit(Math.min(maxGrit, vars.getGrit() + 5.0));
                            // Full sync: resource changed by a discrete event
                            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player, true);
                        }
                    }

                    // Trigger data-driven on_hurt traits
                    if (race.traits() != null) {
                        for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                            trait.onHurt(player, source, amount);
                        }
                    }
                }
            });
        }
    }

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void creraces$microClimbable(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        BlockPos pos = entity.blockPosition();
        if (entity.level().getBlockEntity(pos) instanceof mc.sayda.creraces.block.entity.MicroBlockEntity micro) {
            // Check a 0.1m radius around the entity's position to pick up thin
            // ladders/vines
            double r = 0.1;
            if (checkGridClimbable(entity, pos, micro, 0, 0) ||
                    checkGridClimbable(entity, pos, micro, r, 0) ||
                    checkGridClimbable(entity, pos, micro, -r, 0) ||
                    checkGridClimbable(entity, pos, micro, 0, r) ||
                    checkGridClimbable(entity, pos, micro, 0, -r)) {
                cir.setReturnValue(true);
            }
        }
    }

    private boolean checkGridClimbable(LivingEntity entity, BlockPos hostPos,
            mc.sayda.creraces.block.entity.MicroBlockEntity micro, double ox, double oz) {
        // Use consistent scale and slot math
        double x = entity.getX() + ox;
        double y = entity.getY();
        double z = entity.getZ() + oz;

        // Check multiple Y points
        return isClimbableAt(hostPos, micro, x, y, z) ||
                isClimbableAt(hostPos, micro, x, y + 0.5, z) ||
                isClimbableAt(hostPos, micro, x, y + 1.0, z);
    }

    private boolean isClimbableAt(BlockPos hostPos, mc.sayda.creraces.block.entity.MicroBlockEntity micro, double x,
            double y, double z) {
        int sx = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(x - hostPos.getX());
        int sy = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(y - hostPos.getY());
        int sz = mc.sayda.creraces.block.entity.MicroBlockEntity.clampSlot(z - hostPos.getZ());

        if (sx < 0 || sx > 3 || sy < 0 || sy > 3 || sz < 0 || sz > 3)
            return false;

        BlockState slotState = micro.getSlot(sx, sy, sz);
        return slotState.is(net.minecraft.tags.BlockTags.CLIMBABLE)
                || slotState.getBlock() instanceof net.minecraft.world.level.block.VineBlock;
    }

    @Inject(method = "travel", at = @At("TAIL"))
    private void creraces$buoyancyTravel(net.minecraft.world.phys.Vec3 travelVector, CallbackInfo ci) {
        mc.sayda.creraces.engine.AquaticMovementHandler.buoyancyTick((LivingEntity) (Object) this);
    }

    @Unique
    private boolean creraces$isServant(LivingEntity entity) {
        return ((IPersistentDataAccessor) entity).creraces$getPersistentData().contains("creraces:servant_of");
    }

    @Unique
    private Player creraces$getRootUndeadOwner(Entity killer) {
        if (killer instanceof Player player) {
            if (creraces$isUndead(player)) return player;
        }

        if (killer instanceof OwnableEntity ownable) {
            Entity owner = ownable.getOwner();
            if (owner instanceof Player player && creraces$isUndead(player)) {
                return player;
            }
        }

        if (killer instanceof LivingEntity le && ((IPersistentDataAccessor) le).creraces$getPersistentData().contains("creraces:servant_of")) {
            try {
                UUID ownerUuid = ((IPersistentDataAccessor) le).creraces$getPersistentData().getUUID("creraces:servant_of");
                Player player = le.level().getPlayerByUUID(ownerUuid);
                if (player != null && creraces$isUndead(player)) {
                    return player;
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    @Unique
    private boolean creraces$isUndead(Player player) {
        return mc.sayda.creraces.capability.DataUtils.getVariables(player)
                .map(vars -> "creraces:undead".equals(vars.getRace()))
                .orElse(false);
    }

    @Unique
    private void creraces$spawnUndeadRemains(LivingEntity victim) {
        UndeadRemainsEntity remains = ModEntities.REMAINS_UNDEAD.get().create(victim.level());
        if (remains != null) {
            remains.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), 0);
            
            // Track owner
            if (((IPersistentDataAccessor) victim).creraces$getPersistentData().contains("creraces:servant_of")) {
                remains.setOwnerUUID(((IPersistentDataAccessor) victim).creraces$getPersistentData().getUUID("creraces:servant_of"));
            }

            // Ensure the remains themselves are NOT tagged as servants
            ((IPersistentDataAccessor) remains).creraces$getPersistentData().remove("creraces:servant_of");

            victim.level().addFreshEntity(remains);
        }
    }

    @Unique
    private void creraces$spawnNormalRemains(LivingEntity victim) {
        RemainsEntity remains = ModEntities.REMAINS.get().create(victim.level());
        if (remains != null) {
            remains.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), 0);
            
            // Ensure the remains themselves are NOT tagged as servants
            ((IPersistentDataAccessor) remains).creraces$getPersistentData().remove("creraces:servant_of");

            victim.level().addFreshEntity(remains);
        }
    }
}
