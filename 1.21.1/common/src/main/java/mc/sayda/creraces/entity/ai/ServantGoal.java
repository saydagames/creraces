package mc.sayda.creraces.entity.ai;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.item.CommandingStaffItem;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Carries out the owner's staff command for a claimed servant: follow the owner, walk to the
 * commanded spot, or hold that spot in attack mode while no target is picked. Free mode leaves
 * the mob's own AI in charge, and chasing a picked target is ServantAttackGoal's job.
 */
public class ServantGoal extends Goal {
    private static final double LEASH_DISTANCE_SQ = 32.0 * 32.0;
    private static final double TELEPORT_DISTANCE_SQ = 16.0 * 16.0;
    private static final double FOLLOW_DISTANCE_SQ = 4.0 * 4.0;
    private static final double STOP_DISTANCE_SQ = 2.0 * 2.0;
    private static final int PATH_RECALC_TICKS = 10;

    private final Mob mob;
    private Player owner;
    private int timeToRecalcPath;

    public ServantGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.TARGET));
    }

    /**
     * The player this servant answers to, or null if it is unclaimed, the owner is not in the
     * mob's level, or the stored UUID is unreadable.
     */
    @Nullable
    static Player findOwner(Mob mob) {
        CompoundTag data = ((IPersistentDataAccessor) mob).creraces$getPersistentData();
        if (!data.contains(CommandingStaffItem.SERVANT_OF)) return null;
        UUID ownerId;
        try {
            ownerId = DataUtils.loadUUID(data, CommandingStaffItem.SERVANT_OF);
        } catch (IllegalArgumentException e) {
            // An int array of the wrong length; treat the mob as unowned instead of breaking its AI.
            CreRaces.LOGGER.debug("Unreadable servant owner on {}: {}", mob, e.getMessage());
            return null;
        }
        return ownerId != null ? mob.level().getPlayerByUUID(ownerId) : null;
    }

    @Override
    public boolean canUse() {
        Player player = findOwner(mob);
        ItemStack staff = player != null ? CommandingStaffItem.findHeldStaff(player) : null;
        if (staff == null) return false;
        CompoundTag tag = CommandingStaffItem.commandData(staff);
        String mode = tag.getString(CommandingStaffItem.TAG_COMMAND_MODE);
        if ("free".equals(mode)) return false;
        // A picked attack target hands control to ServantAttackGoal.
        if ("attack".equals(mode) && tag.contains(CommandingStaffItem.TAG_COMMAND_TARGET)) return false;
        this.owner = player;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && owner.isAlive() && owner.distanceToSqr(mob) < LEASH_DISTANCE_SQ;
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.mob.getNavigation().stop();
    }

    @Override
    @SuppressWarnings("null")
    public void tick() {
        ItemStack staff = CommandingStaffItem.findHeldStaff(owner);
        if (staff == null) return;
        CompoundTag tag = CommandingStaffItem.commandData(staff);
        String commandMode = tag.contains(CommandingStaffItem.TAG_COMMAND_MODE)
                ? tag.getString(CommandingStaffItem.TAG_COMMAND_MODE)
                : "follow";

        switch (commandMode) {
            case "attack" -> {
                if (!tag.contains(CommandingStaffItem.TAG_COMMAND_TARGET)
                        && tag.contains(CommandingStaffItem.TAG_COMMAND_POS)) {
                    holdCommandPos(tag.getCompound(CommandingStaffItem.TAG_COMMAND_POS));
                }
            }
            case "free" -> {
                // The mob's own AI runs as normal; it just may never turn on its owner.
                if (mob.getTarget() == owner) {
                    mob.setTarget(null);
                }
            }
            case "move" -> {
                if (tag.contains(CommandingStaffItem.TAG_COMMAND_POS)) {
                    holdCommandPos(tag.getCompound(CommandingStaffItem.TAG_COMMAND_POS));
                }
            }
            case "follow" -> followOwner();
        }
    }

    private void followOwner() {
        clearInvalidTarget();
        if (--this.timeToRecalcPath > 0) return;
        this.timeToRecalcPath = PATH_RECALC_TICKS;

        double distanceSq = mob.distanceToSqr(owner);
        if (distanceSq > TELEPORT_DISTANCE_SQ) {
            teleportToOwner();
        } else if (distanceSq > FOLLOW_DISTANCE_SQ) {
            this.mob.getNavigation().moveTo(owner, 1.0);
        } else if (distanceSq < STOP_DISTANCE_SQ) {
            this.mob.getNavigation().stop();
        }
    }

    private void holdCommandPos(CompoundTag posTag) {
        clearInvalidTarget();
        Vec3 targetPos = new Vec3(posTag.getDouble("x"), posTag.getDouble("y"), posTag.getDouble("z"));
        if (mob.distanceToSqr(targetPos) > 1.0) {
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = PATH_RECALC_TICKS;
                this.mob.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.0);
            }
        } else {
            mob.getNavigation().stop();
        }
    }

    /** Drops a dead target, or the owner if the mob somehow picked them. */
    private void clearInvalidTarget() {
        LivingEntity target = mob.getTarget();
        if (target == null || target == owner || !target.isAlive()) {
            mob.setTarget(null);
        }
    }

    private void teleportToOwner() {
        BlockPos center = owner.blockPosition();
        Level level = mob.level();

        // An airborne owner needs solid ground somewhere below to land the servant on.
        boolean foundGround = owner.onGround();
        if (!foundGround) {
            for (int y = 0; y < 16; y++) {
                BlockPos below = center.below(y);
                if (level.getBlockState(below).isSolidRender(level, below)) {
                    center = below.above();
                    foundGround = true;
                    break;
                }
            }
        }
        // Nothing to stand on (e.g. the owner is flying high): wait instead of dropping the servant mid-air.
        if (!foundGround) return;

        for (int i = 0; i < 10; ++i) {
            int x = mob.getRandom().nextInt(7) - 3;
            int y = mob.getRandom().nextInt(3) - 1;
            int z = mob.getRandom().nextInt(7) - 3;
            BlockPos target = center.offset(x, y, z);

            if (isValidTeleportSpot(target)) {
                mob.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, mob.getYRot(), mob.getXRot());
                mob.fallDistance = 0;
                mob.getNavigation().stop();
                return;
            }
        }
    }

    private boolean isValidTeleportSpot(BlockPos pos) {
        Level level = mob.level();
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && !level.getBlockState(pos.below()).isAir();
    }
}
