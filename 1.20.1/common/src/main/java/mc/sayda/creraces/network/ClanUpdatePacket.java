package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.territory.ClanData;
import mc.sayda.creraces.territory.FactionData;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.function.Supplier;

/**
 * S2C: pushes full clan state to a client (triggers ClanManagementScreen.update).
 */
@SuppressWarnings("null")
public class ClanUpdatePacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "clan_update");

    public static final class FactionInfo {
        public final UUID   factionId;
        public final String factionName;
        public final String leaderName;
        public final int    memberCount;

        public FactionInfo(UUID factionId, String factionName, String leaderName, int memberCount) {
            this.factionId   = factionId;
            this.factionName = factionName;
            this.leaderName  = leaderName;
            this.memberCount = memberCount;
        }
    }

    public final UUID              clanId;
    public final UUID              leaderId;
    public final String            leaderName;
    public final List<FactionInfo> factions;

    public ClanUpdatePacket(UUID clanId, UUID leaderId, String leaderName, List<FactionInfo> factions) {
        this.clanId     = clanId;
        this.leaderId   = leaderId;
        this.leaderName = leaderName;
        this.factions   = factions;
    }

    public ClanUpdatePacket(FriendlyByteBuf buf) {
        this.clanId     = buf.readUUID();
        this.leaderId   = buf.readUUID();
        this.leaderName = buf.readUtf(32);
        int size = buf.readVarInt();
        if (size < 0 || size > 256) throw new IllegalStateException("Oversized clan update packet: " + size);
        this.factions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID   fid   = buf.readUUID();
            String fname = buf.readUtf(64);
            String lname = buf.readUtf(32);
            int    mc    = buf.readVarInt();
            factions.add(new FactionInfo(fid, fname, lname, mc));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(clanId);
        buf.writeUUID(leaderId);
        buf.writeUtf(leaderName);
        buf.writeVarInt(factions.size());
        for (FactionInfo f : factions) {
            buf.writeUUID(f.factionId);
            buf.writeUtf(f.factionName);
            buf.writeUtf(f.leaderName);
            buf.writeVarInt(f.memberCount);
        }
    }

    public static ClanUpdatePacket from(ClanData clan, TerritoryManager tm, MinecraftServer server) {
        String leaderName = resolveName(server, clan.getLeaderId());
        List<FactionInfo> list = new ArrayList<>();
        for (UUID fid : clan.getMemberFactionIds()) {
            FactionData faction = tm.getFaction(fid);
            if (faction == null) continue;
            UUID leaderId = faction.getLeader();
            String fl = leaderId != null ? resolveName(server, leaderId) : "?";
            list.add(new FactionInfo(fid, faction.getName(), fl, faction.getMembers().size()));
        }
        return new ClanUpdatePacket(clan.getId(), clan.getLeaderId(), leaderName, list);
    }

    private static String resolveName(MinecraftServer server, UUID uuid) {
        ServerPlayer p = server.getPlayerList().getPlayer(uuid);
        return p != null ? p.getName().getString() : uuid.toString().substring(0, 8);
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() ->
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () ->
                mc.sayda.creraces.client.screen.ClanManagementScreen.update(this)));
    }
}
