package mc.sayda.creraces.mixin;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.block.entity.MicroBlockEntity;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.effect.FairyDustEffect;
import mc.sayda.creraces.effect.ThornsEffect;
import mc.sayda.creraces.engine.AquaticMovementHandler;
import mc.sayda.creraces.engine.ChannelingManager;
import mc.sayda.creraces.engine.SpiritMobilityHandler;
import mc.sayda.creraces.engine.TraitDispatch;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.race.ResourceType;
import mc.sayda.creraces.registry.ModDamageTags;
import mc.sayda.creraces.registry.ModMobEffects;
import mc.sayda.creraces.registry.ModParticles;
import mc.sayda.creraces.team.RaceTeamManager;
import mc.sayda.creraces.util.CombatAttributes;
import mc.sayda.creraces.util.CombatUtils;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import mc.sayda.creraces.util.ISleepSlotTracker;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.registry.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.List;
import javax.annotation.Nullable;

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

    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float creraces$applyHealingReceived(float amount) {
        if (amount <= 0)
            return amount;
        LivingEntity entity = (LivingEntity) (Object) this;
        double multiplier = CombatAttributes.getHealingReceived(entity);
        return (float) (amount * multiplier);
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void creraces$cancelJump(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        var stunned = ModMobEffects.STUNNED;
        var rooted = ModMobEffects.ROOTED;
        var frozen = ModMobEffects.FROZEN;
        if ((stunned != null && entity.hasEffect(stunned)) ||
                (rooted != null && entity.hasEffect(rooted)) ||
                (frozen != null && entity.hasEffect(frozen))) {
            ci.cancel();
        }
    }

    @Inject(method = "baseTick", at = @At("TAIL"))
    private void creraces$landSuffocation(CallbackInfo ci) {
        SpiritMobilityHandler.tick((LivingEntity) (Object) this);
        AquaticMovementHandler.buoyancyTick((LivingEntity) (Object) this);
        if ((Object) this instanceof Player) {
            Player player = (Player) (Object) this;
            if (player.isAlive() && !player.level().isClientSide()) {
                DataUtils.getVariables(player).ifPresent(vars -> {
                    Race race = RaceRegistry.get(vars.getRace());
                    if (race != null) {
                        Race.Passives passives = race.passives();
                        if (passives != null) {
                            // Water breathers refill air while submerged.
                            boolean canBreatheWater = vars.isAquatic() || vars.isUndead()
                                    || passives.canBreatheUnderwater();
                            var waterTag = FluidTags.WATER;
                            if (canBreatheWater && waterTag != null && player.isEyeInFluid(waterTag)) {
                                if (player.getAirSupply() < player.getMaxAirSupply()) {
                                    player.setAirSupply(this.increaseAirSupply(player.getAirSupply()));
                                }
                            }

                            // Aquatic races lose a point of air every landSuffocationInterval ticks on land (every tick if unset).
                            int airInterval = passives.landSuffocationInterval();
                            boolean mustBeInWater = (vars.isAquatic() || airInterval > 0) && !vars.isUndead();
                            var waterBreathing = MobEffects.WATER_BREATHING;
                            if (mustBeInWater && !player.isInWaterRainOrBubble()
                                    && (waterBreathing == null || !player.hasEffect(waterBreathing))) {

                                int interval = airInterval > 0 ? airInterval : 1;
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
            CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        try {
            var builder = cir.getReturnValue();
            @SuppressWarnings("unchecked")
            Holder<Attribute>[] attributes =
                    new Holder[] {
                    ModAttributes.HEALING_RECEIVED,
                    ModAttributes.ARMOR_PIERCE,
                    ModAttributes.ARMOR_SHRED,
                    ModAttributes.MAGIC_RESIST,
                    ModAttributes.MAGIC_PIERCE,
                    ModAttributes.MAGIC_SHRED
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
            CreRaces.LOGGER.error(
                    "Failed to add custom generic attributes to LivingEntity.createLivingAttributes: {}",
                    e.getMessage());
        }
    }

    // True when the damage modifiers brought a real hit down to nothing. Set by
    // creraces$applyDamageModifiers and read by creraces$onHitLogic, which sits at the same HEAD point
    // but runs second because it is declared later.
    @Unique
    private boolean creraces$hitNegated;

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float creraces$applyDamageModifiers(float amount, DamageSource source) {
        float modified = creraces$modifyIncomingDamage(amount, source);
        this.creraces$hitNegated = amount > 0 && modified <= 0;
        return modified;
    }

    @SuppressWarnings("null")
    @Unique
    private float creraces$modifyIncomingDamage(float amount, DamageSource source) {
        if (amount <= 0)
            return amount;

        // Race-wide damage immunities cancel the hit outright.
        if ((Object) this instanceof Player player) {
            IPlayerVariables vars = DataUtils.getVariables(player).orElse(null);
            if (vars != null) {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    Race.Passives passives = race.passives();
                    if (passives != null) {
                        List<String> immune = passives.immuneToDamageTypes();
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

        float currentAmount = amount;
        if ((Object) this instanceof Player player) {
            currentAmount = TraitDispatch.runFloatChain("modifyDamageTaken", player,
                    currentAmount, (trait, value) -> trait.modifyDamageTaken(player, source, value));
        }

        return currentAmount;
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void creraces$onHitLogic(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (((LivingEntity) (Object) this).hasEffect(ModMobEffects.INVULNERABILITY)
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            cir.setReturnValue(false);
            return;
        }

        if (SpiritMobilityHandler.isOnSpiritPlane((LivingEntity) (Object) this)) {
            if (!source.is(DamageTypes.FELL_OUT_OF_WORLD)
                    && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                if (!source.is(ModDamageTags.IS_MAGIC) && !source.is(ModDamageTags.IS_TRUE)) {
                    Entity attacker = source.getEntity();
                    if (!(attacker instanceof LivingEntity livingAttacker)
                            || !SpiritMobilityHandler.isOnSpiritPlane(livingAttacker)) {
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }

        if (this.creraces$hitNegated) {
            cir.setReturnValue(false);
            return;
        }

        if (amount > 0 && !this.level().isClientSide() && (Object) this instanceof Player channelTarget) {
            ChannelingManager.onDamage(channelTarget);
        }

        // Friendly fire / Team check
        if (source.getEntity() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) source.getEntity();
            if (!RaceTeamManager.canHurt((LivingEntity) (Object) this, attacker)) {
                creraces$healBlockedServantFire((LivingEntity) (Object) this, attacker, amount);
                cir.setReturnValue(false);
                return;
            }
        }

        // Vanilla's own zero-damage hits (snowballs, eggs) keep their knockback and aggro, but trigger
        // none of the on-hit effects below.
        if (amount <= 0) {
            return;
        }

        // Particles showing whether the hit was magic, physical or true damage
        if (!this.level().isClientSide()) {
            SimpleParticleType pt = null;
            var magicTag = ModDamageTags.IS_MAGIC;
            var physicalTag = ModDamageTags.IS_PHYSICAL;
            var trueTag = ModDamageTags.IS_TRUE;

            if (magicTag != null && source.is(magicTag)) {
                pt = ModParticles.MAGIC_DAMAGE.get();
            } else if (physicalTag != null && source.is(physicalTag)) {
                pt = ModParticles.PHYSICAL_DAMAGE.get();
            } else if (trueTag != null && source.is(trueTag)) {
                pt = ModParticles.TRUE_DAMAGE.get();
            }

            if (pt != null && this.level() instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel) this
                        .level();
                // Spawn particles on server-side to sync with all clients
                serverLevel.sendParticles(pt, this.getX(), this.getY(0.5), this.getZ(), 15, 0.2, 0.2, 0.2, 0.1);
            }
        }

        // Attacker side: rage races build rage when they land a hit.
        if (source.getEntity() instanceof Player) {
            Player attackerPlayer = (Player) source.getEntity();
            if (!attackerPlayer.level().isClientSide()) {
                DataUtils.getVariables(attackerPlayer).ifPresent(vars -> {
                    Race race = RaceRegistry.get(vars.getRace());
                    if (race != null) {
                        if (race.resourceType() == ResourceType.RAGE) {
                            var maxRageAttr = ModAttributes.MAX_RAGE;
                            double maxRage = maxRageAttr != null ? attackerPlayer.getAttributeValue(maxRageAttr) : 0.0;
                            if (vars.getRage() < maxRage) {
                                vars.setRage(Math.min(maxRage, vars.getRage() + 5.0));
                                vars.setResourceTimer(attackerPlayer.level().getGameTime());
                                // Full sync: resource changed by a discrete event
                                BoundaryHandler.resyncVariables(attackerPlayer,
                                        attackerPlayer,
                                        true);
                            }
                        }
                    }
                });
            }
        }

        // Victim side
        if ((Object) this instanceof Player) {
            Player victimPlayer = (Player) (Object) this;
            if (!victimPlayer.level().isClientSide()) {
                // This runs before vanilla's invulnerability frames, so thorns damage must not trigger
                // thorns again or two players with the effect would bounce it back and forth forever.
                MobEffectInstance thorns = victimPlayer.getEffect(ModMobEffects.THORNS);
                if (thorns != null && !source.is(DamageTypes.THORNS)) {
                    Entity currentAttacker = source.getEntity();
                    if (currentAttacker instanceof LivingEntity) {
                        LivingEntity le = (LivingEntity) currentAttacker;
                        if (currentAttacker != victimPlayer) {
                            var thornsSource = victimPlayer.damageSources().thorns(victimPlayer);
                            if (thornsSource != null) {
                                le.hurt(thornsSource, ThornsEffect.retaliationDamage(amount, thorns.getAmplifier()));
                                victimPlayer.level().playSound(null, victimPlayer.blockPosition(),
                                        SoundEvents.THORNS_HIT,
                                        SoundSource.PLAYERS, 0.5f, 1.0f);
                            }
                        }
                    }
                }

                DataUtils.getVariables(victimPlayer).ifPresent(vars -> {
                    Race race = RaceRegistry.get(vars.getRace());
                    if (race != null) {
                        // Grit races build grit when they get hit.
                        if (race.resourceType() == ResourceType.GRIT) {
                            var maxGritAttr = ModAttributes.MAX_GRIT;
                            double maxGrit = maxGritAttr != null ? victimPlayer.getAttributeValue(maxGritAttr) : 0.0;
                            if (vars.getGrit() < maxGrit) {
                                vars.setGrit(Math.min(maxGrit, vars.getGrit() + 5.0));
                                vars.setResourceTimer(victimPlayer.level().getGameTime());
                                // Full sync: resource changed by a discrete event
                                BoundaryHandler.resyncVariables(victimPlayer, victimPlayer,
                                        true);
                            }
                        }

                        // Taking damage breaks camouflage and puts it on cooldown.
                        var camouflageEffect = ModMobEffects.CAMOUFLAGE;
                        if (camouflageEffect != null && victimPlayer.hasEffect(camouflageEffect)) {
                            victimPlayer.removeEffect(camouflageEffect);
                            vars.setCooldown(ResourceLocation.fromNamespaceAndPath("creraces", "camouflage"),
                                    220);
                            BoundaryHandler.resyncVariables(victimPlayer, victimPlayer, true);
                        }

                        // Data-driven on_hurt traits
                        TraitDispatch.runVoid("onHurt", victimPlayer,
                                trait -> trait.onHurt(victimPlayer, source, amount));
                    }
                });
            }
        }
    }

    /**
     * Stops vanilla refilling air on land for races that can't breathe there. Otherwise the refill
     * and the baseTick drain fight each other every tick and the air bubbles flicker.
     */
    @Inject(method = "increaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void creraces$suppressLandAirRefill(int air, CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof Player) {
            Player player = (Player) (Object) this;
            if (!player.level().isClientSide()) {
                var varsOpt = DataUtils.getVariables(player);
                if (varsOpt.isPresent()) {
                    IPlayerVariables vars = varsOpt.get();
                    Race race = RaceRegistry.get(vars.getRace());
                    Race.Passives passives = race != null ? race.passives() : null;

                    int airInterval = passives != null ? passives.landSuffocationInterval() : -1;
                    boolean mustBeInWater = (vars.isAquatic() || airInterval > 0) && !vars.isUndead();
                    var waterBreathing = MobEffects.WATER_BREATHING;
                    if (mustBeInWater && !player.isInWaterRainOrBubble()
                            && (waterBreathing == null || !player.hasEffect(waterBreathing))) {
                        cir.setReturnValue(air);
                    }
                }
            }
        }
    }

    // Unlike on 1.20.1, minecraft:no_knockback needs no handling: vanilla 1.21 skips hurt()'s knockback for it.
    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    private void creraces$knockbackPassive(double strength, double x, double z, CallbackInfo ci) {
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
        // Spirits never take fall damage.
        if (SpiritMobilityHandler.isOnSpiritPlane((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onClimbable()Z", at = @At("HEAD"), cancellable = true)
    private void creraces$microClimbable(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        BlockPos pos = entity.blockPosition();
        var level = entity.level();
        if (level != null
                && level.getBlockEntity(pos) instanceof MicroBlockEntity micro) {
            // Probe 0.1 blocks around the entity so thin ladders and vines near slot edges still count.
            double r = 0.1;
            if (creraces$checkGridClimbable(entity, pos, micro, 0, 0) ||
                    creraces$checkGridClimbable(entity, pos, micro, r, 0) ||
                    creraces$checkGridClimbable(entity, pos, micro, -r, 0) ||
                    creraces$checkGridClimbable(entity, pos, micro, 0, r) ||
                    creraces$checkGridClimbable(entity, pos, micro, 0, -r)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Unique
    private boolean creraces$checkGridClimbable(LivingEntity entity, BlockPos hostPos,
            MicroBlockEntity micro, double ox, double oz) {
        // Feet, waist and head height.
        double x = entity.getX() + ox;
        double y = entity.getY();
        double z = entity.getZ() + oz;

        return creraces$isClimbableAt(hostPos, micro, x, y, z) ||
                creraces$isClimbableAt(hostPos, micro, x, y + 0.5, z) ||
                creraces$isClimbableAt(hostPos, micro, x, y + 1.0, z);
    }

    @Unique
    private boolean creraces$isClimbableAt(BlockPos hostPos, MicroBlockEntity micro, double x,
            double y, double z) {
        int sx = MicroBlockEntity.clampSlot(x - hostPos.getX());
        int sy = MicroBlockEntity.clampSlot(y - hostPos.getY());
        int sz = MicroBlockEntity.clampSlot(z - hostPos.getZ());

        if (sx < 0 || sx > 3 || sy < 0 || sy > 3 || sz < 0 || sz > 3)
            return false;

        if (micro == null)
            return false;
        BlockState slotState = micro.getSlot(sx, sy, sz);
        var climbableTag = BlockTags.CLIMBABLE;
        return (climbableTag != null && slotState.is(climbableTag))
                || slotState.getBlock() instanceof VineBlock;
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
            if (source.is(ModDamageTags.IS_PHYSICAL)) {
                double armor = CombatAttributes.getArmor(victim);
                double toughness = victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
                double pierce = CombatAttributes.getArmorPierce(leAttacker);
                double shred = CombatAttributes.getArmorShred(leAttacker);

                double penEff = Math.max(0.4, 1.0 - (toughness * 0.02));
                double effectiveArmor = (armor * (1.0 - (shred * penEff))) - (pierce * penEff);

                if (effectiveArmor > 0) {
                    modifiedAmount *= (float) (100.0 / (100.0 + effectiveArmor));
                }
                applied = true;
            } else if (source.is(ModDamageTags.IS_MAGIC)) {
                double mr = CombatAttributes.getMagicResist(victim);
                double pierce = CombatAttributes.getMagicPierce(leAttacker);
                double shred = CombatAttributes.getMagicShred(leAttacker);

                double effectiveMR = (mr * (1.0 - shred)) - pierce;
                if (effectiveMR > 0) {
                    modifiedAmount *= (float) (100.0 / (100.0 + effectiveMR));
                }
                applied = true;
            }
        } else {
            var magicTag = ModDamageTags.IS_MAGIC;
            if (magicTag != null && source.is(magicTag)) {
                // Environment/No-attacker magic damage (e.g. potion)
                double mr = CombatAttributes.getMagicResist(victim);
                if (mr > 0) {
                    modifiedAmount *= (float) (100.0 / (100.0 + mr));
                }
                applied = true;
            }
        }

        try {
            var defScaleType = ScaleTypes.DEFENSE;
            if (defScaleType != null) {
                float defScale = defScaleType.getScaleData(victim).getScale();
                if (defScale != 1.0f && defScale > 0) {
                    modifiedAmount /= defScale;
                    // Pehkui's defense scale changes the result, so return it even when no armor/MR
                    // reduction applied; when one did, the two stack.
                    applied = true;
                }
            }
        } catch (Throwable pehkuiMissing) {
            // Pehkui is optional; without it there is no defense scale to apply.
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
        if (attacker instanceof Mob attackerMob && attackerMob.getTarget() == victim) {
            attackerMob.setTarget(null);
        }
        if (victim instanceof Mob victimMob && victimMob.getTarget() == attacker) {
            victimMob.setTarget(null);
        }

        if (amount <= 0 || !(victim instanceof Mob) || !creraces$isServant(victim))
            return;

        Player victimOwner = CombatUtils.getRootOwner(victim);
        Player attackerOwner = CombatUtils.getRootOwner(attacker);
        if (victimOwner == null || attackerOwner == null || !victimOwner.getUUID().equals(attackerOwner.getUUID()))
            return;

        victim.heal(amount);
    }

    @Inject(method = "getVisibilityPercent", at = @At("HEAD"), cancellable = true)
    private void creraces$trueInvisibilityVisibility(Entity viewer, CallbackInfoReturnable<Double> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (ModMobEffects.isInvisible(entity)) {
            cir.setReturnValue(0.0);
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void creraces$onDeath(DamageSource source, CallbackInfo ci) {
        if (this.level().isClientSide())
            return;

        // spawnOnDeath passive
        if ((Object) this instanceof Player) {
            Player player = (Player) (Object) this;
            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    Race.Passives passives = race.passives();
                    if (passives != null) {
                        Race.EntitySpawnData data = passives.spawnOnDeath();
                        if (data != null) {
                            // tryParse so a malformed entity id in JSON can't crash the death handler
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

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true)
    private float creraces$applyShields(float amount, DamageSource source) {
        if (amount <= 0)
            return amount;
        LivingEntity victim = (LivingEntity) (Object) this;
        float currentAmount = amount;

        // Shields soak damage after armor but before absorption and health.
        @SuppressWarnings("unchecked")
        final Holder<MobEffect>[] shields =
                new Holder[] {
                ModMobEffects.SHIELD,
                ModMobEffects.AP_SHIELD,
                ModMobEffects.AD_SHIELD
        };

        for (Holder<MobEffect> shield : shields) {
            if (shield == null)
                continue;
            if (victim.hasEffect(shield)) {
                boolean blocks = false;
                if (shield == ModMobEffects.SHIELD) {
                    blocks = true;
                } else if (shield == ModMobEffects.AP_SHIELD) {
                    blocks = source.is(ModDamageTags.IS_MAGIC);
                } else if (shield == ModMobEffects.AD_SHIELD) {
                    blocks = source.is(ModDamageTags.IS_PHYSICAL);
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
                            victim.addEffect(new MobEffectInstance(
                                    shield, -1, (int) remaining - 1, false, true));
                        }
                        victim.level().playSound((Player) null, victim.getX(),
                                victim.getY(), victim.getZ(),
                                SoundEvents.ITEM_BREAK,
                                SoundSource.NEUTRAL, 1.0f, 0.8f);
                        return 0.0f;
                    } else {
                        currentAmount -= shieldHp;
                        victim.removeEffect(shield);
                        victim.level().playSound((Player) null, victim.getX(),
                                victim.getY(), victim.getZ(),
                                SoundEvents.ITEM_BREAK,
                                SoundSource.NEUTRAL, 1.0f, 0.5f);
                    }
                }
            }
        }
        return currentAmount;
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void creraces$blockNegatedEffect(
            MobEffectInstance effectInstance,
            @Nullable Entity source,
            CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof Player))
            return;
        Player player = (Player) (Object) this;
        DataUtils.getVariables(player).ifPresent(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race == null || race.passives() == null)
                return;
            List<String> negated = race.passives().immuneToPotionEffects();
            if (negated == null || negated.isEmpty())
                return;

            ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effectInstance.getEffect().value());
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

    // Since 1.21 MobEffect's removal hook no longer gets the entity, so fairy dust's flight boost is undone
    // here, whether the effect expired, washed off or was cleared.
    @Inject(method = "onEffectRemoved", at = @At("TAIL"))
    private void creraces$resetFairyDust(MobEffectInstance effect, CallbackInfo ci) {
        if (effect.getEffect().value() instanceof FairyDustEffect) {
            FairyDustEffect.resetFlightScale((LivingEntity) (Object) this);
        }
    }

    // MobType was removed in 1.21+; heal/harm potion inversion for undead now goes through
    // this method instead (see LivingEntity#isInvertedHealAndHarm, vanilla default checks
    // EntityTypeTags.INVERTED_HEALING_AND_HARM). The old aquatic/MobType.WATER branch had no
    // direct replacement - it only affected Impaling's bonus damage, which 1.21+ resolves
    // through data-driven enchantment effect components rather than an overridable method.
    @Inject(method = "isInvertedHealAndHarm", at = @At("HEAD"), cancellable = true)
    private void creraces$isInvertedHealAndHarm(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player) {
            Player player = (Player) (Object) this;
            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (vars.isUndead() || (race != null && race.isUndead())) {
                    cir.setReturnValue(true);
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
