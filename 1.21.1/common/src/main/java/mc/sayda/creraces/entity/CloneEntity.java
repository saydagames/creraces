package mc.sayda.creraces.entity;

import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.CastSource;
import mc.sayda.creraces.race.RaceIncidents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A copy of a player, made by the summon_clones action. It repeats its owner's movement turned,
 * mirrored and offset around a centre point, and renders as its owner (skin, armour, addons). The
 * placement is synced, so the client moves it from its own copy of the owner without lag; what it
 * does besides moving (lifetime, copied abilities, break actions) lives on the server. It keeps its
 * owner's Pehkui size, which also sizes its hitbox.
 */
@SuppressWarnings("null")
public class CloneEntity extends LivingEntity implements OwnableEntity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Vector3f> CENTER =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> BASE_YAW =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROTATION =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> MIRROR =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.BYTE);
    /** Right, up and forward, measured from the direction the owner faced when casting. */
    private static final EntityDataAccessor<Vector3f> OFFSET =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DELAY =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.INT);

    /** The furthest a clone can lag behind its owner, in ticks. */
    public static final int MAX_DELAY = 100;
    /** A clone this far from its owner vanishes; it would be leaving the loaded area. */
    private static final double MAX_DISTANCE = 96;
    private static final double DISTRACT_RANGE = 16;

    public enum Mirror { NONE, LEFT_RIGHT, FRONT_BACK }

    /** Where one clone stands relative to its owner. */
    public record Placement(float rotation, Mirror mirror, Vec3 offset, int delay) {
    }

    /** What a clone does besides moving. Server side only. */
    public record Behaviour(int lifetime, double damageMultiplier, double damageToOwner, boolean copyAbilities,
            boolean copyAttacks, boolean distract, List<ActionRegistry.RaceAction> onBreak,
            List<ActionRegistry.RaceAction> onExpire) {
    }

    /** One tick of the owner, kept for clones that echo them a little later. */
    private record Snapshot(Vec3 position, float yRot, float xRot, float headRot, float bodyRot, Pose pose) {
        static Snapshot of(Player owner) {
            return new Snapshot(owner.position(), owner.getYRot(), owner.getXRot(), owner.getYHeadRot(),
                    owner.yBodyRot, owner.getPose());
        }
    }

    private final ArrayDeque<Snapshot> history = new ArrayDeque<>();
    @Nullable
    private Behaviour behaviour;
    private int age;

    public CloneEntity(EntityType<? extends CloneEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
        builder.define(CENTER, new Vector3f());
        builder.define(BASE_YAW, 0f);
        builder.define(ROTATION, 0f);
        builder.define(MIRROR, (byte) 0);
        builder.define(OFFSET, new Vector3f());
        builder.define(DELAY, 0);
    }

    /** Sets a new clone up around {@code center}; call before adding it to the level. */
    public void setup(Player owner, Vec3 center, float baseYaw, Placement placement, Behaviour behaviour,
            float health) {
        this.entityData.set(OWNER, Optional.of(owner.getUUID()));
        this.entityData.set(CENTER, center.toVector3f());
        this.entityData.set(BASE_YAW, baseYaw);
        this.entityData.set(ROTATION, placement.rotation());
        this.entityData.set(MIRROR, (byte) placement.mirror().ordinal());
        this.entityData.set(OFFSET, placement.offset().toVector3f());
        this.entityData.set(DELAY, Mth.clamp(placement.delay(), 0, MAX_DELAY));
        this.behaviour = behaviour;

        AttributeInstance maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(Math.max(1, health));
        }
        setHealth(getMaxHealth());

        follow(owner);
        RaceIncidents.copySize(owner, this);
        // Start where it stands rather than sliding in from wherever it was created.
        this.xo = this.xOld = getX();
        this.yo = this.yOld = getY();
        this.zo = this.zOld = getZ();
        this.yRotO = getYRot();
        this.xRotO = getXRot();
        this.yBodyRotO = this.yBodyRot;
        this.yHeadRotO = this.yHeadRot;
    }

    @Override
    public void tick() {
        super.tick();
        Player owner = owner();
        boolean server = !level().isClientSide();
        if (owner == null || !owner.isAlive() || owner.isSpectator()
                || distanceToSqr(owner) > MAX_DISTANCE * MAX_DISTANCE) {
            // A client that can't see the owner keeps the server's positions until the server removes it.
            if (server) {
                vanish();
            }
            return;
        }
        follow(owner);
        if (server) {
            serverTick(owner);
        }
    }

    private void serverTick(Player owner) {
        if (behaviour == null) {
            vanish();
            return;
        }
        // Stays the owner's Pehkui size, which can change while it lives (Bat Form, potions, ...).
        RaceIncidents.copySize(owner, this);
        age++;
        if (behaviour.lifetime() >= 0 && age >= behaviour.lifetime()) {
            runAsClone(owner, behaviour.onExpire(), null);
            vanish();
            return;
        }
        if (behaviour.distract() && age % 20 == 1) {
            for (Mob mob : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(DISTRACT_RANGE),
                    mob -> mob.getTarget() == owner)) {
                mob.setTarget(this);
            }
        }
    }

    /** Moves the clone to where its owner's movement maps: turned, mirrored and offset around the centre. */
    private void follow(Player owner) {
        history.addLast(Snapshot.of(owner));
        while (history.size() > this.entityData.get(DELAY) + 1) {
            history.removeFirst();
        }
        Snapshot echo = history.peekFirst();

        Vector3f c = this.entityData.get(CENTER);
        Vec3 center = new Vec3(c.x(), c.y(), c.z());
        double baseYaw = Math.toRadians(this.entityData.get(BASE_YAW));
        // In the cast frame the owner's facing at the time is +Z, and their right hand is -X.
        Vec3 local = turn(echo.position().subtract(center), -baseYaw);
        local = switch (mirror()) {
            case LEFT_RIGHT -> new Vec3(-local.x, local.y, local.z);
            case FRONT_BACK -> new Vec3(local.x, local.y, -local.z);
            case NONE -> local;
        };
        local = turn(local, Math.toRadians(this.entityData.get(ROTATION)));
        Vector3f offset = this.entityData.get(OFFSET);
        local = local.add(-offset.x(), offset.y(), offset.z());
        Vec3 position = center.add(turn(local, baseYaw));
        setPos(position.x, position.y, position.z);

        setYRot(turnYaw(echo.yRot()));
        setXRot(echo.xRot());
        this.yHeadRot = turnYaw(echo.headRot());
        this.yBodyRot = turnYaw(echo.bodyRot());
        setPose(echo.pose());
    }

    /** Turns a vector around the vertical axis the way raising the yaw turns the view. */
    private static Vec3 turn(Vec3 vector, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(vector.x * cos - vector.z * sin, vector.y, vector.x * sin + vector.z * cos);
    }

    private float turnYaw(float yaw) {
        float base = this.entityData.get(BASE_YAW);
        float relative = yaw - base;
        relative = switch (mirror()) {
            case LEFT_RIGHT -> -relative;
            case FRONT_BACK -> 180 - relative;
            case NONE -> relative;
        };
        return Mth.wrapDegrees(base + relative + this.entityData.get(ROTATION));
    }

    private Mirror mirror() {
        Mirror[] mirrors = Mirror.values();
        return mirrors[Mth.clamp(this.entityData.get(MIRROR), 0, mirrors.length - 1)];
    }

    /** Runs actions with this clone as the source of the cast and its owner as the caster. */
    public void runAsClone(Player owner, List<ActionRegistry.RaceAction> actions, @Nullable LivingEntity target) {
        if (actions.isEmpty()) {
            return;
        }
        CastSource.run(new CastSource.Replay(this, damageMultiplier()),
                () -> ActionRegistry.runChain(actions, owner, target, null, blockPosition()));
    }

    /** A broken clone runs its break actions and vanishes: no death event, loot or kill credit. */
    @Override
    public void die(DamageSource source) {
        Player owner = owner();
        if (!level().isClientSide() && owner != null && behaviour != null) {
            runAsClone(owner, behaviour.onBreak(), source.getEntity() instanceof LivingEntity attacker ? attacker : null);
        }
        vanish();
    }

    private void vanish() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() / 2, getZ(), 8, 0.2, 0.4, 0.2, 0.02);
        }
        discard();
    }

    /** The damage_to_owner share of every hit a clone takes reaches its owner too. */
    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        super.actuallyHurt(source, amount);
        Player owner = owner();
        if (!level().isClientSide() && owner != null && behaviour != null && behaviour.damageToOwner() > 0) {
            owner.hurt(source, (float) (amount * behaviour.damageToOwner()));
        }
    }

    /** Only hits from someone break a clone; walls, falls and water can't touch it. */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return super.isInvulnerableTo(source)
                || source.getEntity() == null && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Nullable
    public Player owner() {
        UUID id = getOwnerUUID();
        return id != null ? level().getPlayerByUUID(id) : null;
    }

    public boolean isOwnedBy(Player player) {
        return player.getUUID().equals(getOwnerUUID());
    }

    @Nullable
    @Override
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER).orElse(null);
    }

    public boolean copiesAbilities() {
        return behaviour != null && behaviour.copyAbilities();
    }

    public boolean copiesAttacks() {
        return behaviour != null && behaviour.copyAttacks();
    }

    public double damageMultiplier() {
        return behaviour != null ? behaviour.damageMultiplier() : 1.0;
    }

    /** Clones go by their owner's name, so nobody can tell them apart by it. */
    @Override
    public Component getName() {
        Player owner = owner();
        return owner != null ? owner.getName() : super.getName();
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    public HumanoidArm getMainArm() {
        Player owner = owner();
        return owner != null ? owner.getMainArm() : HumanoidArm.RIGHT;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    /** Clones belong to one cast and are never written to disk. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
