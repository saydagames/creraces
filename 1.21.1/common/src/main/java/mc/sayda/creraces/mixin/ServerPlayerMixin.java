package mc.sayda.creraces.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.Direction;

@SuppressWarnings("null")
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    // Vanilla's startSleepInBed requires a real BedBlock state at the position,
    // so this spoofs one for microblock beds and for the force-sleep tag set by
    // SleepAction.
    // NeoForge patches startSleepInBed to fire CanPlayerSleepEvent, which moves vanilla's body
    // (and these getBlockState reads) into a synthetic lambda. Target both shapes and require
    // only one to match, so the same spoof works on either loader.
    @WrapOperation(method = { "startSleepInBed",
            "lambda$startSleepInBed$13" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"), require = 1)
    private BlockState creraces$spoofMicroblockBedState(Level level, BlockPos pos, Operation<BlockState> original) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.getTags().contains("creraces_force_sleep")) {
            return Blocks.RED_BED.defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, player.getDirection())
                    .setValue(BedBlock.PART, BedPart.HEAD);
        }

        BlockState state = original.call(level, pos);
        if (state.is(ModBlocks.MICRO_BLOCK.get())) {
            if (level.getBlockEntity(pos) instanceof mc.sayda.creraces.block.entity.MicroBlockEntity micro) {
                int slotIdx = -1;
                if ((Object) this instanceof mc.sayda.creraces.util.ISleepSlotTracker tracker) {
                    slotIdx = tracker.creraces$getSleepSlot();
                }

                if (slotIdx < 0) {
                    slotIdx = mc.sayda.creraces.block.entity.MicroBlockEntity.findBedSlot(micro);
                }

                if (slotIdx >= 0) {
                    BlockState bedState = micro.getSlot(slotIdx % 4, (slotIdx / 4) % 4, slotIdx / 16);
                    if (bedState.getBlock() instanceof BedBlock) {
                        return Blocks.RED_BED.defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING,
                                        bedState.getValue(HorizontalDirectionalBlock.FACING))
                                .setValue(BedBlock.PART, BedPart.HEAD);
                    }
                }
            }
        }
        return state;
    }

    @Inject(method = "bedBlocked", at = @At("HEAD"), cancellable = true)
    private void creraces$bypassMicroblockSolidCheck(BlockPos pos, Direction direction,
            CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        BlockState state = player.level().getBlockState(pos);
        if (state.is(ModBlocks.MICRO_BLOCK.get())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setRespawnPosition", at = @At("HEAD"), cancellable = true)
    private void creraces$cancelForcedSleepRespawn(net.minecraft.resources.ResourceKey<Level> dimension,
            @javax.annotation.Nullable BlockPos pos, float angle, boolean forced, boolean sendMessage,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.getTags().contains("creraces_force_sleep")) {
            ci.cancel();
        }
    }
}
