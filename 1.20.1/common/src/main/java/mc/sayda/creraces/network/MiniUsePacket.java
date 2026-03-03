package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.block.entity.MicroBlockEntity;
import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * C2S: Client requests to open the interface of a mini-block in a specific
 * slot.
 * Sent when the player right-clicks an existing interactive micro-block
 * (crafting table,
 * barrel, furnace, etc.) while in smallBuild mode.
 */
public class MiniUsePacket {

    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "mini_use");

    private final BlockPos hostPos;
    private final int slotX, slotY, slotZ;
    private final net.minecraft.world.InteractionHand hand;

    public MiniUsePacket(BlockPos hostPos, int slotX, int slotY, int slotZ, net.minecraft.world.InteractionHand hand) {
        this.hostPos = hostPos;
        this.slotX = slotX;
        this.slotY = slotY;
        this.slotZ = slotZ;
        this.hand = hand;
    }

    /** Decode from network buffer. */
    public MiniUsePacket(FriendlyByteBuf buf) {
        this.hostPos = buf.readBlockPos();
        this.slotX = buf.readByte();
        this.slotY = buf.readByte();
        this.slotZ = buf.readByte();
        this.hand = buf.readEnum(net.minecraft.world.InteractionHand.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(hostPos);
        buf.writeByte(slotX);
        buf.writeByte(slotY);
        buf.writeByte(slotZ);
        buf.writeEnum(hand);
    }

    @SuppressWarnings("null")
    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        dev.architectury.networking.NetworkManager.PacketContext ctx = contextSupplier.get();
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer player))
                return;

            var level = player.serverLevel();
            var blockState = level.getBlockState(hostPos);

            if (!blockState.is(ModBlocks.MICRO_BLOCK.get()))
                return;
            if (!(level.getBlockEntity(hostPos) instanceof MicroBlockEntity micro))
                return;

            // Bounds check (packet could be crafted with out-of-range values)
            if (slotX < 0 || slotX >= 4 || slotY < 0 || slotY >= 4 || slotZ < 0 || slotZ >= 4)
                return;

            BlockState slotState = micro.getSlot(slotX, slotY, slotZ);
            if (slotState.isAir())
                return;

            // Delegate to unified interaction helper
            micro.handleSlotUse(player, hand, slotX, slotY, slotZ);
        });
    }
}
