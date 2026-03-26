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
    private UUID targetUuid; // Target player's UUID for role actions

    public enum Action {
        CREATE, JOIN, LEAVE, INVITE, TOGGLE_FRIENDLY_FIRE, PROMOTE, DEMOTE, KICK
    }

    public TeamRequestPacket(Action action, String data) {
        this.action = action;
        this.data = data;
    }

    public TeamRequestPacket(Action action, UUID targetUuid) {
        this.action = action;
        this.targetUuid = targetUuid;
    }

    public TeamRequestPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        if (buf.readBoolean())
            this.data = buf.readUtf(256);
        if (buf.readBoolean())
            this.targetUuid = buf.readUUID();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeBoolean(data != null);
        if (data != null)
            buf.writeUtf(data);
        buf.writeBoolean(targetUuid != null);
        if (targetUuid != null)
            buf.writeUUID(targetUuid);
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
                    int teamMax = mc.sayda.creraces.config.CreRacesConfig.NETWORK_TEAM_NAME_MAX_LEN.get();
                    if (data.length() > teamMax) {
                        CreRaces.LOGGER.warn("Player {} tried to create team with oversized name ({} chars)",
                                player.getName().getString(), data.length());
                        data = data.substring(0, teamMax);
                    }
                    RaceTeamManager.createTeam(player, data);
                }
                case JOIN -> {
                    var invite = RaceTeamManager.getPendingInvite(player.getUUID());
                    if (invite.isPresent()) {
                        RaceTeamManager.joinTeam(player, invite.get());
                        RaceTeamManager.clearInvite(player.getUUID());
                    } else {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.creraces.team.no_invite"));
                    }
                }
                case LEAVE -> RaceTeamManager.leaveTeam(player);
                case INVITE -> {
                    if (data == null || data.isBlank())
                        return;
                    int playerMax = mc.sayda.creraces.config.CreRacesConfig.NETWORK_TEAM_NAME_MAX_LEN.get();
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
                    } else {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.creraces.team.player_not_found", data));
                    }
                }
                case TOGGLE_FRIENDLY_FIRE -> RaceTeamManager.toggleFriendlyFire(player);
                case PROMOTE -> {
                    if (targetUuid != null) {
                        RaceTeamManager.promoteMember(player, targetUuid, player.getServer());
                    }
                }
                case DEMOTE -> {
                    if (targetUuid != null) {
                        RaceTeamManager.demoteMember(player, targetUuid, player.getServer());
                    }
                }
                case KICK -> {
                    if (targetUuid != null) {
                        RaceTeamManager.kickMember(player, targetUuid, player.getServer());
                    }
                }
            }
        });
    }
}
