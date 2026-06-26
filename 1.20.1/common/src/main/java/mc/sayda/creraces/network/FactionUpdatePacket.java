package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.territory.FactionData;
import mc.sayda.creraces.territory.FactionRank;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.*;
import java.util.function.Supplier;

/**
 * S2C: broadcasts current faction state to all members.
 */
@SuppressWarnings("null")
public class FactionUpdatePacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "faction_update");

    public static final class MemberInfo {
        public final UUID uuid;
        public final String name;
        public final FactionRank rank;

        public MemberInfo(UUID uuid, String name, FactionRank rank) {
            this.uuid = uuid;
            this.name = name;
            this.rank = rank;
        }
    }

    public final UUID factionId;
    public final String factionName;
    public final List<MemberInfo> members;
    /** Null when the faction is not in any clan. */
    public final UUID clanId;

    public FactionUpdatePacket(UUID factionId, String factionName, List<MemberInfo> members, UUID clanId) {
        this.factionId = factionId;
        this.factionName = factionName;
        this.members = members;
        this.clanId = clanId;
    }

    public FactionUpdatePacket(FriendlyByteBuf buf) {
        this.factionId = buf.readUUID();
        this.factionName = buf.readUtf(64);
        int size = buf.readVarInt();
        if (size < 0 || size > 256) throw new IllegalStateException("Oversized faction packet: " + size);
        this.members = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUUID();
            String name = buf.readUtf(32);
            FactionRank rank = buf.readEnum(FactionRank.class);
            members.add(new MemberInfo(uuid, name, rank));
        }
        this.clanId = buf.readBoolean() ? buf.readUUID() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(factionId);
        buf.writeUtf(factionName);
        buf.writeVarInt(members.size());
        for (MemberInfo m : members) {
            buf.writeUUID(m.uuid);
            buf.writeUtf(m.name);
            buf.writeEnum(m.rank);
        }
        buf.writeBoolean(clanId != null);
        if (clanId != null) buf.writeUUID(clanId);
    }

    public static FactionUpdatePacket from(FactionData faction, MinecraftServer server) {
        List<MemberInfo> list = new ArrayList<>();
        for (Map.Entry<UUID, FactionRank> e : faction.getMembers().entrySet()) {
            var p = server.getPlayerList().getPlayer(e.getKey());
            String name = p != null ? p.getName().getString() : e.getKey().toString().substring(0, 8);
            list.add(new MemberInfo(e.getKey(), name, e.getValue()));
        }
        return new FactionUpdatePacket(faction.getId(), faction.getName(), list, faction.getClanId());
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                mc.sayda.creraces.client.screen.FactionManagementScreen.update(this);
            });
        });
    }
}
