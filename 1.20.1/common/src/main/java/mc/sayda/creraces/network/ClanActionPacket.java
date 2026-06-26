package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.territory.ClanData;
import mc.sayda.creraces.territory.FactionData;
import mc.sayda.creraces.territory.FactionRank;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * C2S: client requests a clan operation.
 */
@SuppressWarnings("null")
public class ClanActionPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "clan_action");

    private static final int NAME_MAX = 64;

    public enum Action { CREATE, INVITE_FACTION, KICK_FACTION, TRANSFER_LEAD, DISBAND, VIEW }

    private final Action action;
    private String name;
    private UUID targetUuid;

    public ClanActionPacket(Action action) { this.action = action; }
    public ClanActionPacket(Action action, String name) { this.action = action; this.name = name; }
    public ClanActionPacket(Action action, UUID targetUuid) { this.action = action; this.targetUuid = targetUuid; }

    public ClanActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        if (buf.readBoolean()) this.name = buf.readUtf(NAME_MAX);
        if (buf.readBoolean()) this.targetUuid = buf.readUUID();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeBoolean(name != null);
        if (name != null) buf.writeUtf(name);
        buf.writeBoolean(targetUuid != null);
        if (targetUuid != null) buf.writeUUID(targetUuid);
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            TerritoryManager tm = TerritoryManager.get();
            UUID playerId = player.getUUID();

            switch (action) {
                case CREATE -> {
                    if (tm.getClanId(playerId) != null) return; // already in a clan
                    if (!tm.hasFaction(playerId)) return;
                    UUID fId = tm.getFactionId(playerId);
                    FactionData f = tm.getFaction(fId);
                    if (f == null || f.getRank(playerId) != FactionRank.LEADER) return;
                    ResourceLocation raceId = DataUtils.getVariables(player)
                            .map(IPlayerVariables::getRace)
                            .orElse(new ResourceLocation("creraces", "none"));
                    UUID newClanId = tm.createClan(raceId, playerId);
                    // Refresh faction screen (clanId is now set) then open clan screen
                    FactionData updated = tm.getFaction(fId);
                    if (updated != null)
                        BoundaryHandler.sendFactionUpdate(player, FactionUpdatePacket.from(updated, player.getServer()));
                    ClanData newClan = tm.getClans().get(newClanId);
                    if (newClan != null) {
                        BoundaryHandler.sendClanUpdate(player, ClanUpdatePacket.from(newClan, tm, player.getServer()));
                        BoundaryHandler.sendOpenClanManage(player);
                    }
                }
                case INVITE_FACTION -> {
                    if (name == null || name.isBlank()) return;
                    UUID clanId = tm.getClanId(playerId);
                    if (clanId == null) return;
                    ClanData clan = tm.getClans().get(clanId);
                    if (clan == null || !clan.getLeaderId().equals(playerId)) return;
                    ServerPlayer leader = player.getServer().getPlayerList().getPlayerByName(name);
                    if (leader == null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "msg.creraces.team.player_not_found", name));
                        return;
                    }
                    FactionData targetFd = null;
                    for (FactionData fd : tm.getFactions().values()) {
                        if (fd.getLeader() != null && fd.getLeader().equals(leader.getUUID())) {
                            targetFd = fd;
                            break;
                        }
                    }
                    if (targetFd == null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "creraces.territory.player_has_no_faction", name));
                        return;
                    }
                    if (targetFd.getClanId() != null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "creraces.territory.faction_already_in_clan", name));
                        return;
                    }
                    tm.addFactionToClan(clanId, targetFd.getId());
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                            "creraces.territory.faction_invited_to_clan", name));
                }
                case KICK_FACTION -> {
                    if (targetUuid == null) return;
                    UUID clanId = tm.getClanId(playerId);
                    if (clanId == null) return;
                    ClanData clan = tm.getClans().get(clanId);
                    if (clan == null || !clan.getLeaderId().equals(playerId)) return;
                    if (!clan.getMemberFactionIds().contains(targetUuid)) return;
                    tm.removeFactionFromClan(clanId, targetUuid);
                }
                case TRANSFER_LEAD -> {
                    if (targetUuid == null) return;
                    UUID clanId = tm.getClanId(playerId);
                    if (clanId == null) return;
                    ClanData clan = tm.getClans().get(clanId);
                    if (clan == null || !clan.getLeaderId().equals(playerId)) return;
                    // Target must be the leader of a faction that belongs to this clan.
                    boolean targetIsClanLeader = clan.getMemberFactionIds().stream().anyMatch(fId -> {
                        FactionData fd = tm.getFaction(fId);
                        return fd != null && targetUuid.equals(fd.getLeader());
                    });
                    if (!targetIsClanLeader) return;
                    clan.setLeaderId(targetUuid);
                }
                case DISBAND -> {
                    UUID clanId = tm.getClanId(playerId);
                    if (clanId == null) return;
                    ClanData clan = tm.getClans().get(clanId);
                    if (clan == null || !clan.getLeaderId().equals(playerId)) return;
                    tm.disbandClan(player.getServer(), clanId);
                }
                case VIEW -> {
                    UUID clanId = tm.getClanId(playerId);
                    if (clanId == null) return;
                    ClanData clan = tm.getClans().get(clanId);
                    if (clan == null) return;
                    BoundaryHandler.sendClanUpdate(player,
                            ClanUpdatePacket.from(clan, tm, player.getServer()));
                    BoundaryHandler.sendOpenClanManage(player);
                }
            }
        });
    }
}
