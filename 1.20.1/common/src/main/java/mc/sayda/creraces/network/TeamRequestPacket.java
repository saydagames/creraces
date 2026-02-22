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
        CREATE, JOIN, LEAVE, INVITE, TOGGLE_FRIENDLY_FIRE
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
            this.data = buf.readUtf();
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
                case CREATE -> RaceTeamManager.createTeam(player, data);
                case JOIN -> {
                    RaceTeamManager.getPendingInvite(player.getUUID()).ifPresent(teamId -> {
                        RaceTeamManager.joinTeam(player, teamId);
                        RaceTeamManager.clearInvite(player.getUUID());
                    });
                }
                case LEAVE -> RaceTeamManager.leaveTeam(player);
                case INVITE -> {
                    ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(data);
                    if (target != null) {
                        RaceTeamManager.getPlayerTeam(player).ifPresent(team -> {
                            RaceTeamManager.invitePlayer(player, target, team.getId());
                        });
                    }
                }
                case TOGGLE_FRIENDLY_FIRE -> RaceTeamManager.toggleFriendlyFire(player);
            }
        });
    }
}
