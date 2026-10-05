package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.ability.HexPos;
import mc.sayda.creraces.client.screen.ResearchTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** S2C: the research table's essence grid, pushed to an open research table screen. */
public class SyncHexGridPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "sync_hex_grid");

    private final Map<HexPos, EssenceType> grid;

    public SyncHexGridPacket(Map<HexPos, EssenceType> grid) {
        this.grid = grid;
    }

    public SyncHexGridPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        grid = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            HexPos pos = HexPos.decode(buf);
            EssenceType essence = buf.readEnum(EssenceType.class);
            grid.put(pos, essence);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(grid.size());
        for (Map.Entry<HexPos, EssenceType> e : grid.entrySet()) {
            e.getKey().encode(buf);
            buf.writeEnum(e.getValue());
        }
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> {
            if (Minecraft.getInstance().screen instanceof ResearchTableScreen screen) {
                screen.receiveGridSync(grid);
            }
        });
    }
}
