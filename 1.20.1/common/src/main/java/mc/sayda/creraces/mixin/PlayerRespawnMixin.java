package mc.sayda.creraces.mixin;

import mc.sayda.creraces.block.entity.MicroBlockEntity;
import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Player.class)
public abstract class PlayerRespawnMixin {

    @Inject(method = "findRespawnPositionAndUseSpawnBlock", at = @At("HEAD"), cancellable = true)
    private static void creraces$supportMicroblockRespawn(ServerLevel serverLevel, BlockPos blockPos, float f,
            boolean bl, boolean bl2, CallbackInfoReturnable<Optional<Vec3>> cir) {
        BlockState blockState = serverLevel.getBlockState(blockPos);

        if (blockState.is(ModBlocks.MICRO_BLOCK.get())) {
            if (serverLevel.getBlockEntity(blockPos) instanceof MicroBlockEntity micro) {
                for (int x = 0; x < 4; x++) {
                    for (int y = 0; y < 4; y++) {
                        for (int z = 0; z < 4; z++) {
                            if (micro.getSlot(x, y, z).getBlock() instanceof BedBlock bed) {
                                BlockState bedState = micro.getSlot(x, y, z);
                                // Mini beds have no native stand-up logic, so just return the adjusted bed position directly.
                                Optional<Vec3> optional = Optional
                                        .of(MicroBlockEntity.computeBedStandPosition(blockPos, bedState, x, y, z));
                                cir.setReturnValue(optional);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
