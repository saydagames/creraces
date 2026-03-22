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
import net.minecraft.server.level.ServerPlayer;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        if (entity.hasEffect(mc.sayda.creraces.registry.ModMobEffects.STUNNED.get()) ||
                entity.hasEffect(mc.sayda.creraces.registry.ModMobEffects.ROOTED.get()) ||
                entity.hasEffect(mc.sayda.creraces.registry.ModMobEffects.FROZEN.get())) {
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
                            // Aquatic races suffocate slower (1/3 vanilla speed)
                            if (player.tickCount % 3 == 0) {
                                int air = player.getAirSupply();
                                player.setAirSupply(this.decreaseAirSupply(air));
                            }
                            if (player.getAirSupply() <= -20) {
                                player.setAirSupply(0);
                                player.hurt(player.damageSources().drown(), 2.0F);
                            }
                        }
                    }

                    // Water Transition Recheck
                    if (player instanceof ServerPlayer sp) {
                        boolean inWater = player.isInWater();
                        boolean wasInWater = ((IPersistentDataAccessor) sp).creraces$getPersistentData()
                                .getBoolean("creraces:was_in_water");
                        if (inWater != wasInWater) {
                            ((IPersistentDataAccessor) sp).creraces$getPersistentData()
                                    .putBoolean("creraces:was_in_water", inWater);
                            mc.sayda.creraces.race.AttributeIncidents.eikiJudgment(sp);
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
                    "[CreRaces] Failed to add custom generic attributes to LivingEntity.createLivingAttributes: {}",
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
        // Global Spirit Realm damage immunity
        if (mc.sayda.creraces.engine.SpiritMobilityHandler.isSpirit((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
            return;
        }

        if (amount <= 0) {
            cir.setReturnValue(false);
            return;
        }

        // Team check
        if (source.getEntity() instanceof Player attacker && (Object) this instanceof Player victim) {
            if (!mc.sayda.creraces.team.RaceTeamManager.canHurt(victim, attacker)) {
                cir.setReturnValue(false);
                return;
            }
        }

        // Visual Feedback for Damage Types (LoL-style)
        if (!this.level().isClientSide()) {
            net.minecraft.core.particles.SimpleParticleType pt = null;
            if (source.is(mc.sayda.creraces.registry.ModDamageTags.IS_MAGIC)) {
                pt = net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT;
            } else if (source.is(mc.sayda.creraces.registry.ModDamageTags.IS_PHYSICAL)) {
                pt = net.minecraft.core.particles.ParticleTypes.CRIT;
            } else if (source.is(mc.sayda.creraces.registry.ModDamageTags.IS_TRUE)) {
                pt = net.minecraft.core.particles.ParticleTypes.NAUTILUS;
            }

            if (pt != null) {
                ((net.minecraft.server.level.ServerLevel) this.level()).sendParticles(pt, this.getX(),
                        this.getY() + 1.0,
                        this.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
            }
        }

        // Handle being hit (Attacker is Player)
        if (source.getEntity() instanceof Player player && !player.level().isClientSide()) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    // Gain Rage on hit if the race uses it
                    if (race.resourceType() == mc.sayda.creraces.race.ResourceType.RAGE) {
                        double maxRage = player.getAttributeValue(ModAttributes.MAX_RAGE.get());
                        if (vars.getRage() < maxRage) {
                            vars.setRage(Math.min(maxRage, vars.getRage() + 5.0));
                            vars.setResourceTimer(player.level().getGameTime());
                            // Full sync: resource changed by a discrete event
                            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player, true);
                        }
                    }
                }
            });
        }

        // Handle being hit (Victim is Player)
        if ((Object) this instanceof Player player && !player.level().isClientSide()) {
            // THORNS Logic
            if (player.hasEffect(mc.sayda.creraces.registry.ModMobEffects.THORNS.get())) {
                Entity currentAttacker = source.getEntity();
                if (currentAttacker instanceof LivingEntity le && currentAttacker != player) {
                    le.hurt(player.damageSources().thorns(player), 2.0F);
                }
            }

            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    // Gain Grit on being hit if the race uses it
                    if (race.resourceType() == mc.sayda.creraces.race.ResourceType.GRIT) {
                        double maxGrit = player.getAttributeValue(ModAttributes.MAX_GRIT.get());
                        if (vars.getGrit() < maxGrit) {
                            vars.setGrit(Math.min(maxGrit, vars.getGrit() + 5.0));
                            vars.setResourceTimer(player.level().getGameTime());
                            // Full sync: resource changed by a discrete event
                            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player, true);
                        }
                    }

                    // Camouflage Interrupt
                    @SuppressWarnings("null")
                    boolean hasCamouflage = player.hasEffect(mc.sayda.creraces.registry.ModMobEffects.CAMOUFLAGE.get());
                    if (hasCamouflage) {
                        player.removeEffect(mc.sayda.creraces.registry.ModMobEffects.CAMOUFLAGE.get());
                        vars.setCooldown(new net.minecraft.resources.ResourceLocation("creraces:camouflage"), 220);
                        mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player, true);
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
                        // Return air unchanged - do not let vanilla refill it while we're draining on
                        // land
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
        // Workaround for 1.20.1: Native no_knockback tag is only fully supported in 1.21.1+
        // This mixin-based check ensures tagged damage types skip knockback.
        if (this.creraces$lastDamageSource != null && this.creraces$lastDamageSource.is(mc.sayda.creraces.registry.ModDamageTags.NO_KNOCKBACK)) {
            ci.cancel();
            return;
        }

        if ((Object) this instanceof Player player) {
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

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("TAIL"))
    private void creraces$buoyancyTravel(net.minecraft.world.phys.Vec3 travelVector, CallbackInfo ci) {
        mc.sayda.creraces.engine.AquaticMovementHandler.buoyancyTick((LivingEntity) (Object) this);
    }

    @Inject(method = "getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F", at = @At("HEAD"), cancellable = true)
    private void creraces$lolDamageReduction(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        if (amount <= 0)
            return;
        LivingEntity victim = (LivingEntity) (Object) this;
        float modifiedAmount = amount;
        boolean applied = false;

        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity leAttacker) {
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
        } else if (source.is(mc.sayda.creraces.registry.ModDamageTags.IS_MAGIC)) {
            // Environment/No-attacker magic damage (e.g. potion)
            double mr = mc.sayda.creraces.util.CombatAttributes.getMagicResist(victim);
            if (mr > 0) {
                modifiedAmount *= (float) (100.0 / (100.0 + mr));
            }
            applied = true;
        }

        // Pehkui Defense Scale
        try {
            float defScale = virtuoel.pehkui.api.ScaleTypes.DEFENSE.getScaleData(victim).getScale();
            if (defScale != 1.0f && defScale > 0) {
                modifiedAmount /= defScale;
                applied = true;
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

    @Unique
    private void creraces$spawnUndeadRemains(LivingEntity victim) {
        UndeadRemainsEntity remains = ModEntities.REMAINS_UNDEAD.get().create(victim.level());
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
        if (creraces$isServant(victim)) {
            creraces$spawnUndeadRemains(victim);
            return;
        }

        // 2. Data-Driven remains for players/mobs is now handled via OnKillTrait
        // in IncidentResolver and undead.json

        // 3. Existing spawnOnDeath passive logic for Players
        if ((Object) this instanceof Player player) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    Race.Passives passives = race.passives();
                    if (passives != null) {
                        Race.EntitySpawnData data = passives.spawnOnDeath();
                        if (data != null) {
                            for (int i = 0; i < data.count(); i++) {
                                Entity entity = BuiltInRegistries.ENTITY_TYPE
                                        .get(new ResourceLocation(data.entityType()))
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
                    float shieldHp = inst.getAmplifier() + 1.0f;
                    if (shieldHp >= currentAmount) {
                        float remaining = shieldHp - currentAmount;
                        victim.removeEffect(shield);
                        if (remaining > 0.1f) {
                            victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                    shield, -1, (int) remaining - 1, false, true));
                        }
                        victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                                net.minecraft.sounds.SoundEvents.ITEM_BREAK,
                                net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 0.8f);
                        return 0.0f;
                    } else {
                        currentAmount -= shieldHp;
                        victim.removeEffect(shield);
                        victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
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
        if (!((Object) this instanceof Player player))
            return;
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
}
