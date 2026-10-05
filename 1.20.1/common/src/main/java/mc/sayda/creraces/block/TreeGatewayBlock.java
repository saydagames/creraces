package mc.sayda.creraces.block;

import mc.sayda.creraces.util.ReturnPoint;
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

/**
 * A way out of the fairy realm. Players who came in through an ability that saved a return point
 * (the fae circle) go back to it; anyone else leaves the way a death respawn would.
 */
public class TreeGatewayBlock extends Block {

    public TreeGatewayBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && !ReturnPoint.sendBack(serverPlayer)) {
            sendToRespawnPoint(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Bed or respawn anchor (using up a charge, as dying does), otherwise the overworld spawn. */
    private static void sendToRespawnPoint(ServerPlayer player) {
        BlockPos respawnPos = player.getRespawnPosition();
        ServerLevel respawnLevel = player.server.getLevel(player.getRespawnDimension());
        if (respawnPos != null && respawnLevel != null) {
            Optional<Vec3> standPos = Player.findRespawnPositionAndUseSpawnBlock(
                    respawnLevel, respawnPos, player.getRespawnAngle(), player.isRespawnForced(), false);
            if (standPos.isPresent()) {
                Vec3 v = standPos.get();
                player.teleportTo(respawnLevel, v.x, v.y, v.z, player.getRespawnAngle(), player.getXRot());
                return;
            }
        }

        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            BlockPos spawn = overworld.getSharedSpawnPos();
            player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
        }
    }
}
