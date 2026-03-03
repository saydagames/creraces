package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.block.entity.MicroBlockEntity;
import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.core.Direction;

/**
 * C2S: Client requests to remove a mini-block from a specific slot.
 * The item is dropped server-side.
 */
public class MiniRemovePacket {

    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "mini_remove");

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

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer serverPlayer))
                return;
            ServerLevel level = serverPlayer.serverLevel();

            if (!(level.getBlockEntity(hostPos) instanceof MicroBlockEntity micro))
                return;

            // Validate slot bounds
            if (slotX < 0 || slotX >= 4 || slotY < 0 || slotY >= 4 || slotZ < 0 || slotZ >= 4)
                return;

            BlockState removed = micro.getSlot(slotX, slotY, slotZ);
            if (removed.isAir())
                return; // Nothing to remove

            // Try to add to inventory first
            ItemStack drop = new ItemStack(removed.getBlock().asItem());
            if (!drop.isEmpty()) {
                if (!serverPlayer.getInventory().add(drop)) {
                    // Drop on the ground if inventory is full
                    net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                            level, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), drop);
                    itemEntity.setDefaultPickUpDelay();
                    level.addFreshEntity(itemEntity);
                }
            }

            // Clear the targeted slot
            micro.setSlot(slotX, slotY, slotZ, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());

            // --- DESTRUCTION COORDINATION (Fix dupes) ---
            if (removed.getBlock() instanceof net.minecraft.world.level.block.DoorBlock) {
                DoubleBlockHalf half = removed.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
                int otherY = (half == DoubleBlockHalf.LOWER) ? slotY + 1 : slotY - 1;
                // Bounds-check before accessing companion slot
                if (otherY >= 0 && otherY < 4) {
                    BlockState otherState = MicroBlockEntity.getSlotGlobal(level, hostPos, slotX, otherY, slotZ);
                    if (otherState.getBlock() == removed.getBlock()) {
                        MicroBlockEntity.setSlotGlobal(level, hostPos, slotX, otherY, slotZ,
                                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }
                }
            } else if (removed.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
                BedPart part = removed.getValue(BlockStateProperties.BED_PART);
                Direction facing = removed.getValue(BlockStateProperties.HORIZONTAL_FACING);
                int ox = (part == BedPart.FOOT) ? facing.getStepX() : -facing.getStepX();
                int oz = (part == BedPart.FOOT) ? facing.getStepZ() : -facing.getStepZ();
                int headX = slotX + ox;
                int headZ = slotZ + oz;
                // Bounds-check before accessing companion slot
                if (headX >= 0 && headX < 4 && headZ >= 0 && headZ < 4) {
                    BlockState otherState = MicroBlockEntity.getSlotGlobal(level, hostPos, headX, slotY, headZ);
                    if (otherState.getBlock() == removed.getBlock()) {
                        MicroBlockEntity.setSlotGlobal(level, hostPos, headX, slotY, headZ,
                                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }
                }
            } else if (removed.getBlock() instanceof net.minecraft.world.level.block.JukeboxBlock) {
                level.levelEvent(null, 1010, hostPos, 0);
            }

            // Trigger neighbor updates
            micro.updateConnections(slotX, slotY, slotZ);

            BlockState hostState = level.getBlockState(hostPos);
            level.sendBlockUpdated(hostPos, hostState, hostState, 3);

            // If the MicroBlock is now empty, remove the host block
            if (micro.isEmpty()) {
                level.removeBlock(hostPos, false);
            }
        });
    }
}
