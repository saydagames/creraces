package mc.sayda.creraces.entity;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.registry.ModEntities;
import mc.sayda.creraces.team.RaceTeamManager;
import mc.sayda.creraces.util.RaceUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A thrown feather that pierces through entities, sticks where it lands, and can be recalled
 * back to its owner. Players of the thrower's race can also walk over a landed feather to pick it up.
 */
public class FeatherProjectile extends ThrowableItemProjectile {
    private static final EntityDataAccessor<ItemStack> ITEM_STACK = SynchedEntityData.defineId(FeatherProjectile.class,
            EntityDataSerializers.ITEM_STACK);
    private static final ResourceKey<DamageType> FEATHER_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(CreRaces.MODID, "feather"));
    private static final double RECALL_SPEED = 1.2;
    private static final String THROWER_RACE_KEY = "ThrowerRace";

    private float damage = CreRacesConfig.ENTITY_FEATHER_DAMAGE.get().floatValue();
    private boolean recalling = false;
    /** Entity ids already hit on the current flight, so a piercing feather damages each one once. */
    private final Set<Integer> hitEntities = new HashSet<>();
    @Nullable
    private ResourceLocation throwerRace;

    public FeatherProjectile(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        this.refreshDimensions();
    }

    public FeatherProjectile(Level level, LivingEntity shooter) {
        super(ModEntities.FEATHER_PROJECTILE.get(), shooter, level);
        this.refreshDimensions();
        if (shooter instanceof Player player) {
            this.throwerRace = RaceUtils.raceOf(player);
        }
    }

    @Override
    @Nonnull
    public EntityDimensions getDimensions(@Nonnull Pose pose) {
        return Objects.requireNonNull(EntityDimensions.fixed(0.1f, 0.1f));
    }

    public void setRecalling(boolean recalling) {
        this.recalling = recalling;
        if (recalling) {
            this.setNoGravity(true);
            this.setOnGround(false);
            this.hitEntities.clear();
        }
    }

    @Override
    public void setItem(@Nonnull ItemStack stack) {
        this.getEntityData().set(ITEM_STACK, stack.copy());
    }

    public void setDamage(float damage) {
        this.damage = damage;
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
        } else if (result.getType() == HitResult.Type.BLOCK && !recalling) {
            stickInBlock(result);
        }
    }

    private void stickInBlock(HitResult result) {
        Vec3 hitLoc = result.getLocation();

        // Lock rotation to the flight direction before the motion is zeroed.
        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() > 0.01) {
            this.setYRot((float) (Math.atan2(movement.x, movement.z) * (180D / Math.PI)));
            this.setXRot((float) (Math.atan2(movement.y, movement.horizontalDistance()) * (180D / Math.PI)));
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }

        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
        this.setOnGround(true);
        // A recall can hit the same entities again on the way back.
        this.hitEntities.clear();

        if (result instanceof BlockHitResult blockHit) {
            // Nudge it out of the face by about half a feather's width so it looks embedded, not buried.
            Direction direction = blockHit.getDirection();
            this.setPos(hitLoc.x() + direction.getStepX() * 0.05,
                    hitLoc.y() + direction.getStepY() * 0.05,
                    hitLoc.z() + direction.getStepZ() * 0.05);
        } else {
            this.setPos(hitLoc.x, hitLoc.y, hitLoc.z);
        }
    }

    @Override
    protected void onHitEntity(@Nonnull EntityHitResult result) {
        if (this.level().isClientSide || !(result.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (hitEntities.contains(living.getId())) {
            return;
        }
        if (!(this.getOwner() instanceof LivingEntity shooter) || !RaceTeamManager.canHurt(living, shooter)) {
            return;
        }

        var damageTypes = this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        DamageSource source = new DamageSource(damageTypes.getHolderOrThrow(FEATHER_DAMAGE), this, shooter);
        living.hurt(source, this.damage);
        living.knockback(0.4D, this.getDeltaMovement().x, this.getDeltaMovement().z);
        // No discard: feathers pierce and keep flying.
        hitEntities.add(living.getId());
    }

    @Override
    public void playerTouch(@Nonnull Player player) {
        if (this.level().isClientSide || !(this.onGround() || recalling)) {
            return;
        }
        // Feathers saved before the thrower's race was recorded fall back to the owner's current race
        if (throwerRace == null && this.getOwner() instanceof Player owner) {
            throwerRace = RaceUtils.raceOf(owner);
        }
        boolean sameRace = throwerRace != null && throwerRace.equals(RaceUtils.raceOf(player));
        if (sameRace && player.getInventory().add(getItem())) {
            this.discard();
        }
    }

    @Override
    public void addAdditionalSaveData(@Nonnull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // Vanilla only saves its own item slot, which this class bypasses; loading already routes
        // "Item" back through setItem, so writing it here is all the round trip needs.
        tag.put("Item", getItem().save(new CompoundTag()));
        if (throwerRace != null) {
            tag.putString(THROWER_RACE_KEY, throwerRace.toString());
        }
    }

    @Override
    public void readAdditionalSaveData(@Nonnull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(THROWER_RACE_KEY, Tag.TAG_STRING)) {
            throwerRace = ResourceLocation.tryParse(tag.getString(THROWER_RACE_KEY));
        }
    }

    @Override
    public void tick() {
        if (recalling) {
            tickRecall();
            return;
        }

        if (!this.onGround()) {
            super.tick();
            if (!this.isNoGravity()) {
                // Extra configurable pull on top of the throwable's own gravity.
                this.setDeltaMovement(this.getDeltaMovement().add(0,
                        -CreRacesConfig.ENTITY_FEATHER_GRAVITY.get(), 0));
            }
        } else {
            // Stay stuck to the surface.
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    /** Flies straight back to the owner through blocks, hitting entities on the way. */
    private void tickRecall() {
        Entity owner = this.getOwner();
        if (owner == null || !owner.isAlive() || owner.level() != this.level()) {
            recalling = false;
            this.setNoGravity(false);
            return;
        }

        Vec3 target = Objects.requireNonNull(owner.position().add(0, 1.0, 0)); // aim for the waist
        Vec3 dir = Objects.requireNonNull(target.subtract(this.position()).normalize());
        Vec3 movement = Objects.requireNonNull(dir.scale(RECALL_SPEED));
        this.setDeltaMovement(movement);
        // Moved directly rather than through super.tick(), so the return trip ignores blocks.
        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);

        // Without super.tick() there is no hit detection, so check for entities by hand.
        AABB search = Objects.requireNonNull(this.getBoundingBox().inflate(0.5));
        for (LivingEntity e : Objects.requireNonNull(this.level().getEntitiesOfClass(LivingEntity.class, search))) {
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
    }
}
