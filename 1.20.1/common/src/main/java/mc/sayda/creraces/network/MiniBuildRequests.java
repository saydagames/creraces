package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.block.entity.MicroBlockEntity;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** Checks shared by the mini-block packets before they act on a slot of a micro block. */
final class MiniBuildRequests {
    private MiniBuildRequests() {
    }

    /**
     * Whether the player may act on the given slot of the micro block at hostPos. The reach check
     * keeps a crafted packet from editing micro blocks anywhere in the loaded world.
     */
    static boolean canTarget(ServerPlayer player, BlockPos hostPos, int slotX, int slotY, int slotZ,
            String packetName) {
        if (!DataUtils.canInteractWithMiniBuild(player)) {
            return false;
        }
        double reachSq = CreRacesConfig.MINI_CRAFTING_DISTANCE_SQR.get();
        if (player.distanceToSqr(hostPos.getX() + 0.5, hostPos.getY() + 0.5, hostPos.getZ() + 0.5) > reachSq) {
            CreRaces.LOGGER.warn("{}: Rejected - player {} too far from hostPos {}",
                    packetName, player.getName().getString(), hostPos);
            return false;
        }
        return isInGrid(slotX) && isInGrid(slotY) && isInGrid(slotZ);
    }

    static boolean isInGrid(int slot) {
        return slot >= 0 && slot < MicroBlockEntity.SIZE;
    }
}
