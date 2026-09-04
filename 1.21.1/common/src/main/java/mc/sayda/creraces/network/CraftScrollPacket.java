package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.block.entity.ResearchTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class CraftScrollPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "craft_scroll");

    private final BlockPos tablePos;

    public CraftScrollPacket(BlockPos tablePos) {
        this.tablePos = tablePos;
    }

    public CraftScrollPacket(FriendlyByteBuf buf) {
        this.tablePos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(tablePos);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> {
            if (ctx.get().getPlayer() instanceof ServerPlayer sp && sp.level().getBlockEntity(tablePos) instanceof ResearchTableBlockEntity be) {
                be.attemptCraft(sp);
            }
        });
    }
}
