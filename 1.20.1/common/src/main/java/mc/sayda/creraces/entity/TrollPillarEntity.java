package mc.sayda.creraces.entity;

import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.registry.ModMobEffects;
import mc.sayda.creraces.team.RaceTeamManager;
import mc.sayda.creraces.util.RaceUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Troll Pillar - a stationary entity summoned by the Troll's Troll Pillar ability.
 * <ul>
 * <li>Immune to most damage types (fire, arrows, potions, fall, explosion, etc.).</li>
 * <li>Immobile (movement speed = 0).</li>
 * <li>Pulses Troll's Curse to entities within the configured radius at a configurable interval.</li>
 * <li>Discards itself after a configurable lifetime (default 30 seconds).</li>
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
    public boolean hurt(DamageSource source, float amount) {
        if (ignoresDamageFrom(source)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    /** Environmental and indirect damage the pillar ignores; only direct hits wear it down. */
    private static boolean ignoresDamageFrom(DamageSource source) {
        return source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.FALL)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.LIGHTNING_BOLT)
                || source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)
                || source.is(DamageTypes.WITHER) || source.is(DamageTypes.WITHER_SKULL)
                || source.is(DamageTypes.DRAGON_BREATH)
                || source.is(DamageTypes.FALLING_ANVIL)
                || source.is(DamageTypes.CACTUS)
                || source.is(DamageTypes.TRIDENT)
                || source.getDirectEntity() instanceof AbstractArrow
                || source.getDirectEntity() instanceof ThrownPotion
                || source.getDirectEntity() instanceof AreaEffectCloud;
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

        if (ticksAlive % Math.max(1, CreRacesConfig.ENTITY_TROLL_PILLAR_PULSE_INTERVAL.get()) == 0) {
            if (!pulseCurse()) {
                return;
            }
        }

        if (ticksAlive >= CreRacesConfig.ENTITY_TROLL_PILLAR_LIFETIME_TICKS.get()) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.NEUTRAL, 1.0f, 1.0f);
            this.discard();
        } else if (ticksAlive % 2 == 0) {
            spawnMistParticles();
        }
    }

    /**
     * Curses everything in range the owner may hurt, plus players of the owner's own race (Troll's Curse
     * speeds trolls up instead of slowing them). Returns false if the pillar discarded itself.
     */
    private boolean pulseCurse() {
        var curseEffect = ModMobEffects.TROLL_CURSE.get();
        if (curseEffect == null) {
            return true;
        }
        LivingEntity owner = getOwner();
        // A player owner who died or logged out takes the pillar with them.
        if (owner instanceof Player p && (!p.isAlive() || !this.level().players().contains(p))) {
            this.discard();
            return false;
        }

        ResourceLocation ownerRace = owner instanceof Player ownerPlayer ? RaceUtils.raceOf(ownerPlayer) : null;
        double radius = CreRacesConfig.ENTITY_TROLL_PILLAR_CURSE_RADIUS.get();
        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(radius),
                e -> e != this && (owner == null || RaceTeamManager.canHurt(e, owner) || isOfRace(e, ownerRace)));
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(curseEffect,
                    CreRacesConfig.ENTITY_TROLL_PILLAR_CURSE_DURATION.get(), 0, true, true));
        }
        return true;
    }

    private void spawnMistParticles() {
        ServerLevel serverLevel = (ServerLevel) this.level();
        double px = this.getX() + (this.random.nextDouble() - 0.5) * 1.5;
        double py = this.getY() + this.random.nextDouble() * 2.5;
        double pz = this.getZ() + (this.random.nextDouble() - 0.5) * 1.5;
        serverLevel.sendParticles(ParticleTypes.CLOUD, px, py, pz, 1, 0.0, 0.05, 0.0, 0.0);
        if (this.random.nextDouble() < 0.3) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, px, py, pz, 1, 0.0, 0.02, 0.0, 0.0);
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
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    // Returning PASS keeps Animal's feeding/breeding interaction off the pillar entirely.
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    private static boolean isOfRace(LivingEntity entity, @Nullable ResourceLocation race) {
        return race != null && entity instanceof Player player && race.equals(RaceUtils.raceOf(player));
    }
}
