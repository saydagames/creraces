package mc.sayda.creraces.engine;

import mc.sayda.creraces.ability.Ability;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.entity.CloneEntity;
import mc.sayda.creraces.team.RaceTeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Keeps track of each player's clones and has them repeat what the player does. Server side. */
@SuppressWarnings("null")
public final class CloneManager {
    /** Each owner's live clones. Weak, so clones dropped with their chunk don't linger here. */
    private static final Map<UUID, Set<CloneEntity>> CLONES = new HashMap<>();
    /** The reach of a clone's copied melee swing, as a player's. */
    private static final double MELEE_REACH = 3.0;
    /** How far a clone looks for the block it aims at, as AbilityIncidents does for the player. */
    private static final double BLOCK_REACH = 5.0;

    private CloneManager() {
    }

    /** Called once a new clone is in the level. */
    public static void track(CloneEntity clone, Player owner) {
        CLONES.computeIfAbsent(owner.getUUID(), id -> Collections.newSetFromMap(new WeakHashMap<>())).add(clone);
    }

    /** The owner's clones that are still around; costs nothing for a player without any. */
    public static List<CloneEntity> clonesOf(Player owner) {
        Set<CloneEntity> clones = CLONES.get(owner.getUUID());
        if (clones == null) {
            return List.of();
        }
        clones.removeIf(clone -> clone.isRemoved() || !clone.isAlive());
        if (clones.isEmpty()) {
            CLONES.remove(owner.getUUID());
            return List.of();
        }
        return new ArrayList<>(clones);
    }

    public static void removeClones(Player owner) {
        clonesOf(owner).forEach(Entity::discard);
    }

    /** The owner cast an ability: each clone that copies abilities casts it too, from where it stands. */
    public static void replayAbility(Player owner, Ability ability, @Nullable AbilitySlot slot) {
        if (!ability.copiedByClones() || ability.onActivate() == null) {
            return;
        }
        for (CloneEntity clone : clonesOf(owner)) {
            if (clone.copiesAbilities()) {
                BlockPos aimed = aimedBlock(clone);
                CastSource.run(new CastSource.Replay(clone, clone.damageMultiplier()),
                        () -> ActionRegistry.runChain(ability.onActivate(), owner, null, slot, aimed));
            }
        }
    }

    /**
     * The owner swung at an entity: each clone that copies attacks hits whatever is in front of it,
     * for the owner's attack damage at the swing's charge. The owner deals the hit, so it counts as
     * theirs (on_hit traits, crits, kill credit).
     */
    public static void copyAttack(Player owner) {
        float charge = owner.getAttackStrengthScale(0.5f);
        double damage = owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.2 + charge * charge * 0.8);
        for (CloneEntity clone : clonesOf(owner)) {
            if (!clone.copiesAttacks()) {
                continue;
            }
            clone.swing(InteractionHand.MAIN_HAND, true);
            LivingEntity victim = meleeTarget(owner, clone);
            if (victim != null) {
                victim.hurt(owner.damageSources().playerAttack(owner), (float) (damage * clone.damageMultiplier()));
                victim.knockback(0.4, clone.getX() - victim.getX(), clone.getZ() - victim.getZ());
            }
        }
    }

    /** The first entity in a clone's reach it may hit: not its owner, nor anyone the owner can't hurt. */
    @Nullable
    private static LivingEntity meleeTarget(Player owner, CloneEntity clone) {
        Vec3 eye = clone.getEyePosition();
        Vec3 look = clone.getViewVector(1f);
        Vec3 end = eye.add(look.scale(MELEE_REACH));
        AABB swept = clone.getBoundingBox().expandTowards(look.scale(MELEE_REACH)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(clone.level(), clone, eye, end, swept,
                entity -> entity instanceof LivingEntity living && living != owner && living.isAlive()
                        && RaceTeamManager.canHurt(living, owner));
        return hit != null ? (LivingEntity) hit.getEntity() : null;
    }

    /** The space in front of the block face a clone looks at, like an ability's "block" position. */
    @Nullable
    private static BlockPos aimedBlock(CloneEntity clone) {
        Vec3 eye = clone.getEyePosition();
        BlockHitResult hit = clone.level().clip(new ClipContext(eye, eye.add(clone.getViewVector(1f).scale(BLOCK_REACH)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, clone));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos().relative(hit.getDirection()) : null;
    }
}
