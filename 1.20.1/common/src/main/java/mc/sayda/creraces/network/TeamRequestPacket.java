package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.team.RaceTeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client-to-Server packet for team requests.
 */
public class TeamRequestPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "team_request");

    private final Action action;
    private String data; // Team name or player name
    private UUID teamId;

    public enum Action {
        CREATE, JOIN, LEAVE, INVITE, TOGGLE_FRIENDLY_FIRE, PROMOTE, DEMOTE
    }

    public TeamRequestPacket(Action action, String data) {
        this.action = action;
        this.data = data;
    }

    public TeamRequestPacket(Action action, UUID teamId) {
        this.action = action;
        this.teamId = teamId;
    }

    public TeamRequestPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        if (buf.readBoolean())
            this.data = buf.readUtf(32767); // Safe
                                            // upper
                                            // bound
                                            // for
                                            // reading,
                                            // validated
                                            // further
                                            // in
                                            // handle()
        if (buf.readBoolean())
            this.teamId = buf.readUUID();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeBoolean(data != null);
        if (data != null)
            buf.writeUtf(data);
        buf.writeBoolean(teamId != null);
        if (teamId != null)
            buf.writeUUID(teamId);
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player))
                return;
            switch (action) {
                case CREATE -> {
                    if (data == null || data.isBlank())
                        return;
                    int teamMax = 16;
                    if (data.length() > teamMax) {
                        CreRaces.LOGGER.warn("Player {} tried to create team with oversized name ({} chars)",
                                player.getName().getString(), data.length());
                        data = data.substring(0, teamMax);
                    }
                    RaceTeamManager.createTeam(player, data);
                }
                case JOIN -> {
                    RaceTeamManager.getPendingInvite(player.getUUID()).ifPresent(teamId -> {
                        RaceTeamManager.joinTeam(player, teamId);
                        RaceTeamManager.clearInvite(player.getUUID());
                    });
                }
                case LEAVE -> RaceTeamManager.leaveTeam(player);
                case INVITE -> {
                    if (data == null || data.isBlank())
                        return;
                    int playerMax = 16;
                    if (data.length() > playerMax) {
                        CreRaces.LOGGER.warn("Player {} tried to invite oversized name ({} chars)",
                                player.getName().getString(), data.length());
                        data = data.substring(0, playerMax);
                    }
                    ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(data);
                    if (target != null) {
                        RaceTeamManager.getPlayerTeam(player).ifPresent(team -> {
                            RaceTeamManager.invitePlayer(player, target, team.getId());
                        });
                    }
                }
                case TOGGLE_FRIENDLY_FIRE -> RaceTeamManager.toggleFriendlyFire(player);
                case PROMOTE -> {
                    if (teamId != null) {
                        RaceTeamManager.promoteMember(player, teamId, player.getServer());
                    }
                }
                case DEMOTE -> {
                    if (teamId != null) {
                        RaceTeamManager.demoteMember(player, teamId, player.getServer());
                    }
                }
            }
        });
    }
}
