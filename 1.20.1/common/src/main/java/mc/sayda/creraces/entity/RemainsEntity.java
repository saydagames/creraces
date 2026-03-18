package mc.sayda.creraces.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class RemainsEntity extends Mob {
    private static final EntityDataAccessor<Integer> TICK_COUNT = SynchedEntityData.defineId(RemainsEntity.class,
            EntityDataSerializers.INT);

    public RemainsEntity(EntityType<? extends RemainsEntity> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setNoAi(true);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TICK_COUNT, 0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            int ticks = this.entityData.get(TICK_COUNT) + 1;
            this.entityData.set(TICK_COUNT, ticks);
            if (ticks >= mc.sayda.creraces.config.CreRacesConfig.REMAINS_DECAY_TIME.get()) {
                this.discard();
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DecayTicks", this.entityData.get(TICK_COUNT));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(TICK_COUNT, tag.getInt("DecayTicks"));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, mc.sayda.creraces.config.CreRacesConfig.REMAINS_HEALTH.get())
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }
}
