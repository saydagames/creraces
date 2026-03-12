package mc.sayda.creraces.entity.ai;

import mc.sayda.creraces.registry.ModItems;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.EnumSet;
import java.util.UUID;

public class ServantGoal extends Goal {
    private final Mob mob;
    private Player owner;
    private int timeToRecalcPath;

    public ServantGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        CompoundTag nbt = ((IPersistentDataAccessor) mob).creraces$getPersistentData();
        if (nbt.contains("creraces:servant_of")) {
            try {
                String uuidStr = nbt.getString("creraces:servant_of");
                UUID ownerUUID = UUID.fromString(uuidStr);
                Player p = mob.level().getPlayerByUUID(ownerUUID);
                if (p != null) {
                    ItemStack staff = p.getMainHandItem().is(mc.sayda.creraces.registry.ModItems.COMMANDING_STAFF.get()) ? p.getMainHandItem() : p.getOffhandItem();
                    if (staff.is(mc.sayda.creraces.registry.ModItems.COMMANDING_STAFF.get())) {
                        CompoundTag tag = staff.getOrCreateTag();
                        String mode = tag.getString("CommandMode");
                        if ("free".equals(mode)) return false;
                        if ("attack".equals(mode) && tag.contains("CommandTarget")) return false;
                        this.owner = p;
                        return true;
                    }
                }
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && owner.isAlive() && owner.distanceToSqr(mob) < 1024.0;
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
        ItemStack staff = owner.getMainHandItem().is(ModItems.COMMANDING_STAFF.get()) ? owner.getMainHandItem() : owner.getOffhandItem();
        CompoundTag tag = staff.getOrCreateTag();

        String commandMode = tag.contains("CommandMode") ? tag.getString("CommandMode") : "follow";

        switch (commandMode) {
            case "attack" -> {
                if (!tag.contains("CommandTarget") && tag.contains("CommandPos")) {
                    // Hybrid: if no target, move to position
                    CompoundTag posTag = tag.getCompound("CommandPos");
                    Vec3 targetPos = new Vec3(posTag.getDouble("x"), posTag.getDouble("y"), posTag.getDouble("z"));
                    
                    if (mob.getTarget() == null || mob.getTarget() == owner || !mob.getTarget().isAlive()) {
                        mob.setTarget(null); 
                    }
                    if (mob.distanceToSqr(targetPos) > 1.0) {
                        if (--this.timeToRecalcPath <= 0) {
                            this.timeToRecalcPath = 10;
                            this.mob.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.0);
                        }
                    } else {
                        mob.getNavigation().stop();
                    }
                }
            }
            case "free" -> {
                if (mob.getTarget() == owner) {
                    mob.setTarget(null); // Stop attacking owner if somehow targeted
                }
                // Free mode = default AI behavior, we don't interfere
            }
            case "move" -> {
                if (tag.contains("CommandPos")) {
                    CompoundTag posTag = tag.getCompound("CommandPos");
                    Vec3 targetPos = new Vec3(posTag.getDouble("x"), posTag.getDouble("y"), posTag.getDouble("z"));
                    
                    if (mob.getTarget() == null || mob.getTarget() == owner || !mob.getTarget().isAlive()) {
                        mob.setTarget(null); 
                    }
                    if (mob.distanceToSqr(targetPos) > 1.0) {
                        if (--this.timeToRecalcPath <= 0) {
                            this.timeToRecalcPath = 10;
                            this.mob.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.0);
                        }
                    } else {
                        mob.getNavigation().stop();
                    }
                }
            }
            case "follow" -> {
                if (mob.getTarget() == null || mob.getTarget() == owner || !mob.getTarget().isAlive()) {
                    mob.setTarget(null);
                }
                if (--this.timeToRecalcPath <= 0) {
                    this.timeToRecalcPath = 10;
                    if (mob.distanceToSqr(owner) > 256.0) {
                        teleportToOwner();
                    } else if (mob.distanceToSqr(owner) > 16.0) {
                        this.mob.getNavigation().moveTo(owner, 1.0);
                    } else if (mob.distanceToSqr(owner) < 4.0) {
                        this.mob.getNavigation().stop();
                    }
                }
            }
        }
    }

    private void teleportToOwner() {
        BlockPos center = owner.blockPosition();
        Level level = mob.level();
        
        // If owner is high up, find the ground
        boolean foundGround = false;
        if (owner.onGround()) {
            foundGround = true;
        } else {
            for (int y = 0; y < 16; y++) {
                BlockPos below = center.below(y);
                if (level.getBlockState(below).isSolidRender(level, below)) {
                    center = below.above();
                    foundGround = true;
                    break;
                }
            }
        }

        // Only teleport if we found ground or a safe spot
        if (foundGround) {
            // Try to find a spot nearby
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
        
        // No longer falling back to owner's exact position if flying/no ground
        // They will wait until the player is closer to ground or a spot is found
    }

    private boolean isValidTeleportSpot(BlockPos pos) {
        Level level = mob.level();
        return level.getBlockState(pos).isAir() && 
               level.getBlockState(pos.above()).isAir() && 
               !level.getBlockState(pos.below()).isAir();
    }
}
