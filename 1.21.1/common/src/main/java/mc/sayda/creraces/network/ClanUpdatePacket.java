package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.territory.ClanData;
import mc.sayda.creraces.territory.DiplomacyStatus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * S2C: pushes diplomacy state for the player's race (triggers ClanManagementScreen.update).
 */
@SuppressWarnings("null")
public class ClanUpdatePacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "clan_update");

    public final ResourceLocation raceId;
    public final Map<ResourceLocation, DiplomacyStatus> relations;

    public ClanUpdatePacket(ResourceLocation raceId, Map<ResourceLocation, DiplomacyStatus> relations) {
        this.raceId    = raceId;
        this.relations = Collections.unmodifiableMap(relations);
    }

    public ClanUpdatePacket(FriendlyByteBuf buf) {
        this.raceId = buf.readResourceLocation();
        int size = buf.readVarInt();
        if (size < 0 || size > 256) throw new IllegalStateException("Oversized clan update: " + size);
        Map<ResourceLocation, DiplomacyStatus> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(buf.readResourceLocation(), buf.readEnum(DiplomacyStatus.class));
        }
        this.relations = Collections.unmodifiableMap(map);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(raceId);
        buf.writeVarInt(relations.size());
        for (Map.Entry<ResourceLocation, DiplomacyStatus> e : relations.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            buf.writeEnum(e.getValue());
        }
    }

    public static ClanUpdatePacket from(ClanData clan) {
        return new ClanUpdatePacket(clan.getRaceId(), new HashMap<>(clan.getRelations()));
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() ->
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () ->
                mc.sayda.creraces.client.screen.ClanManagementScreen.update(this)));
    }
}
