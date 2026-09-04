package mc.sayda.creraces.entity.ai;

import mc.sayda.creraces.registry.ModItems;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;

import java.util.EnumSet;
import java.util.UUID;

public class ServantAttackGoal extends Goal {
    private final Mob mob;
    private Player owner;

    public ServantAttackGoal(Mob mob) {
        this.mob = mob;
        // We ONLY set Flag.TARGET so we don't block Flag.MOVE or Flag.LOOK 
        // which the MeleeAttackGoal needs to actually hit the target.
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        CompoundTag nbt = ((IPersistentDataAccessor) mob).creraces$getPersistentData();
        if (nbt.contains("creraces:servant_of")) {
            try {
                UUID ownerUUID = mc.sayda.creraces.capability.DataUtils.loadUUID(nbt, "creraces:servant_of");
                Player p = mob.level().getPlayerByUUID(ownerUUID);
                if (p != null) {
                    ItemStack staff = p.getMainHandItem().is(ModItems.COMMANDING_STAFF.get()) ? p.getMainHandItem() : p.getOffhandItem();
                    if (staff.is(ModItems.COMMANDING_STAFF.get())) {
                        CompoundTag tag = mc.sayda.creraces.util.ItemNbt.get(staff);
                        String mode = tag.getString("CommandMode");
                        if ("attack".equals(mode) && tag.contains("CommandTarget")) {
                            this.owner = p;
                            return true;
                        }
                    }
                }
            } catch (Exception e) { mc.sayda.creraces.CreRaces.LOGGER.debug("Error reading servant NBT: {}", e.getMessage()); }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        ItemStack staff = owner.getMainHandItem().is(ModItems.COMMANDING_STAFF.get()) ? owner.getMainHandItem() : owner.getOffhandItem();
        CompoundTag tag = mc.sayda.creraces.util.ItemNbt.get(staff);
        if (tag.contains("CommandTarget")) {
            UUID targetUUID = tag.getUUID("CommandTarget");
            Entity target = mob.level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(targetUUID) : null;
            if (target instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
                if (livingTarget != owner) {
                    mob.setTarget(livingTarget);
                } else {
                    mob.setTarget(null);
                }
            } else {
                mob.setTarget(null);
            }
        }
    }
}
