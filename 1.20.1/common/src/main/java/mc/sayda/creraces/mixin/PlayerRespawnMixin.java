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
                                // Subblocks are 1/4th scale.
                                double scale = 1.0 / mc.sayda.creraces.block.entity.MicroBlockEntity.SIZE;
                                double subBlockX = (x * scale) + (scale / 2.0);
                                double subBlockY = (y * scale);
                                double subBlockZ = (z * scale) + (scale / 2.0);

                                BlockState bedState = micro.getSlot(x, y, z);
                                net.minecraft.core.Direction facing = bedState.getValue(BedBlock.FACING);
                                net.minecraft.world.level.block.state.properties.BedPart part = bedState
                                        .getValue(BedBlock.PART);

                                // Final centering refinement: Halfway between 1.0 and 1.35
                                // 1.175 slots away from HEAD center
                                if (part == net.minecraft.world.level.block.state.properties.BedPart.HEAD) {
                                    subBlockX += facing.getOpposite().getStepX() * (scale * 1.175);
                                    subBlockZ += facing.getOpposite().getStepZ() * (scale * 1.175);
                                } else {
                                    subBlockX += facing.getOpposite().getStepX() * (scale * 0.175);
                                    subBlockZ += facing.getOpposite().getStepZ() * (scale * 0.175);
                                }

                                // Find a safe place to stand up, similar to standard BedBlock respawn.
                                // But since a miniature bed doesn't have native standup, simply return the
                                // microbed pos adjusted.
                                Optional<Vec3> optional = Optional.of(new Vec3(
                                        blockPos.getX() + subBlockX,
                                        blockPos.getY() + subBlockY + (0.6875 * scale),
                                        blockPos.getZ() + subBlockZ));
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
