package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.HexPos;
import mc.sayda.creraces.block.entity.ResearchTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class RemoveEssencePacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "remove_essence");

    private final BlockPos tablePos;
    private final HexPos hexPos;

    public RemoveEssencePacket(BlockPos tablePos, HexPos hexPos) {
        this.tablePos = tablePos;
        this.hexPos = hexPos;
    }

    public RemoveEssencePacket(FriendlyByteBuf buf) {
        this.tablePos = buf.readBlockPos();
        this.hexPos = HexPos.decode(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(tablePos);
        hexPos.encode(buf);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> {
            if (ctx.get().getPlayer() instanceof ServerPlayer sp && sp.level().getBlockEntity(tablePos) instanceof ResearchTableBlockEntity be) {
                be.removeEssence(hexPos);
                BoundaryHandler.syncHexGrid(sp, be);
            }
        });
    }
}
