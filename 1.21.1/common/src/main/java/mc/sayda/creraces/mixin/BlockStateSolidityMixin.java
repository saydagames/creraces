package mc.sayda.creraces.mixin;

import mc.sayda.creraces.block.MicroBlock;
import mc.sayda.creraces.block.entity.MicroBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SupportType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateSolidityMixin {

    @Shadow
    public abstract Block getBlock();

    @Inject(method = "isFaceSturdy(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/SupportType;)Z", at = @At("HEAD"), cancellable = true)
    private void creraces$isMicroFaceSturdy(BlockGetter level, net.minecraft.core.BlockPos pos, Direction face,
            SupportType supportType,
            CallbackInfoReturnable<Boolean> cir) {
        if (this.getBlock() instanceof MicroBlock) {
            if (level.getBlockEntity(pos) instanceof MicroBlockEntity micro) {
                // Check if any mini-block on this face is solid.
                // For the micro-grid, we consider a face sturdy if ANY slot touching that face
                // is occupied.
                int xStart = (face == Direction.EAST) ? 3 : 0;
                int xEnd = (face == Direction.WEST) ? 0 : 3;
                int yStart = (face == Direction.UP) ? 3 : 0;
                int yEnd = (face == Direction.DOWN) ? 0 : 3;
                int zStart = (face == Direction.SOUTH) ? 3 : 0;
                int zEnd = (face == Direction.NORTH) ? 0 : 3;

                for (int x = xStart; x <= xEnd; x++) {
                    for (int y = yStart; y <= yEnd; y++) {
                        for (int z = zStart; z <= zEnd; z++) {
                            BlockState slotState = micro.getSlot(x, y, z);
                            if (!slotState.isAir()) {
                                cir.setReturnValue(true);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
