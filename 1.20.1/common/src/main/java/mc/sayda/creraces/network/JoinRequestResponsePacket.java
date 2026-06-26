package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.territory.FactionData;
import mc.sayda.creraces.territory.FactionRank;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * C2S: officer accepts or denies a faction join request.
 */
@SuppressWarnings("null")
public class JoinRequestResponsePacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "join_request_response");

    private final UUID applicantUuid;
    private final boolean accept;

    public JoinRequestResponsePacket(UUID applicantUuid, boolean accept) {
        this.applicantUuid = applicantUuid;
        this.accept = accept;
    }

    public JoinRequestResponsePacket(FriendlyByteBuf buf) {
        this.applicantUuid = buf.readUUID();
        this.accept = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(applicantUuid);
        buf.writeBoolean(accept);
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer officer)) return;
            TerritoryManager tm = TerritoryManager.get();
            UUID officerId = officer.getUUID();
            if (!tm.hasFaction(officerId)) return;

            UUID factionId = tm.getFactionId(officerId);
            FactionData faction = tm.getFaction(factionId);
            if (faction == null) return;
            FactionRank rank = faction.getRank(officerId);
            if (rank == null || !rank.isAtLeast(FactionRank.OFFICER)) return;

            if (accept) {
                // Reject if the applicant already belongs to a faction — addMember would
                // silently overwrite their playerFaction binding otherwise.
                if (tm.hasFaction(applicantUuid)) {
                    officer.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                            "creraces.territory.player_has_faction", applicantUuid));
                    return;
                }
                tm.addMember(factionId, applicantUuid, FactionRank.MEMBER);
                ServerPlayer applicant = officer.getServer().getPlayerList().getPlayer(applicantUuid);
                if (applicant != null) {
                    applicant.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                            "creraces.territory.joined_faction", faction.getName()));
                }
                // Broadcast updated roster
                FactionUpdatePacket upd = FactionUpdatePacket.from(faction, officer.getServer());
                for (UUID memberId : faction.getMembers().keySet()) {
                    ServerPlayer online = officer.getServer().getPlayerList().getPlayer(memberId);
                    if (online != null) BoundaryHandler.sendFactionUpdate(online, upd);
                }
            }
        });
    }
}
