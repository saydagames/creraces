package mc.sayda.creraces.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mc.sayda.creraces.registry.ModBlocks;
import mc.sayda.creraces.util.ISleepSlotTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
public abstract class PlayerSleepMixin {

    // I HATE MIXINS, GOD PLEASE WORK
    @WrapOperation(method = "startSleepInBed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState creraces$spoofMicroblockBedState(Level level, BlockPos pos, Operation<BlockState> original) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.getTags().contains("creraces_force_sleep")) {
            return Blocks.RED_BED.defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, player.getDirection())
                    .setValue(BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD);
        }

        BlockState state = original.call(level, pos);
        if (state.is(ModBlocks.MICRO_BLOCK.get())) {
            if (level.getBlockEntity(pos) instanceof mc.sayda.creraces.block.entity.MicroBlockEntity micro) {
                int slotIdx = -1;
                if ((Object) this instanceof ISleepSlotTracker tracker) {
                    slotIdx = tracker.creraces$getSleepSlot();
                }

                if (slotIdx < 0) {
                    for (int i = 0; i < mc.sayda.creraces.block.entity.MicroBlockEntity.TOTAL; i++) {
                        if (micro.getSlot(i % 4, (i / 4) % 4, i / 16).getBlock() instanceof BedBlock) {
                            slotIdx = i;
                            break;
                        }
                    }
                }

                if (slotIdx >= 0) {
                    BlockState bedState = micro.getSlot(slotIdx % 4, (slotIdx / 4) % 4, slotIdx / 16);
                    if (bedState.getBlock() instanceof BedBlock) {
                        return Blocks.RED_BED.defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING,
                                        bedState.getValue(HorizontalDirectionalBlock.FACING))
                                .setValue(BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD);
                    }
                }
            }
        }
        return state;
    }
}
