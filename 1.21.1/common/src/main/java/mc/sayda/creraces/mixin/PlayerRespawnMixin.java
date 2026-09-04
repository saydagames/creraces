package mc.sayda.creraces.mixin;

import mc.sayda.creraces.block.entity.MicroBlockEntity;
import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21 moved respawn-point resolution off Player onto ServerPlayer, and it now returns a
 * DimensionTransition rather than an Optional<Vec3>. The private helper that does the actual
 * search returns a package-private record, so hook the public entry point instead and build the
 * transition here.
 */
@Mixin(ServerPlayer.class)
public abstract class PlayerRespawnMixin {

    @Inject(method = "findRespawnPositionAndUseSpawnBlock", at = @At("HEAD"), cancellable = true)
    private void creraces$supportMicroblockRespawn(boolean forced,
            DimensionTransition.PostDimensionTransition postTransition,
            CallbackInfoReturnable<DimensionTransition> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        BlockPos respawnPos = self.getRespawnPosition();
        if (respawnPos == null) {
            return;
        }
        ServerLevel respawnLevel = self.server.getLevel(self.getRespawnDimension());
        if (respawnLevel == null) {
            return;
        }

        BlockState hostState = respawnLevel.getBlockState(respawnPos);
        if (!hostState.is(ModBlocks.MICRO_BLOCK.get())
                || !(respawnLevel.getBlockEntity(respawnPos) instanceof MicroBlockEntity micro)) {
            return;
        }

        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 4; z++) {
                    BlockState bedState = micro.getSlot(x, y, z);
                    if (bedState.getBlock() instanceof BedBlock) {
                        // Mini beds have no native stand-up logic, so just use the adjusted bed position directly.
                        Vec3 standPos = MicroBlockEntity.computeBedStandPosition(respawnPos, bedState, x, y, z);
                        cir.setReturnValue(new DimensionTransition(respawnLevel, standPos, Vec3.ZERO,
                                self.getRespawnAngle(), 0.0f, postTransition));
                        return;
                    }
                }
            }
        }
    }
}
