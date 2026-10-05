package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.block.entity.MicroBlockEntity;
import mc.sayda.creraces.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * C2S: opens the interface of an interactive mini-block (crafting table, barrel, furnace, etc.)
 * that the player right-clicked while in smallBuild mode.
 */
public class MiniUsePacket {

    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "mini_use");

    private final BlockPos hostPos;
    private final int slotX, slotY, slotZ;
    private final InteractionHand hand;

    public MiniUsePacket(BlockPos hostPos, int slotX, int slotY, int slotZ, InteractionHand hand) {
        this.hostPos = hostPos;
        this.slotX = slotX;
        this.slotY = slotY;
        this.slotZ = slotZ;
        this.hand = hand;
    }

    public MiniUsePacket(FriendlyByteBuf buf) {
        this.hostPos = Objects.requireNonNull(buf.readBlockPos());
        this.slotX = buf.readByte();
        this.slotY = buf.readByte();
        this.slotZ = buf.readByte();
        this.hand = Objects.requireNonNull(buf.readEnum(InteractionHand.class));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(Objects.requireNonNull(hostPos));
        buf.writeByte(slotX);
        buf.writeByte(slotY);
        buf.writeByte(slotZ);
        buf.writeEnum(Objects.requireNonNull(hand));
    }

    @SuppressWarnings("null")
    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext ctx = contextSupplier.get();
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer player))
                return;
            if (!MiniBuildRequests.canTarget(player, hostPos, slotX, slotY, slotZ, "MiniUsePacket"))
                return;

            ServerLevel level = player.serverLevel();
            if (!level.getBlockState(hostPos).is(ModBlocks.MICRO_BLOCK.get()))
                return;
            if (!(level.getBlockEntity(hostPos) instanceof MicroBlockEntity micro))
                return;
            if (micro.getSlot(slotX, slotY, slotZ).isAir())
                return;

            micro.handleSlotUse(player, hand, slotX, slotY, slotZ);
        });
    }
}
