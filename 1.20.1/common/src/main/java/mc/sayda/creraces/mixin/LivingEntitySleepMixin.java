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
import net.minecraft.world.level.block.state.properties.BedPart;
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
                    for (int i = 0; i < MicroBlockEntity.TOTAL; i++) {
                        if (micro.getSlot(i % 4, (i / 4) % 4, i / 16).getBlock() instanceof BedBlock) {
                            cir.setReturnValue(true);
                            return;
                        }
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
                    for (int i = 0; i < MicroBlockEntity.TOTAL; i++) {
                        if (micro.getSlot(i % 4, (i / 4) % 4, i / 16).getBlock() instanceof BedBlock) {
                            slotIdx = i;
                            break;
                        }
                    }
                }

                if (slotIdx >= 0) {
                    int x = slotIdx % 4;
                    int y = (slotIdx / 4) % 4;
                    int z = slotIdx / 16;
                    BlockState bedState = micro.getSlot(x, y, z);

                    if (bedState.getBlock() instanceof BedBlock) {
                        double scale = 1.0 / MicroBlockEntity.SIZE;
                        Direction facing = bedState.getValue(BedBlock.FACING);
                        BedPart part = bedState.getValue(BedBlock.PART);

                        double subBlockX = (x * scale) + (scale / 2.0);
                        double subBlockY = (y * scale);
                        double subBlockZ = (z * scale) + (scale / 2.0);
                        double bedPillowHeight = 0.6875 * scale;

                        // Final centering refinement: Halfway between 1.0 and 1.35
                        // 1.175 slots away from HEAD center
                        if (part == BedPart.HEAD) {
                            subBlockX += facing.getOpposite().getStepX() * (scale * 1.175);
                            subBlockZ += facing.getOpposite().getStepZ() * (scale * 1.175);
                        } else {
                            subBlockX += facing.getOpposite().getStepX() * (scale * 0.175);
                            subBlockZ += facing.getOpposite().getStepZ() * (scale * 0.175);
                        }

                        this.setPos(
                                pos.getX() + subBlockX,
                                pos.getY() + subBlockY + bedPillowHeight,
                                pos.getZ() + subBlockZ);

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
                        for (int i = 0; i < MicroBlockEntity.TOTAL; i++) {
                            BlockState s = micro.getSlot(i % 4, (i / 4) % 4, i / 16);
                            if (s.getBlock() instanceof BedBlock) {
                                slotIdx = i;
                                break;
                            }
                        }
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
