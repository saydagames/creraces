package mc.sayda.creraces.entity;

import mc.sayda.creraces.config.CreRacesConfig;

import mc.sayda.creraces.registry.ModEntities;
import mc.sayda.creraces.registry.ModMobEffects;
import mc.sayda.creraces.team.RaceTeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("null")
public class TornadoEntity extends TamableAnimal {
    private int ticksAlive = 0;

    public TornadoEntity(EntityType<? extends TornadoEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(0.6f);
        this.xpReward = 0;
        this.setNoAi(false);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setInvulnerable(true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, CreRacesConfig.ENTITY_TORNADO_MOVEMENT_SPEED.get())
                .add(Attributes.MAX_HEALTH, CreRacesConfig.ENTITY_TORNADO_HEALTH.get())
                .add(Attributes.ARMOR, CreRacesConfig.ENTITY_TORNADO_ARMOR.get())
                .add(Attributes.ATTACK_DAMAGE, CreRacesConfig.ENTITY_TORNADO_ATTACK_DAMAGE.get())
                .add(Attributes.FOLLOW_RANGE, CreRacesConfig.ENTITY_TORNADO_FOLLOW_RANGE.get())
                .add(Attributes.FLYING_SPEED, CreRacesConfig.ENTITY_TORNADO_FLYING_SPEED.get());
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.FALL) ||
                source.is(DamageTypes.CACTUS) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.LIGHTNING_BOLT)
                ||
                source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION) ||
                source.is(DamageTypes.DRAGON_BREATH) || source.is(DamageTypes.WITHER)) {
            return false;
        }
        if (source.getDirectEntity() instanceof Player) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean ignoreExplosion() {
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.level().isClientSide())
            return;

        ticksAlive++;

        // Hurricane logic: pull and dizzy
        applyHurricaneEffects();

        if (ticksAlive > CreRacesConfig.ENTITY_TORNADO_LIFETIME_TICKS.get()) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.NEUTRAL, 1.0f, 2.0f);
            this.discard();
        }
    }

    private void applyHurricaneEffects() {
        Vec3 center = this.position();
        float radius = CreRacesConfig.ENTITY_TORNADO_RADIUS.get();
        LivingEntity owner = this.getOwner();
        net.minecraft.resources.ResourceLocation dizzinessId = new net.minecraft.resources.ResourceLocation("creraces",
                "dizziness");

        AABB area = new AABB(center, center).inflate(radius);

        // Reflect Projectiles
        List<net.minecraft.world.entity.projectile.Projectile> projectiles = this.level().getEntitiesOfClass(
                net.minecraft.world.entity.projectile.Projectile.class, area,
                p -> {
                    if (p.getDeltaMovement().lengthSqr() <= 0)
                        return false;
                    net.minecraft.world.entity.Entity pOwner = p.getOwner();
                    return owner == null || (pOwner instanceof LivingEntity le && RaceTeamManager.canHurt(le, owner))
                            || pOwner == null;
                });
        for (net.minecraft.world.entity.projectile.Projectile p : projectiles) {
            Vec3 away = p.position().subtract(center).normalize().scale(0.5);
            p.setDeltaMovement(p.getDeltaMovement().add(away));
            p.hasImpulse = true;
        }

        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
                entity -> entity != this && entity != owner && (owner == null || RaceTeamManager.canHurt(entity, owner))
                        && !mc.sayda.creraces.util.RaceUtils.isImmuneToEffect(entity, dizzinessId));

        for (LivingEntity target : targets) {
            // Apply Dizziness
            target.addEffect(new MobEffectInstance(ModMobEffects.DIZZINESS.get(),
                    CreRacesConfig.ENTITY_TORNADO_DIZZINESS_DURATION.get(), 0, false, false));

            // Subtle pull towards center
            Vec3 delta = center.subtract(target.position());
            if (delta.length() > 0.5) {
                target.setDeltaMovement(target.getDeltaMovement()
                        .add(delta.normalize().scale(CreRacesConfig.ENTITY_TORNADO_PULL_FORCE.get())));
            }
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return ModEntities.TORNADO.get().create(level);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return Ingredient.of().test(stack);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canCollideWith(net.minecraft.world.entity.Entity entity) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }
}
