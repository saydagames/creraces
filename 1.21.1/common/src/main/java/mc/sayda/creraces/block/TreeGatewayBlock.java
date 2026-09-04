package mc.sayda.creraces.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class TreeGatewayBlock extends Block {

    public TreeGatewayBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                  BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            // Try personal spawn (bed / respawn anchor)
            if (sp.getRespawnPosition() != null) {
                net.minecraft.world.level.portal.DimensionTransition transition =
                        sp.findRespawnPositionAndUseSpawnBlock(false, net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING);
                if (!transition.missingRespawnBlock()) {
                    Vec3 v = transition.pos();
                    sp.teleportTo(transition.newLevel(), v.x, v.y, v.z, transition.yRot(), transition.xRot());
                    return InteractionResult.sidedSuccess(false);
                }
            }
            // Fallback: world spawn
            ServerLevel overworld = sp.server.getLevel(Level.OVERWORLD);
            if (overworld != null) {
                BlockPos spawn = overworld.getSharedSpawnPos();
                sp.teleportTo(overworld,
                        spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                        sp.getYRot(), sp.getXRot());
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
