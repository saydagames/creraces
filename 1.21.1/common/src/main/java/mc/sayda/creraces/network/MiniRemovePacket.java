package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.block.entity.MicroBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.function.Supplier;

/** C2S: removes the mini-block in one slot and hands its item to the player (or drops it). */
@SuppressWarnings("null")
public class MiniRemovePacket {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "mini_remove");

    private final BlockPos hostPos;
    private final int slotX, slotY, slotZ;

    public MiniRemovePacket(BlockPos hostPos, int slotX, int slotY, int slotZ) {
        this.hostPos = hostPos;
        this.slotX = slotX;
        this.slotY = slotY;
        this.slotZ = slotZ;
    }

    public MiniRemovePacket(FriendlyByteBuf buf) {
        this.hostPos = buf.readBlockPos();
        this.slotX = buf.readByte();
        this.slotY = buf.readByte();
        this.slotZ = buf.readByte();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(hostPos);
        buf.writeByte(slotX);
        buf.writeByte(slotY);
        buf.writeByte(slotZ);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer serverPlayer))
                return;
            if (!MiniBuildRequests.canTarget(serverPlayer, hostPos, slotX, slotY, slotZ, "MiniRemovePacket"))
                return;

            ServerLevel level = serverPlayer.serverLevel();
            if (!(level.getBlockEntity(hostPos) instanceof MicroBlockEntity micro))
                return;

            BlockState removed = micro.getSlot(slotX, slotY, slotZ);
            if (removed.isAir())
                return;

            ItemStack drop = new ItemStack(removed.getBlock().asItem());
            if (!drop.isEmpty() && !serverPlayer.getInventory().add(drop)) {
                ItemEntity itemEntity = new ItemEntity(level, serverPlayer.getX(), serverPlayer.getY(),
                        serverPlayer.getZ(), drop);
                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
            }

            micro.setSlot(slotX, slotY, slotZ, Blocks.AIR.defaultBlockState());
            clearLinkedParts(serverPlayer, level, removed);

            micro.updateConnections(slotX, slotY, slotZ);
            BlockState hostState = level.getBlockState(hostPos);
            level.sendBlockUpdated(hostPos, hostState, hostState, Block.UPDATE_ALL);

            if (micro.isEmpty()) {
                level.removeBlock(hostPos, false);
            }
        });
    }

    /**
     * Doors and beds fill two slots, so the other half is cleared too; otherwise it could be
     * removed again for a second item. A removed jukebox also stops its music.
     */
    private void clearLinkedParts(ServerPlayer player, ServerLevel level, BlockState removed) {
        if (removed.getBlock() instanceof DoorBlock) {
            DoubleBlockHalf half = removed.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            int otherY = half == DoubleBlockHalf.LOWER ? slotY + 1 : slotY - 1;
            clearIfSameBlock(level, removed, slotX, otherY, slotZ);
        } else if (removed.getBlock() instanceof BedBlock) {
            BedPart part = removed.getValue(BlockStateProperties.BED_PART);
            Direction facing = removed.getValue(BlockStateProperties.HORIZONTAL_FACING);
            int step = part == BedPart.FOOT ? 1 : -1;
            clearIfSameBlock(level, removed, slotX + step * facing.getStepX(), slotY, slotZ + step * facing.getStepZ());
        } else if (removed.getBlock() instanceof JukeboxBlock) {
            // The remover is left out: MiniBlockPlaceMixin fires this event on their client already
            level.levelEvent(player, LevelEvent.SOUND_STOP_JUKEBOX_SONG, hostPos, 0);
            level.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, hostPos, GameEvent.Context.of(removed));
        }
    }

    private void clearIfSameBlock(ServerLevel level, BlockState removed, int x, int y, int z) {
        if (MicroBlockEntity.getSlotGlobal(level, hostPos, x, y, z).getBlock() == removed.getBlock()) {
            MicroBlockEntity.setSlotGlobal(level, hostPos, x, y, z, Blocks.AIR.defaultBlockState());
        }
    }
}
