package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.ability.HexPos;
import mc.sayda.creraces.block.entity.ResearchTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class PlaceEssencePacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "place_essence");

    private final BlockPos tablePos;
    private final HexPos hexPos;
    private final EssenceType essence;

    public PlaceEssencePacket(BlockPos tablePos, HexPos hexPos, EssenceType essence) {
        this.tablePos = tablePos;
        this.hexPos = hexPos;
        this.essence = essence;
    }

    public PlaceEssencePacket(FriendlyByteBuf buf) {
        this.tablePos = buf.readBlockPos();
        this.hexPos = HexPos.decode(buf);
        this.essence = buf.readEnum(EssenceType.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(tablePos);
        hexPos.encode(buf);
        buf.writeEnum(essence);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> {
            if (ctx.get().getPlayer() instanceof ServerPlayer sp && sp.level().getBlockEntity(tablePos) instanceof ResearchTableBlockEntity be) {
                be.placeEssence(hexPos, essence);
                BoundaryHandler.syncHexGrid(sp, be);
            }
        });
    }
}
