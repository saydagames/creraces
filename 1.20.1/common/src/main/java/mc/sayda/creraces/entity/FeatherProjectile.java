package mc.sayda.creraces.entity;

import mc.sayda.creraces.config.CreRacesConfig;

import mc.sayda.creraces.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nonnull;

public class FeatherProjectile extends ThrowableItemProjectile {
    private static final EntityDataAccessor<ItemStack> ITEM_STACK = SynchedEntityData.defineId(FeatherProjectile.class,
            EntityDataSerializers.ITEM_STACK);
    private float damage = CreRacesConfig.ENTITY_FEATHER_DAMAGE.get().floatValue();

    public FeatherProjectile(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public FeatherProjectile(Level level, LivingEntity shooter) {
        super(ModEntities.FEATHER_PROJECTILE.get(), shooter, level);
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
            super.onHit(result);
        } else if (result.getType() == HitResult.Type.BLOCK) {
            // Stop movement and stick to the block
            this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            this.setNoGravity(true);
            this.setOnGround(true);

            // Add a slight offset so it's not inside the block
            if (result instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                net.minecraft.core.Direction direction = blockHit.getDirection();
                this.setPos(this.getX() + direction.getStepX() * 0.05,
                        this.getY() + direction.getStepY() * 0.05,
                        this.getZ() + direction.getStepZ() * 0.05);
            }
        }
    }

    @Override
    protected void onHitEntity(@Nonnull EntityHitResult result) {
        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity living) {
            net.minecraft.world.entity.Entity owner = this.getOwner();
            if (owner instanceof LivingEntity shooter && !mc.sayda.creraces.team.RaceTeamManager.canHurt(living, shooter)) {
                this.discard();
                return;
            }

            net.minecraft.world.damagesource.DamageSource source = this.damageSources().thrown(this, owner);
            if (source != null) {
                living.hurt(source, this.damage);
            }
            this.discard();
        }
    }

    @Override
    public void playerTouch(@Nonnull Player player) {
        if (!this.level().isClientSide && this.onGround()) {
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
        if (!this.onGround()) {
            super.tick();
            if (!this.isNoGravity()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0,
                        -CreRacesConfig.ENTITY_FEATHER_GRAVITY.get(), 0)); // Subtle floaty fall
            }
        } else {
            // Stay stuck to ground/surface
            this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
    }
}
