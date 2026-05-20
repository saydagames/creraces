package mc.sayda.creraces.entity;

import mc.sayda.creraces.config.CreRacesConfig;

import mc.sayda.creraces.registry.ModEntities;
import mc.sayda.creraces.registry.ModMobEffects;
import mc.sayda.creraces.util.IPersistentDataAccessor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Poison Emitter - a stationary entity summoned by the Ratkin.
 * Pulses Ratvenom to enemies within 5.5 blocks.
 * Stacks increase every 120 ticks (0.2 of lifetime).
 */
@SuppressWarnings("null")
public class PoisonEmitterEntity extends TamableAnimal {

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
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.level().isClientSide())
            return;

        ticksAlive++;
        double lifetime = CreRacesConfig.ENTITY_POISON_EMITTER_LIFETIME_TICKS.get();
        double progress = (double) ticksAlive / lifetime;

        // Legacy Stacks logic (0 to 4 based on lifetime progress)
        int stacks = 0;
        if (progress >= 0.8) stacks = 4;
        else if (progress >= 0.6) stacks = 3;
        else if (progress >= 0.4) stacks = 2;
        else if (progress >= 0.2) stacks = 1;

        // Sky visibility bonus (+1 amplifier)
        if (this.level().canSeeSky(this.blockPosition())) {
            stacks += 1;
        }

        // Particle VFX every tick (server side send to all)
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(mc.sayda.creraces.registry.ModParticles.POISON_EMITTER.get(),
                    this.getX(), this.getY(), this.getZ(),
                    15, 3.0, 3.0, 3.0, 0.0);
        }

        // Pulse Ratvenom to nearby entities
        var venomEffect = ModMobEffects.RAT_VENOM.get();
        if (venomEffect != null) {
            Vec3 center = this.position();
            LivingEntity owner = getOwner();

            // Cleanup: if owner IS a player but is no longer online/valid, discard the emitter
            if (owner instanceof Player p && (!p.isAlive() || !this.level().players().contains(p))) {
                this.discard();
                return;
            }

            net.minecraft.resources.ResourceLocation venomId = new net.minecraft.resources.ResourceLocation(
                    "creraces", "rat_venom");
            List<LivingEntity> nearby = this.level().getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(center, center).inflate(CreRacesConfig.ENTITY_POISON_EMITTER_RADIUS.get()),
                    e -> e != this && e != owner && (owner == null || mc.sayda.creraces.team.RaceTeamManager.canHurt(e, owner))
                            && !mc.sayda.creraces.util.RaceUtils.isImmuneToEffect(e, venomId));

            for (LivingEntity target : nearby) {
                // Apply source tag for attribution if owner is present
                if (owner != null && target instanceof IPersistentDataAccessor accessor) {
                    CompoundTag data = accessor.creraces$getPersistentData();
                    data.putString("creraces:source", owner.getUUID().toString());
                }

                target.addEffect(new MobEffectInstance(venomEffect, 102, stacks, true, true));
            }
        }

        // Self-destruct after lifetime expires
        if (ticksAlive >= lifetime) {
            this.level().playSound(null, this.blockPosition(),
                    net.minecraft.sounds.SoundEvents.STONE_BREAK,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
            this.discard();
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return Ingredient.of().test(stack);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return ModEntities.POISON_EMITTER.get().create(level);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean canCollideWith(net.minecraft.world.entity.Entity entity) {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }
}
