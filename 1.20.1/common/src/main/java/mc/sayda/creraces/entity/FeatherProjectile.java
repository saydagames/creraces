package mc.sayda.creraces.entity;

import mc.sayda.creraces.config.CreRacesConfig;

import mc.sayda.creraces.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

public class FeatherProjectile extends ThrowableItemProjectile {
    private static final EntityDataAccessor<ItemStack> ITEM_STACK = SynchedEntityData.defineId(FeatherProjectile.class,
            EntityDataSerializers.ITEM_STACK);
    private float damage = CreRacesConfig.ENTITY_FEATHER_DAMAGE.get().floatValue();
    private boolean isRecalling = false;
    private final java.util.Set<Integer> hitEntities = new java.util.HashSet<>();

    public FeatherProjectile(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        this.refreshDimensions();
    }

    public FeatherProjectile(Level level, LivingEntity shooter) {
        super(ModEntities.FEATHER_PROJECTILE.get(), shooter, level);
        this.refreshDimensions();
    }

    public FeatherProjectile(Level level, LivingEntity shooter, double damage) {
        super(ModEntities.FEATHER_PROJECTILE.get(), shooter, level);
        this.damage = (float) damage;
        this.refreshDimensions();
    }

    public FeatherProjectile(Level level, double x, double y, double z) {
        super(ModEntities.FEATHER_PROJECTILE.get(), x, y, z, level);
        this.refreshDimensions();
    }

    @Override
    @Nonnull
    public EntityDimensions getDimensions(@Nonnull net.minecraft.world.entity.Pose pose) {
        return EntityDimensions.fixed(0.1f, 0.1f);
    }

    public void setRecalling(boolean recalling) {
        this.isRecalling = recalling;
        if (recalling) {
            this.setNoGravity(true);
            this.setOnGround(false);
            this.hitEntities.clear(); // Reset hit list for the return journey
        }
    }

    @Override
    public void setItem(@Nonnull ItemStack stack) {
        this.getEntityData().set(ITEM_STACK, stack.copy());
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamage() {
        return damage;
    }

    @Override
    @Nonnull
    protected Item getDefaultItem() {
        return Items.FEATHER;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.getEntityData().define(ITEM_STACK, new ItemStack(getDefaultItem()));
    }

    @Override
    @Nonnull
    public ItemStack getItem() {
        return this.getEntityData().get(ITEM_STACK);
    }

    @Override
    protected void onHit(@Nonnull HitResult result) {
        if (result.getType() == HitResult.Type.ENTITY) {
            this.onHitEntity((EntityHitResult) result);
        } else if (result.getType() == HitResult.Type.BLOCK) {
            if (isRecalling) {
                return; // Ignore blocks when recalling
            }
            
            // Use the precise impact location from the hit result
            Vec3 hitLoc = result.getLocation();
            
            // Lock rotation based on current movement before stopping
            Vec3 movement = this.getDeltaMovement();
            if (movement.lengthSqr() > 0.01) {
                this.setYRot((float) (Math.atan2(movement.x, movement.z) * (180D / Math.PI)));
                this.setXRot((float) (Math.atan2(movement.y, movement.horizontalDistance()) * (180D / Math.PI)));
                this.yRotO = this.getYRot();
                this.xRotO = this.getXRot();
            }

            // Stop movement and stick to the block
            this.setDeltaMovement(Vec3.ZERO);
            this.setNoGravity(true);
            this.setOnGround(true);
            this.hitEntities.clear(); // Reset hit list so it can hit things on the return journey

            // Add a slight negative offset so it looks embedded in the block
            if (result instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                net.minecraft.core.Direction direction = blockHit.getDirection();
                // Move it slightly away from the block (0.05 is ~half a feather's width)
                this.setPos(hitLoc.x() + direction.getStepX() * 0.05,
                        hitLoc.y() + direction.getStepY() * 0.05,
                        hitLoc.z() + direction.getStepZ() * 0.05);
            } else {
                this.setPos(hitLoc.x, hitLoc.y, hitLoc.z);
            }
        }
    }

    @Override
    protected void onHitEntity(@Nonnull EntityHitResult result) {
        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity living) {
            if (hitEntities.contains(living.getId())) {
                return;
            }

            net.minecraft.world.entity.Entity owner = this.getOwner();
            if (owner instanceof LivingEntity shooter && !mc.sayda.creraces.team.RaceTeamManager.canHurt(living, shooter)) {
                return;
            }

            net.minecraft.world.damagesource.DamageSource source = this.damageSources().thrown(this, owner);
            if (source != null) {
                living.hurt(source, this.damage);
                hitEntities.add(living.getId());
            }
            // Passthrough: NO discard() here
        }
    }

    @Override
    public void playerTouch(@Nonnull Player player) {
        if (!this.level().isClientSide && (this.onGround() || isRecalling)) {
            java.util.Optional<mc.sayda.creraces.capability.IPlayerVariables> vars = mc.sayda.creraces.capability.DataUtils
                    .getVariables(player);
            if (vars.isPresent()
                    && mc.sayda.creraces.race.RaceRegistry.HARPY.equals(vars.get().getRace())) {
                if (player.getInventory().add(getItem())) {
                    this.discard();
                }
            }
        }
    }

    @Override
    public void tick() {
        if (isRecalling) {
            net.minecraft.world.entity.Entity owner = this.getOwner();
            if (owner == null || !owner.isAlive() || owner.level() != this.level()) {
                isRecalling = false;
                this.setNoGravity(false);
                return;
            }

            Vec3 target = owner.position().add(0, 1.0, 0); // Aim for waist
            Vec3 dir = target.subtract(this.position()).normalize();
            double speed = 1.2; // Return speed (increased for snappiness)
            Vec3 movement = dir.scale(speed);
            this.setDeltaMovement(movement);
            
            // Use direct positioning for snappy, block-ignoring movement
            this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
            
            // Check for collision with entities manually
            AABB search = this.getBoundingBox().inflate(0.5);
            for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, search)) {
                if (e != owner) {
                    this.onHitEntity(new EntityHitResult(e));
                }
            }

            if (this.position().distanceToSqr(target) < 1.5) {
                if (owner instanceof Player p) {
                    if (p.getInventory().add(getItem())) {
                        this.discard();
                    }
                } else {
                    this.discard();
                }
            }
            return;
        }

        if (!this.onGround()) {
            super.tick();
            if (!this.isNoGravity()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0,
                        -CreRacesConfig.ENTITY_FEATHER_GRAVITY.get(), 0)); // Subtle floaty fall
            }
        } else {
            // Stay stuck to ground/surface
            this.setDeltaMovement(Vec3.ZERO);
        }
    }
}
