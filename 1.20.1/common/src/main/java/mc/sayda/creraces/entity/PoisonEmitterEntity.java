package mc.sayda.creraces.entity;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.effect.SourceTrackedEffect;
import mc.sayda.creraces.registry.ModMobEffects;
import mc.sayda.creraces.registry.ModParticles;
import mc.sayda.creraces.team.RaceTeamManager;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import mc.sayda.creraces.util.RaceUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Poison Emitter - a stationary entity summoned by the Ratkin.
 * Pulses Ratvenom to enemies within the configured radius (5.5 blocks by default).
 * Stacks increase every 0.2 of the emitter's lifetime.
 */
@SuppressWarnings("null")
public class PoisonEmitterEntity extends TamableAnimal {

    private static final ResourceLocation RAT_VENOM_ID = new ResourceLocation(CreRaces.MODID, "rat_venom");

    private int ticksAlive = 0;

    public PoisonEmitterEntity(EntityType<? extends PoisonEmitterEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(0.6f);
        this.xpReward = 0;
        this.setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.MAX_HEALTH, CreRacesConfig.ENTITY_POISON_EMITTER_HEALTH.get())
                .add(Attributes.ARMOR, CreRacesConfig.ENTITY_POISON_EMITTER_ARMOR.get())
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, CreRacesConfig.ENTITY_POISON_EMITTER_FOLLOW_RANGE.get())
                .add(Attributes.KNOCKBACK_RESISTANCE, CreRacesConfig.ENTITY_POISON_EMITTER_KNOCKBACK_RES.get());
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.level().isClientSide())
            return;

        ticksAlive++;
        pulseVenom(this, ticksAlive);
    }

    /**
     * One server tick of the venom aura, shared with PoisonEmitterMobileEntity: particles, a
     * Ratvenom pulse on every enemy in range, and self-destruct once the lifetime runs out.
     */
    static void pulseVenom(TamableAnimal emitter, int ticksAlive) {
        Level level = emitter.level();
        double lifetime = Math.max(1, CreRacesConfig.ENTITY_POISON_EMITTER_LIFETIME_TICKS.get());
        int stacks = venomStacks(ticksAlive / lifetime);
        // Open sky strengthens the venom by one more stack.
        if (level.canSeeSky(emitter.blockPosition())) {
            stacks += 1;
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ModParticles.POISON_EMITTER.get(),
                    emitter.getX(), emitter.getY(), emitter.getZ(),
                    15, 3.0, 3.0, 3.0, 0.0);
        }

        var venomEffect = ModMobEffects.RAT_VENOM.get();
        if (venomEffect != null) {
            LivingEntity owner = emitter.getOwner();
            // A player owner who died or logged out takes the emitter with them.
            if (owner instanceof Player p && (!p.isAlive() || !level.players().contains(p))) {
                emitter.discard();
                return;
            }

            Vec3 center = emitter.position();
            List<LivingEntity> nearby = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(center, center).inflate(CreRacesConfig.ENTITY_POISON_EMITTER_RADIUS.get()),
                    e -> e != emitter && e != owner && (owner == null || RaceTeamManager.canHurt(e, owner))
                            && !RaceUtils.isImmuneToEffect(e, RAT_VENOM_ID));

            for (LivingEntity target : nearby) {
                // Lets the venom's damage be credited to the owner.
                if (owner != null && target instanceof IPersistentDataAccessor accessor) {
                    CompoundTag data = accessor.creraces$getPersistentData();
                    data.putString(SourceTrackedEffect.SOURCE_KEY, owner.getUUID().toString());
                }

                // Only apply or upgrade: re-applying every tick would keep resetting the duration,
                // so RatVenomEffect's every-10-ticks damage would never line up.
                MobEffectInstance existing = target.getEffect(venomEffect);
                if (existing == null || existing.getAmplifier() < stacks) {
                    target.addEffect(new MobEffectInstance(venomEffect, 100, stacks, true, true));
                }
            }
        }

        if (ticksAlive >= lifetime) {
            level.playSound(null, emitter.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.NEUTRAL, 1.0f, 1.0f);
            emitter.discard();
        }
    }

    /** One stack per fifth of the lifetime elapsed, 0 to 4. */
    private static int venomStacks(double lifetimeProgress) {
        if (lifetimeProgress >= 0.8) return 4;
        if (lifetimeProgress >= 0.6) return 3;
        if (lifetimeProgress >= 0.4) return 2;
        if (lifetimeProgress >= 0.2) return 1;
        return 0;
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

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }
}
