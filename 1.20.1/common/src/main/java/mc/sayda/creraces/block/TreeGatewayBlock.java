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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            // Try personal spawn (bed / respawn anchor)
            BlockPos respawnPos = sp.getRespawnPosition();
            if (respawnPos != null) {
                ServerLevel respawnLevel = sp.server.getLevel(sp.getRespawnDimension());
                if (respawnLevel != null) {
                    Optional<Vec3> safePos = Player.findRespawnPositionAndUseSpawnBlock(
                            respawnLevel, respawnPos, sp.getRespawnAngle(), sp.isRespawnForced(), false);
                    if (safePos.isPresent()) {
                        Vec3 v = safePos.get();
                        sp.teleportTo(respawnLevel, v.x, v.y, v.z, sp.getRespawnAngle(), sp.getXRot());
                        return InteractionResult.sidedSuccess(false);
                    }
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
