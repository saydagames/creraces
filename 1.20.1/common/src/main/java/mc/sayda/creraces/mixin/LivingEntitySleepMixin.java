package mc.sayda.creraces.mixin;

import mc.sayda.creraces.block.entity.MicroBlockEntity;
import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mc.sayda.creraces.util.ISleepSlotTracker;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySleepMixin extends Entity {

    @Shadow
    public abstract Optional<BlockPos> getSleepingPos();

    public LivingEntitySleepMixin(net.minecraft.world.entity.EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "checkBedExists", at = @At("HEAD"), cancellable = true)
    private void creraces$allowMicroblockOrForceSleep(CallbackInfoReturnable<Boolean> cir) {
        if (this.getTags().contains("creraces_force_sleep")) {
            cir.setReturnValue(true);
            return;
        }

        this.getSleepingPos().ifPresent(pos -> {
            BlockState state = this.level().getBlockState(pos);
            if (state.is(ModBlocks.MICRO_BLOCK.get())) {
                if (this instanceof ISleepSlotTracker tracker && tracker.creraces$getSleepSlot() >= 0) {
                    cir.setReturnValue(true);
                    return;
                }

                // Fallback for respawn or older saves where slot isn't set
                if (this.level().getBlockEntity(pos) instanceof MicroBlockEntity micro) {
                    if (MicroBlockEntity.findBedSlot(micro) >= 0) {
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }
        });
    }

    @Inject(method = "setPosToBed", at = @At("HEAD"), cancellable = true)
    private void creraces$adjustMicroblockSleepHeight(BlockPos pos, CallbackInfo ci) {
        if (this.getTags().contains("creraces_force_sleep")) {
            ci.cancel();
            return;
        }

        BlockState hostState = this.level().getBlockState(pos);
        if (hostState.is(ModBlocks.MICRO_BLOCK.get())) {
            if (this.level().getBlockEntity(pos) instanceof MicroBlockEntity micro) {
                int slotIdx = -1;
                if (this instanceof ISleepSlotTracker tracker) {
                    slotIdx = tracker.creraces$getSleepSlot();
                }

                // If no slot tracked, try to find the first bed
                if (slotIdx < 0) {
                    slotIdx = MicroBlockEntity.findBedSlot(micro);
                }

                if (slotIdx >= 0) {
                    int x = slotIdx % 4;
                    int y = (slotIdx / 4) % 4;
                    int z = slotIdx / 16;
                    BlockState bedState = micro.getSlot(x, y, z);

                    if (bedState.getBlock() instanceof BedBlock) {
                        Vec3 standPos = MicroBlockEntity.computeBedStandPosition(pos, bedState, x, y, z);
                        this.setPos(standPos.x, standPos.y, standPos.z);
                        ci.cancel();
                    }
                }
            }
        }
    }

    @Inject(method = "getBedOrientation", at = @At("HEAD"), cancellable = true)
    private void creraces$getMicroBedOrientation(CallbackInfoReturnable<Direction> cir) {
        if (this.getTags().contains("creraces_force_sleep")) {
            cir.setReturnValue(this.getDirection());
            return;
        }

        this.getSleepingPos().ifPresent(pos -> {
            if (this.level().getBlockState(pos).is(ModBlocks.MICRO_BLOCK.get())) {
                if (this.level().getBlockEntity(pos) instanceof MicroBlockEntity micro) {
                    int slotIdx = -1;
                    if (this instanceof ISleepSlotTracker tracker) {
                        slotIdx = tracker.creraces$getSleepSlot();
                    }

                    if (slotIdx < 0) {
                        slotIdx = MicroBlockEntity.findBedSlot(micro);
                    }

                    if (slotIdx >= 0) {
                        BlockState bedState = micro.getSlot(slotIdx % 4, (slotIdx / 4) % 4, slotIdx / 16);
                        if (bedState.getBlock() instanceof BedBlock) {
                            cir.setReturnValue(bedState.getValue(BedBlock.FACING));
                        }
                    }
                }
            }
        });
    }

    @Inject(method = "stopSleeping", at = @At("HEAD"))
    private void creraces$clearForceSleepTag(CallbackInfo ci) {
        this.getTags().remove("creraces_force_sleep");
    }
}
