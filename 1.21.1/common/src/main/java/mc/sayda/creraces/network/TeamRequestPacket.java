package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.team.RaceTeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** C2S: a team action from the team screen. */
public class TeamRequestPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "team_request");

    private static final int DATA_MAX_LEN = 256;

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
            this.data = buf.readUtf(DATA_MAX_LEN);
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

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player))
                return;
            switch (action) {
                case CREATE -> {
                    if (data == null || data.isBlank())
                        return;
                    int nameMaxLen = CreRacesConfig.NETWORK_TEAM_NAME_MAX_LEN.get();
                    if (data.length() > nameMaxLen) {
                        CreRaces.LOGGER.warn("Player {} tried to create team with oversized name ({} chars)",
                                player.getName().getString(), data.length());
                        data = data.substring(0, nameMaxLen);
                    }
                    RaceTeamManager.createTeam(player, data);
                }
                case JOIN -> {
                    var invite = RaceTeamManager.getPendingInvite(player.getUUID());
                    if (invite.isPresent()) {
                        RaceTeamManager.joinTeam(player, invite.get());
                        RaceTeamManager.clearInvite(player.getUUID());
                    } else {
                        player.sendSystemMessage(Component.translatable("msg.creraces.team.no_invite"));
                    }
                }
                case LEAVE -> RaceTeamManager.leaveTeam(player);
                case INVITE -> invite(player);
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

    private void invite(ServerPlayer player) {
        if (data == null || data.isBlank())
            return;
        int nameMaxLen = CreRacesConfig.NETWORK_TEAM_NAME_MAX_LEN.get();
        if (data.length() > nameMaxLen) {
            CreRaces.LOGGER.warn("Player {} tried to invite oversized name ({} chars)",
                    player.getName().getString(), data.length());
            data = data.substring(0, nameMaxLen);
        }
        ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(data);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("msg.creraces.team.player_not_found", data));
            return;
        }
        if (CreRacesConfig.TEAM_REQUIRE_SAME_RACE.get()) {
            ResourceLocation inviterRace = DataUtils.getVariables(player).map(IPlayerVariables::getRace).orElse(null);
            ResourceLocation targetRace = DataUtils.getVariables(target).map(IPlayerVariables::getRace).orElse(null);
            if (inviterRace == null || !inviterRace.equals(targetRace)) {
                player.sendSystemMessage(Objects.requireNonNull(
                        Component.translatable("msg.creraces.team.different_race")));
                return;
            }
        }
        RaceTeamManager.getPlayerTeam(player).ifPresent(team -> RaceTeamManager.invitePlayer(player, target, team.getId()));
    }
}
