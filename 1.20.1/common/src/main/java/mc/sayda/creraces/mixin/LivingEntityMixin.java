package mc.sayda.creraces.mixin;

import mc.sayda.creraces.entity.UndeadRemainsEntity;
import mc.sayda.creraces.registry.ModEntities;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import mc.sayda.creraces.util.ISleepSlotTracker;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.registry.ModAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.MobType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements ISleepSlotTracker {

    public LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Shadow
    protected boolean jumping;

    @Shadow
    protected int noJumpDelay;

    @Shadow
    protected abstract void jumpFromGround();

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

    @Shadow
    protected abstract int increaseAirSupply(int air);

    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float creraces$applyHealingReceived(float amount) {
        if (amount <= 0)
            return amount;
        LivingEntity entity = (LivingEntity) (Object) this;
        double multiplier = mc.sayda.creraces.util.CombatAttributes.getHealingReceived(entity);
        return (float) (amount * multiplier);
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void creraces$cancelJump(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        var stunned = mc.sayda.creraces.registry.ModMobEffects.STUNNED.get();
        var rooted = mc.sayda.creraces.registry.ModMobEffects.ROOTED.get();
        var frozen = mc.sayda.creraces.registry.ModMobEffects.FROZEN.get();
        if ((stunned != null && entity.hasEffect(stunned)) ||
                (rooted != null && entity.hasEffect(rooted)) ||
                (frozen != null && entity.hasEffect(frozen))) {
            ci.cancel();
        }
    }

    @Inject(method = "baseTick", at = @At("TAIL"))
    private void creraces$landSuffocation(CallbackInfo ci) {
        mc.sayda.creraces.engine.SpiritMobilityHandler.tick((LivingEntity) (Object) this);
        mc.sayda.creraces.engine.AquaticMovementHandler.buoyancyTick((LivingEntity) (Object) this);
        if ((Object) this instanceof Player) {
            Player player = (Player) (Object) this;
            if (player.isAlive() && !player.level().isClientSide()) {
                DataUtils.getVariables(player).ifPresent(vars -> {
                    Race race = RaceRegistry.get(vars.getRace());
                    if (race != null) {
                        Race.Passives passives = race.passives();
                        if (passives != null) {
                            // Underwater Air Refill (Aquatic/Undead Races)
                            boolean canBreatheWater = vars.isAquatic() || vars.isUndead()
                                    || passives.canBreatheUnderwater();
                            var waterTag = net.minecraft.tags.FluidTags.WATER;
                            if (canBreatheWater && waterTag != null && player.isEyeInFluid(waterTag)) {
                                if (player.getAirSupply() < player.getMaxAirSupply()) {
                                    player.setAirSupply(this.increaseAirSupply(player.getAirSupply()));
                                }
                            }

                            // Land Suffocation (Aquatic Races - Overridable)
                            int airInterval = passives.landSuffocationInterval();
                            boolean mustBeInWater = (vars.isAquatic() || airInterval > 0) && !vars.isUndead();
                            var waterBreathing = net.minecraft.world.effect.MobEffects.WATER_BREATHING;
                            if (mustBeInWater && !player.isInWaterRainOrBubble()
                                    && (waterBreathing == null || !player.hasEffect(waterBreathing))) {

                                // Deterministic air decay based on interval
                                int interval = airInterval > 0 ? airInterval : 1; // Default to 1 if isAquatic was true
                                                                                  // but no interval
                                if (player.tickCount % interval == 0) {
                                    int air = player.getAirSupply();
                                    player.setAirSupply(this.decreaseAirSupply(air));
                                }
                                if (player.getAirSupply() <= -20) {
                                    player.setAirSupply(0);
                                    var drownSource = player.damageSources().drown();
                                    if (drownSource != null)
                                        player.hurt(drownSource, 2.0F);
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    @Inject(method = "canBreatheUnderwater", at = @At("HEAD"), cancellable = true)
    private void creraces$canBreatheUnderwater(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player) {
            Player player = (Player) (Object) this;
            DataUtils.getVariables(player).ifPresent(vars -> {
                if (vars.isAquatic() || vars.isUndead()) {
                    cir.setReturnValue(true);
                    return;
                }
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    Race.Passives passives = race.passives();
                    if (passives != null && passives.canBreatheUnderwater()) {
                        cir.setReturnValue(true);
                    }
                }
            });
        }
    }

    @Inject(method = "createLivingAttributes", at = @At("RETURN"))
    private static void creraces$createAttributes(
            CallbackInfoReturnable<net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder> cir) {
        try {
            var builder = cir.getReturnValue();
            var attributes = new net.minecraft.world.entity.ai.attributes.Attribute[] {
                    mc.sayda.creraces.registry.ModAttributes.HEALING_RECEIVED.get(),
                    mc.sayda.creraces.registry.ModAttributes.ARMOR_PIERCE.get(),
                    mc.sayda.creraces.registry.ModAttributes.ARMOR_SHRED.get(),
                    mc.sayda.creraces.registry.ModAttributes.MAGIC_RESIST.get(),
                    mc.sayda.creraces.registry.ModAttributes.MAGIC_PIERCE.get(),
                    mc.sayda.creraces.registry.ModAttributes.MAGIC_SHRED.get()
            };
            for (var attr : attributes) {
                if (attr != null) {
                    try {
                        builder.add(attr);
                    } catch (IllegalArgumentException e) {
                        // Already added (e.g. by PlayerMixin), safely ignore
                    }
                }
            }
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error(
                    "Failed to add custom generic attributes to LivingEntity.createLivingAttributes: {}",
                    e.getMessage());
        }
    }

    @SuppressWarnings("null")
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float creraces$applyDamageModifiers(float amount, DamageSource source) {
        if (amount <= 0)
            return amount;

        // General Damage Immunity Check
        if ((Object) this instanceof Player player) {
            mc.sayda.creraces.capability.IPlayerVariables vars = DataUtils.getVariables(player).orElse(null);
            if (vars != null) {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    Race.Passives passives = race.passives();
                    if (passives != null) {
                        java.util.List<String> immune = passives.immuneToDamageTypes();
                        if (immune != null && !immune.isEmpty()) {
                            String idStr = source.typeHolder().unwrapKey()
                                    .map(k -> k.location().toString()).orElse("");
                            String path = source.typeHolder().unwrapKey()
                                    .map(k -> k.location().getPath()).orElse("");
                            for (String blocked : immune) {
                                if (blocked.equals(idStr) || blocked.equals(path))
                                    return 0.0f;
                            }
                        }

                        if (passives.unaffectedByLava()) {
                            String dmgType = source.typeHolder().unwrapKey()
                                    .map(k -> k.location().getPath()).orElse("");
                            if (dmgType.equals("in_fire") || dmgType.equals("on_fire") ||
                                    dmgType.equals("lava") || dmgType.equals("hot_floor"))
                                return 0.0f;
                        }
                        if (passives.unaffectedByWater()) {
                            String dmgType = source.typeHolder().unwrapKey()
                                    .map(k -> k.location().getPath()).orElse("");
                            if (dmgType.equals("drown") || dmgType.equals("drowned"))
                                return 0.0f;
                        }
                    }
                }
            }
        }

        // Apply damage modifiers from traits
        float currentAmount = amount;
        if ((Object) this instanceof Player player) {
            IPlayerVariables vars = DataUtils.getVariables(player).orElse(null);
            if (vars != null) {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null && race.traits() != null) {
                    for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                        currentAmount = trait.modifyDamageTaken(player, source, currentAmount);
                    }
                }
            }
        }

        return currentAmount;
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void creraces$onHitLogic(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        this.creraces$lastDamageSource = source;
        if (mc.sayda.creraces.engine.SpiritMobilityHandler.isSpirit((LivingEntity) (Object) this)) {
            if (!source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)
                    && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                if (!source.is(mc.sayda.creraces.registry.ModDamageTags.IS_MAGIC) && !source.is(mc.sayda.creraces.registry.ModDamageTags.IS_TRUE)) {
                    net.minecraft.world.entity.Entity attacker = source.getEntity();
                    if (!(attacker instanceof LivingEntity livingAttacker)
                            || !mc.sayda.creraces.engine.SpiritMobilityHandler.isSpirit(livingAttacker)) {
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }

        if (amount <= 0) {
            cir.setReturnValue(false);
            return;
        }

        if (!this.level().isClientSide() && (Object) this instanceof Player channelTarget) {
            mc.sayda.creraces.engine.ChannelingManager.onDamage(channelTarget);
        }

        // Friendly fire / Team check
        if (source.getEntity() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) source.getEntity();
            if (!mc.sayda.creraces.team.RaceTeamManager.canHurt((LivingEntity) (Object) this, attacker)) {
                creraces$healBlockedServantFire((LivingEntity) (Object) this, attacker, amount);
                cir.setReturnValue(false);
                return;
            }
        }

        // Visual Feedback for Damage Types (LoL-style)
        if (!this.level().isClientSide()) {
            net.minecraft.core.particles.SimpleParticleType pt = null;
            var magicTag = mc.sayda.creraces.registry.ModDamageTags.IS_MAGIC;
            var physicalTag = mc.sayda.creraces.registry.ModDamageTags.IS_PHYSICAL;
            var trueTag = mc.sayda.creraces.registry.ModDamageTags.IS_TRUE;

            if (magicTag != null && source.is(magicTag)) {
                pt = mc.sayda.creraces.registry.ModParticles.MAGIC_DAMAGE.get();
            } else if (physicalTag != null && source.is(physicalTag)) {
                pt = mc.sayda.creraces.registry.ModParticles.PHYSICAL_DAMAGE.get();
            } else if (trueTag != null && source.is(trueTag)) {
                pt = mc.sayda.creraces.registry.ModParticles.TRUE_DAMAGE.get();
            }

            if (pt != null && this.level() instanceof net.minecraft.server.level.ServerLevel) {
                net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) this
                        .level();
                // Spawn particles on server-side to sync with all clients
                serverLevel.sendParticles(pt, this.getX(), this.getY(0.5), this.getZ(), 15, 0.2, 0.2, 0.2, 0.1);
            }
        }

        // Handle being hit (Attacker is Player)
        if (source.getEntity() instanceof Player) {
            Player attackerPlayer = (Player) source.getEntity();
            if (!attackerPlayer.level().isClientSide()) {
                DataUtils.getVariables(attackerPlayer).ifPresent(vars -> {
                    Race race = RaceRegistry.get(vars.getRace());
                    if (race != null) {
                        // Gain Rage on hit if the race uses it
                        if (race.resourceType() == mc.sayda.creraces.race.ResourceType.RAGE) {
                            var maxRageAttr = ModAttributes.MAX_RAGE.get();
                            double maxRage = maxRageAttr != null ? attackerPlayer.getAttributeValue(maxRageAttr) : 0.0;
                            if (vars.getRage() < maxRage) {
                                vars.setRage(Math.min(maxRage, vars.getRage() + 5.0));
                                vars.setResourceTimer(attackerPlayer.level().getGameTime());
                                // Full sync: resource changed by a discrete event
                                mc.sayda.creraces.network.BoundaryHandler.resyncVariables(attackerPlayer,
                                        attackerPlayer,
                                        true);
                            }
                        }
                    }
                });
            }
        }

        // Handle being hit (Victim is Player)
        if ((Object) this instanceof Player) {
            Player victimPlayer = (Player) (Object) this;
            if (!victimPlayer.level().isClientSide()) {
                // THORNS Logic
                var thornsEffect = mc.sayda.creraces.registry.ModMobEffects.THORNS.get();
                if (thornsEffect != null && victimPlayer.hasEffect(thornsEffect)) {
                    Entity currentAttacker = source.getEntity();
                    if (currentAttacker instanceof LivingEntity) {
                        LivingEntity le = (LivingEntity) currentAttacker;
                        if (currentAttacker != victimPlayer) {
                            var thornsSource = victimPlayer.damageSources().thorns(victimPlayer);
                            if (thornsSource != null) {
                                le.hurt(thornsSource, 2.0F);
                                victimPlayer.level().playSound(null, victimPlayer.blockPosition(),
                                        net.minecraft.sounds.SoundEvents.THORNS_HIT,
                                        net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.0f);
                            }
                        }
                    }
                }

                DataUtils.getVariables(victimPlayer).ifPresent(vars -> {
                    Race race = RaceRegistry.get(vars.getRace());
                    if (race != null) {
                        // Gain Grit on being hit if the race uses it
                        if (race.resourceType() == mc.sayda.creraces.race.ResourceType.GRIT) {
                            var maxGritAttr = ModAttributes.MAX_GRIT.get();
                            double maxGrit = maxGritAttr != null ? victimPlayer.getAttributeValue(maxGritAttr) : 0.0;
                            if (vars.getGrit() < maxGrit) {
                                vars.setGrit(Math.min(maxGrit, vars.getGrit() + 5.0));
                                vars.setResourceTimer(victimPlayer.level().getGameTime());
                                // Full sync: resource changed by a discrete event
                                mc.sayda.creraces.network.BoundaryHandler.resyncVariables(victimPlayer, victimPlayer,
                                        true);
                            }
                        }

                        // Camouflage Interrupt
                        var camouflageEffect = mc.sayda.creraces.registry.ModMobEffects.CAMOUFLAGE.get();
                        if (camouflageEffect != null && victimPlayer.hasEffect(camouflageEffect)) {
                            victimPlayer.removeEffect(camouflageEffect);
                            vars.setCooldown(new net.minecraft.resources.ResourceLocation("creraces", "camouflage"),
                                    220);
                            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(victimPlayer, victimPlayer, true);
                        }

                        // Trigger data-driven on_hurt traits
                        if (race.traits() != null) {
                            for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                                trait.onHurt(victimPlayer, source, amount);
                            }
                        }
                    }
                });
            }
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
        if ((Object) this instanceof Player) {
            Player player = (Player) (Object) this;
            if (!player.level().isClientSide()) {
                var varsOpt = DataUtils.getVariables(player);
                if (varsOpt.isPresent()) {
                    mc.sayda.creraces.capability.IPlayerVariables vars = varsOpt.get();
                    Race race = RaceRegistry.get(vars.getRace());
                    Race.Passives passives = race != null ? race.passives() : null;

                    int airInterval = passives != null ? passives.landSuffocationInterval() : -1;
                    boolean mustBeInWater = (vars.isAquatic() || airInterval > 0) && !vars.isUndead();
                    // Null-check WATER_BREATHING for consistency with baseTick pattern
                    var waterBreathing = net.minecraft.world.effect.MobEffects.WATER_BREATHING;
                    if (mustBeInWater && !player.isInWaterRainOrBubble()
                            && (waterBreathing == null || !player.hasEffect(waterBreathing))) {
                        cir.setReturnValue(air);
                    }
                }
            }
        }
    }

    @Unique
    private DamageSource creraces$lastDamageSource;

    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    private void creraces$knockbackPassive(double strength, double x, double z, CallbackInfo ci) {
        // Workaround for 1.20.1: Native no_knockback tag is only fully supported in
        // 1.21.1+
        // This mixin-based check ensures tagged damage types skip knockback.
        if (this.creraces$lastDamageSource != null
                && this.creraces$lastDamageSource.is(mc.sayda.creraces.registry.ModDamageTags.NO_KNOCKBACK)) {
            ci.cancel();
            return;
        }

        if ((Object) this instanceof Player) {
            Player player = (Player) (Object) this;
            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    Race.Passives passives = race.passives();
                    if (passives != null && passives.immuneToKnockback()) {
                        ci.cancel();
                    }
                }
            });
        }
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void creraces$fallDamagePassive(float distance, float multiplier, DamageSource source,
            CallbackInfoReturnable<Boolean> cir) {
        // Global Spirit Realm fall damage immunity
        if (mc.sayda.creraces.engine.SpiritMobilityHandler.isSpirit((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onClimbable()Z", at = @At("HEAD"), cancellable = true)
    private void creraces$microClimbable(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        BlockPos pos = entity.blockPosition();
        var level = entity.level();
        if (level != null
                && level.getBlockEntity(pos) instanceof mc.sayda.creraces.block.entity.MicroBlockEntity micro) {
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
        // Check a few offset points so thin ladders/vines near slot boundaries aren't missed.
        double x = entity.getX() + ox;
        double y = entity.getY();
        double z = entity.getZ() + oz;

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

        if (micro == null)
            return false;
        BlockState slotState = micro.getSlot(sx, sy, sz);
        var climbableTag = net.minecraft.tags.BlockTags.CLIMBABLE;
        return (climbableTag != null && slotState.is(climbableTag))
                || slotState.getBlock() instanceof net.minecraft.world.level.block.VineBlock;
    }

    @Inject(method = "getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F", at = @At("HEAD"), cancellable = true)
    private void creraces$lolDamageReduction(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        if (amount <= 0)
            return;
        LivingEntity victim = (LivingEntity) (Object) this;
        float modifiedAmount = amount;
        boolean applied = false;

        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity) {
            LivingEntity leAttacker = (LivingEntity) attacker;
            if (source.is(mc.sayda.creraces.registry.ModDamageTags.IS_PHYSICAL)) {
                double armor = mc.sayda.creraces.util.CombatAttributes.getArmor(victim);
                double toughness = victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
                double pierce = mc.sayda.creraces.util.CombatAttributes.getArmorPierce(leAttacker);
                double shred = mc.sayda.creraces.util.CombatAttributes.getArmorShred(leAttacker);

                double penEff = Math.max(0.4, 1.0 - (toughness * 0.02));
                double effectiveArmor = (armor * (1.0 - (shred * penEff))) - (pierce * penEff);

                if (effectiveArmor > 0) {
                    modifiedAmount *= (float) (100.0 / (100.0 + effectiveArmor));
                }
                applied = true;
            } else if (source.is(mc.sayda.creraces.registry.ModDamageTags.IS_MAGIC)) {
                double mr = mc.sayda.creraces.util.CombatAttributes.getMagicResist(victim);
                double pierce = mc.sayda.creraces.util.CombatAttributes.getMagicPierce(leAttacker);
                double shred = mc.sayda.creraces.util.CombatAttributes.getMagicShred(leAttacker);

                double effectiveMR = (mr * (1.0 - shred)) - pierce;
                if (effectiveMR > 0) {
                    modifiedAmount *= (float) (100.0 / (100.0 + effectiveMR));
                }
                applied = true;
            }
        } else {
            var magicTag = mc.sayda.creraces.registry.ModDamageTags.IS_MAGIC;
            if (magicTag != null && source.is(magicTag)) {
                // Environment/No-attacker magic damage (e.g. potion)
                double mr = mc.sayda.creraces.util.CombatAttributes.getMagicResist(victim);
                if (mr > 0) {
                    modifiedAmount *= (float) (100.0 / (100.0 + mr));
                }
                applied = true;
            }
        }

        try {
            var defScaleType = virtuoel.pehkui.api.ScaleTypes.DEFENSE;
            if (defScaleType != null) {
                float defScale = defScaleType.getScaleData(victim).getScale();
                if (defScale != 1.0f && defScale > 0) {
                    modifiedAmount /= defScale;
                    // Always return when Pehkui modifies, since the value is now different
                    // from vanilla's calculation. If 'applied' was already true from
                    // physical/magic reduction, this stacks correctly.
                    applied = true;
                }
            }
        } catch (Throwable ignored) {
        }

        if (applied) {
            cir.setReturnValue(modifiedAmount);
        }
    }

    @Unique
    private boolean creraces$isServant(LivingEntity entity) {
        return ((IPersistentDataAccessor) entity).creraces$getPersistentData().contains("creraces:servant_of");
    }

    /**
     * When friendly fire is blocked (e.g. one servant's arrow hits another of the
     * same commander), convert the would-be damage into healing for the victim
     * instead of just no-op'ing the hit, and drop either side's target if it was
     * pointed at the other - otherwise they keep swinging/shooting at a target
     * they can never actually hurt.
     */
    @Unique
    private void creraces$healBlockedServantFire(LivingEntity victim, LivingEntity attacker, float amount) {
        if (attacker instanceof net.minecraft.world.entity.Mob attackerMob && attackerMob.getTarget() == victim) {
            attackerMob.setTarget(null);
        }
        if (victim instanceof net.minecraft.world.entity.Mob victimMob && victimMob.getTarget() == attacker) {
            victimMob.setTarget(null);
        }

        if (amount <= 0 || !(victim instanceof net.minecraft.world.entity.Mob) || !creraces$isServant(victim))
            return;

        Player victimOwner = mc.sayda.creraces.util.CombatUtils.getRootOwner(victim);
        Player attackerOwner = mc.sayda.creraces.util.CombatUtils.getRootOwner(attacker);
        if (victimOwner == null || attackerOwner == null || !victimOwner.getUUID().equals(attackerOwner.getUUID()))
            return;

        victim.heal(amount);
    }

    @Unique
    private void creraces$spawnUndeadRemains(LivingEntity victim) {
        var remainsType = ModEntities.REMAINS_UNDEAD.get();
        if (remainsType == null)
            return;
        var level = victim.level();
        if (level == null)
            return;
        UndeadRemainsEntity remains = remainsType.create(level);
        if (remains != null) {
            remains.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), 0);

            // Track owner
            if (((IPersistentDataAccessor) victim).creraces$getPersistentData().contains("creraces:servant_of")) {
                remains.setOwnerUUID(mc.sayda.creraces.capability.DataUtils.loadUUID(
                        ((IPersistentDataAccessor) victim).creraces$getPersistentData(), "creraces:servant_of"));
            }

            // Ensure the remains themselves are NOT tagged as servants
            ((IPersistentDataAccessor) remains).creraces$getPersistentData().remove("creraces:servant_of");

            victim.level().addFreshEntity(remains);
        }
    }

    @Inject(method = "getVisibilityPercent", at = @At("HEAD"), cancellable = true)
    private void creraces$trueInvisibilityVisibility(Entity viewer, CallbackInfoReturnable<Double> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (mc.sayda.creraces.registry.ModMobEffects.isInvisible(entity)) {
            cir.setReturnValue(0.0);
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void creraces$onDeath(DamageSource source, CallbackInfo ci) {
        if (this.level().isClientSide())
            return;

        LivingEntity victim = (LivingEntity) (Object) this;

        // 1. SERVANT PRIORITY (Legacy remains for specifically tagged servants)
        if (creraces$isServant(victim) && victim.getMobType() == net.minecraft.world.entity.MobType.UNDEAD) {
            creraces$spawnUndeadRemains(victim);
            return;
        }

        // 2. Data-Driven remains for players/mobs is now handled via OnKillTrait
        // in IncidentResolver and undead.json

        // 3. Existing spawnOnDeath passive logic for Players
        if ((Object) this instanceof Player) {
            Player player = (Player) (Object) this;
            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    Race.Passives passives = race.passives();
                    if (passives != null) {
                        Race.EntitySpawnData data = passives.spawnOnDeath();
                        if (data != null) {
                            // LEM-1: use tryParse so malformed JSON entity type strings don't crash the
                            // death handler
                            ResourceLocation entityTypeKey = ResourceLocation.tryParse(data.entityType());
                            for (int i = 0; i < data.count(); i++) {
                                var type = entityTypeKey != null ? BuiltInRegistries.ENTITY_TYPE.get(entityTypeKey)
                                        : null;
                                if (type != null) {
                                    var level = player.level();
                                    if (level != null) {
                                        Entity entity = type.create(level);
                                        if (entity != null) {
                                            entity.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(),
                                                    player.getXRot());
                                            level.addFreshEntity(entity);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true)
    private float creraces$applyShields(float amount, DamageSource source) {
        if (amount <= 0)
            return amount;
        LivingEntity victim = (LivingEntity) (Object) this;
        float currentAmount = amount;

        // TIERED SHIELD logic (Applying AFTER armor reduction but BEFORE
        // absorption/health)
        final net.minecraft.world.effect.MobEffect[] SHIELDS = {
                mc.sayda.creraces.registry.ModMobEffects.SHIELD.get(),
                mc.sayda.creraces.registry.ModMobEffects.AP_SHIELD.get(),
                mc.sayda.creraces.registry.ModMobEffects.AD_SHIELD.get()
        };

        for (net.minecraft.world.effect.MobEffect shield : SHIELDS) {
            if (shield == null)
                continue;
            if (victim.hasEffect(shield)) {
                boolean blocks = false;
                if (shield == mc.sayda.creraces.registry.ModMobEffects.SHIELD.get()) {
                    blocks = true;
                } else if (shield == mc.sayda.creraces.registry.ModMobEffects.AP_SHIELD.get()) {
                    blocks = source.is(mc.sayda.creraces.registry.ModDamageTags.IS_MAGIC);
                } else if (shield == mc.sayda.creraces.registry.ModMobEffects.AD_SHIELD.get()) {
                    blocks = source.is(mc.sayda.creraces.registry.ModDamageTags.IS_PHYSICAL);
                }

                if (!blocks)
                    continue;

                var inst = victim.getEffect(shield);
                if (inst != null) {
                    // Shield level (amplifier + 1) doubles as the shield's remaining HP.
                    float shieldHp = inst.getAmplifier() + 1.0f;
                    if (shieldHp >= currentAmount) {
                        float remaining = shieldHp - currentAmount;
                        victim.removeEffect(shield);
                        if (remaining > 0.1f) {
                            victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                    shield, -1, (int) remaining - 1, false, true));
                        }
                        victim.level().playSound((net.minecraft.world.entity.player.Player) null, victim.getX(),
                                victim.getY(), victim.getZ(),
                                net.minecraft.sounds.SoundEvents.ITEM_BREAK,
                                net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 0.8f);
                        return 0.0f;
                    } else {
                        currentAmount -= shieldHp;
                        victim.removeEffect(shield);
                        victim.level().playSound((net.minecraft.world.entity.player.Player) null, victim.getX(),
                                victim.getY(), victim.getZ(),
                                net.minecraft.sounds.SoundEvents.ITEM_BREAK,
                                net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 0.5f);
                    }
                }
            }
        }
        return currentAmount;
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void creraces$blockNegatedEffect(
            MobEffectInstance effectInstance,
            @javax.annotation.Nullable Entity source,
            CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof Player))
            return;
        Player player = (Player) (Object) this;
        DataUtils.getVariables(player).ifPresent(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race == null || race.passives() == null)
                return;
            java.util.List<String> negated = race.passives().immuneToPotionEffects();
            if (negated == null || negated.isEmpty())
                return;

            ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effectInstance.getEffect());
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

    @Inject(method = "getMobType", at = @At("HEAD"), cancellable = true)
    private void creraces$getMobType(CallbackInfoReturnable<net.minecraft.world.entity.MobType> cir) {
        if ((Object) this instanceof Player) {
            Player player = (Player) (Object) this;
            mc.sayda.creraces.capability.DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (vars.isUndead() || (race != null && race.isUndead())) {
                    cir.setReturnValue(MobType.UNDEAD);
                } else if (vars.isAquatic() || (race != null && race.isAquatic())) {
                    cir.setReturnValue(MobType.WATER);
                }
            });
        }
    }

    // 0.02F is vanilla's base swim-force constant in LivingEntity#travel, used
    // for both water and lava - scaling it here affects both liquids at once.
    @ModifyConstant(method = "travel", constant = @Constant(floatValue = 0.02F))
    private float creraces$applyLiquidSpeedMultiplier(float constant) {
        if ((Object) this instanceof Player player) {
            return DataUtils.getVariables(player).map(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null && race.passives() != null) {
                    return constant * (float) race.passives().liquidSpeedMultiplier().evaluate(player);
                }
                return constant;
            }).orElse(constant);
        }
        return constant;
    }
}
