package mc.sayda.creraces.entity;


import mc.sayda.creraces.config.CreRacesConfig;

import mc.sayda.creraces.registry.ModMobEffects;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Troll Pillar - a stationary entity summoned by the Troll's Troll Pillar
 * ability.
 * <ul>
 * <li>Immune to most damage types (fire, arrows, potions, fall, explosion,
 * etc.).</li>
 * <li>Immobile (movement speed = 0).</li>
 * <li>Pulses Troll's Curse to entities within 5 blocks at a configurable interval.</li>
 * <li>Discards itself after a configurable lifetime (default ~30 seconds).</li>
 * </ul>
 */
public class TrollPillarEntity extends TamableAnimal {

    private int ticksAlive = 0;

    public TrollPillarEntity(EntityType<TrollPillarEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(0.6f);
        this.xpReward = 0;
        this.setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.MAX_HEALTH, CreRacesConfig.ENTITY_TROLL_PILLAR_MAX_HEALTH.get())
                .add(Attributes.ARMOR, CreRacesConfig.ENTITY_TROLL_PILLAR_ARMOR.get())
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, CreRacesConfig.ENTITY_TROLL_PILLAR_FOLLOW_RANGE.get())
                .add(Attributes.KNOCKBACK_RESISTANCE, CreRacesConfig.ENTITY_TROLL_PILLAR_KNOCKBACK_RES.get());
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Immune to environmental and non-direct damage
        if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE))
            return false;
        if (source.is(DamageTypes.FALL))
            return false;
        if (source.is(DamageTypes.DROWN))
            return false;
        if (source.is(DamageTypes.LIGHTNING_BOLT))
            return false;
        if (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION))
            return false;
        if (source.is(DamageTypes.WITHER) || source.is(DamageTypes.WITHER_SKULL))
            return false;
        if (source.is(DamageTypes.DRAGON_BREATH))
            return false;
        if (source.is(DamageTypes.FALLING_ANVIL))
            return false;
        if (source.is(DamageTypes.CACTUS))
            return false;
        if (source.is(DamageTypes.TRIDENT))
            return false;
        if (source.getDirectEntity() instanceof AbstractArrow)
            return false;
        if (source.getDirectEntity() instanceof ThrownPotion)
            return false;
        if (source.getDirectEntity() instanceof AreaEffectCloud)
            return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean ignoreExplosion() {
        return true;
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.level().isClientSide())
            return;

        ticksAlive++;

        // Pulse Troll's Curse to nearby entities (Configurable interval)
        if (ticksAlive % Math.max(1, CreRacesConfig.ENTITY_TROLL_PILLAR_PULSE_INTERVAL.get()) == 0) {
            var curseEffect = ModMobEffects.TROLL_CURSE.get();
            if (curseEffect != null) {
                Vec3 center = this.position();
                LivingEntity owner = getOwner();

                // Cleanup: if owner IS a player but is no longer online/valid, discard the
                // pillar
                if (owner instanceof Player p && (!p.isAlive() || !this.level().players().contains(p))) {
                    this.discard();
                    return;
                }

                double radius = CreRacesConfig.ENTITY_TROLL_PILLAR_CURSE_RADIUS.get();
                List<LivingEntity> targets = level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    getBoundingBox().inflate(radius),
                    e -> e != this && (owner == null || mc.sayda.creraces.team.RaceTeamManager.canHurt(e, owner) || isTroll(e)));
                for (LivingEntity target : targets) {
                    target.addEffect(
                            new MobEffectInstance(curseEffect, CreRacesConfig.ENTITY_TROLL_PILLAR_CURSE_DURATION.get(),
                                    0, true, true));
                }
            }
        }

        // Self-destruct after lifetime expires
        if (ticksAlive >= CreRacesConfig.ENTITY_TROLL_PILLAR_LIFETIME_TICKS.get()) {
            this.level().playSound(null, this.blockPosition(),
                    net.minecraft.sounds.SoundEvents.STONE_BREAK,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
            this.discard();
        } else if (ticksAlive % 2 == 0) {
            double px = this.getX() + (this.random.nextDouble() - 0.5) * 1.5;
            double py = this.getY() + this.random.nextDouble() * 2.5;
            double pz = this.getZ() + (this.random.nextDouble() - 0.5) * 1.5;
            ((net.minecraft.server.level.ServerLevel) this.level()).sendParticles(
                    net.minecraft.core.particles.ParticleTypes.CLOUD, px, py, pz, 1, 0.0, 0.05, 0.0, 0.0);
            if (this.random.nextDouble() < 0.3) {
                ((net.minecraft.server.level.ServerLevel) this.level()).sendParticles(
                        net.minecraft.core.particles.ParticleTypes.SMOKE, px, py, pz, 1, 0.0, 0.02, 0.0, 0.0);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TicksAlive", this.ticksAlive);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.ticksAlive = tag.getInt("TicksAlive");
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return Ingredient.of().test(stack);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    // No interaction handling yet; always passes through.
    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        return net.minecraft.world.InteractionResult.PASS;
    }

    private boolean isTroll(net.minecraft.world.entity.LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            return mc.sayda.creraces.capability.DataUtils.getVariables(player)
                    .map(vars -> new net.minecraft.resources.ResourceLocation("creraces", "troll").equals(vars.getRace()))
                    .orElse(false);
        }
        return false;
    }
}
