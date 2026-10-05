package mc.sayda.creraces.entity.ai;

import mc.sayda.creraces.item.CommandingStaffItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.UUID;

/** Points a servant at the target its owner marked with the staff in attack mode. */
public class ServantAttackGoal extends Goal {
    private final Mob mob;
    private Player owner;

    public ServantAttackGoal(Mob mob) {
        this.mob = mob;
        // TARGET only, so MeleeAttackGoal keeps the MOVE and LOOK flags it needs to reach the target.
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        Player player = ServantGoal.findOwner(mob);
        ItemStack staff = player != null ? CommandingStaffItem.findHeldStaff(player) : null;
        if (staff == null) return false;
        CompoundTag tag = CommandingStaffItem.commandData(staff);
        if (!"attack".equals(tag.getString(CommandingStaffItem.TAG_COMMAND_MODE))
                || !tag.contains(CommandingStaffItem.TAG_COMMAND_TARGET)) {
            return false;
        }
        this.owner = player;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        ItemStack staff = CommandingStaffItem.findHeldStaff(owner);
        if (staff == null) return;
        CompoundTag tag = CommandingStaffItem.commandData(staff);
        if (!tag.contains(CommandingStaffItem.TAG_COMMAND_TARGET)) return;

        UUID targetId = tag.getUUID(CommandingStaffItem.TAG_COMMAND_TARGET);
        Entity target = mob.level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(targetId) : null;
        // Never turn on the owner, even if they marked themselves.
        if (target instanceof LivingEntity livingTarget && livingTarget.isAlive() && livingTarget != owner) {
            mob.setTarget(livingTarget);
        } else {
            mob.setTarget(null);
        }
    }
}
