package mc.sayda.creraces.entity;

import mc.sayda.creraces.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FloatingMoteEntity extends PathfinderMob {

    private double driftVX = 0;
    private double driftVY = 0;
    private double driftVZ = 0;
    private int driftTimer = 0;

    public FloatingMoteEntity(EntityType<? extends FloatingMoteEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(false);
        return nav;
    }

    @Override
    public boolean shouldDiscardFriction() { return true; }

    /**
     * The constructor's setNoGravity(true) does not survive /summon: Entity.load() assigns
     * NoGravity straight from the tag, and a summon without that tag reads false. Motes always
     * float, so answer that here rather than depending on a saved flag.
     */
    @Override
    public boolean isNoGravity() {
        return true;
    }

    /** Motes drift instead of pathfinding, so water is avoided by steering, not by a goal. */
    private boolean waterUnderfoot() {
        net.minecraft.core.BlockPos pos = blockPosition();
        return level().getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                || level().getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.WATER);
    }

    @Override
    public void tick() {
        super.tick();

        if (--driftTimer <= 0) {
            driftTimer = 80 + random.nextInt(80);
            driftVX = (random.nextDouble() - 0.5) * 0.16;
            driftVY = (random.nextDouble() - 0.5) * 0.08;
            driftVZ = (random.nextDouble() - 0.5) * 0.16;
        }

        if (waterUnderfoot()) {
            // Climb and reverse course rather than drifting out over the water.
            driftVY = 0.06;
            driftVX = -driftVX;
            driftVZ = -driftVZ;
        }

        Vec3 motion = getDeltaMovement();
        double newX = motion.x + (driftVX - motion.x) * 0.06;
        double newY = motion.y + (driftVY - motion.y) * 0.06 + Math.sin(tickCount * 0.05) * 0.005;
        double newZ = motion.z + (driftVZ - motion.z) * 0.06;
        setDeltaMovement(newX, newY, newZ);

        if (level().isClientSide && level().random.nextInt(12) == 0) {
            level().addParticle(ModParticles.VEIL_EMBER.get(),
                    getX() + (level().random.nextDouble() - 0.5) * 0.15,
                    getY(),
                    getZ() + (level().random.nextDouble() - 0.5) * 0.15,
                    0, 0.005, 0);
        }
    }

    @Override
    public void checkDespawn() {
        if (!level().isClientSide && level().isDay()) {
            this.discard();
            return;
        }
        super.checkDespawn();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (super.isInvulnerableTo(source)) return true;
        return !source.is(DamageTypes.MAGIC)
            && !source.is(DamageTypes.THORNS)
            && !source.is(DamageTypes.INDIRECT_MAGIC);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    public MobType getMobType() { return MobType.UNDEFINED; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public SoundEvent getHurtSound(DamageSource source) { return null; }

    @Override
    public SoundEvent getDeathSound() { return null; }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); }
}
