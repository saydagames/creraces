package mc.sayda.creraces.team;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages race teams and party logic on the server.
 */
public class RaceTeamManager {
    private static final Map<UUID, RaceTeam> TEAMS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PENDING_INVITES = new ConcurrentHashMap<>(); // Invitee -> TeamId

    public static void broadcastUpdate(RaceTeam team, net.minecraft.server.MinecraftServer server) {
        List<mc.sayda.creraces.network.TeamUpdatePacket.MemberInfo> memberInfos = new ArrayList<>();
        for (UUID memberId : team.getMembers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                memberInfos.add(new mc.sayda.creraces.network.TeamUpdatePacket.MemberInfo(
                        memberId,
                        member.getName().getString(),
                        memberId.equals(team.getLeader())));
            }
        }

        mc.sayda.creraces.network.TeamUpdatePacket pkt = new mc.sayda.creraces.network.TeamUpdatePacket(memberInfos,
                team.isFriendlyFire(), null);

        for (UUID memberId : team.getMembers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                mc.sayda.creraces.network.BoundaryHandler.sendTeamUpdate(member, pkt);
            }
        }
    }

    public static void syncInvite(ServerPlayer player) {
        String inviteTeamName = getPendingInvite(player.getUUID())
                .flatMap(RaceTeamManager::getTeam)
                .map(RaceTeam::getName).orElse(null);
        mc.sayda.creraces.network.BoundaryHandler.sendTeamUpdate(player,
                new mc.sayda.creraces.network.TeamUpdatePacket(Collections.emptyList(), false, inviteTeamName));
    }

    public static class RaceTeam {
        private final UUID id;
        private String name;
        private UUID leader;
        private boolean friendlyFire = false;
        private final Set<UUID> members = ConcurrentHashMap.newKeySet();

        public RaceTeam(UUID id, String name, UUID leader) {
            this.id = id;
            this.name = name;
            this.leader = leader;
            this.members.add(leader);
        }

        public UUID getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public UUID getLeader() {
            return leader;
        }

        public void setLeader(UUID leader) {
            this.leader = leader;
        }

        public Set<UUID> getMembers() {
            return members;
        }

        public boolean isFriendlyFire() {
            return friendlyFire;
        }

        public void setFriendlyFire(boolean friendlyFire) {
            this.friendlyFire = friendlyFire;
        }
    }

    public static boolean canHurt(net.minecraft.world.entity.LivingEntity victim,
            net.minecraft.world.entity.LivingEntity attacker) {
        if (victim == attacker) {
            return false;
        }

        // Tame check
        if (victim instanceof net.minecraft.world.entity.OwnableEntity ownable && ownable.getOwner() == attacker) {
            return false;
        }

        if (victim instanceof Player vPlayer && attacker instanceof Player aPlayer) {
            IPlayerVariables vVars = DataUtils.getVariables(vPlayer).orElse(null);
            IPlayerVariables aVars = DataUtils.getVariables(aPlayer).orElse(null);

            if (vVars != null && aVars != null && vVars.getTeamId() != null
                    && vVars.getTeamId().equals(aVars.getTeamId())) {
                RaceTeam team = TEAMS.get(vVars.getTeamId());
                if (team != null && !team.isFriendlyFire()) {
                    return false;
                }
            }
        }

        // Handle tames of teammates
        if (victim instanceof net.minecraft.world.entity.OwnableEntity ownable
                && ownable.getOwner() instanceof Player oPlayer && attacker instanceof Player aPlayer) {
            IPlayerVariables oVars = DataUtils.getVariables(oPlayer).orElse(null);
            IPlayerVariables aVars = DataUtils.getVariables(aPlayer).orElse(null);

            if (oVars != null && aVars != null && oVars.getTeamId() != null
                    && oVars.getTeamId().equals(aVars.getTeamId())) {
                RaceTeam team = TEAMS.get(oVars.getTeamId());
                if (team != null && !team.isFriendlyFire()) {
                    return false;
                }
            }
        }

        return true;
    }

    public static RaceTeam createTeam(ServerPlayer leader, String name) {
        UUID teamId = UUID.randomUUID();
        RaceTeam team = new RaceTeam(teamId, name, leader.getUUID());
        TEAMS.put(teamId, team);

        applyTeamToPlayer(leader, team);
        broadcastUpdate(team, leader.getServer());
        return team;
    }

    public static void joinTeam(ServerPlayer player, UUID teamId) {
        RaceTeam team = TEAMS.get(teamId);
        if (team != null) {
            team.getMembers().add(player.getUUID());
            applyTeamToPlayer(player, team);
            broadcastUpdate(team, player.getServer());
        }
    }

    public static void leaveTeam(ServerPlayer player) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            UUID teamId = vars.getTeamId();
            if (teamId != null) {
                RaceTeam team = TEAMS.get(teamId);
                if (team != null) {
                    team.getMembers().remove(player.getUUID());
                    if (team.getMembers().isEmpty()) {
                        TEAMS.remove(teamId);
                    } else {
                        if (team.getLeader().equals(player.getUUID())) {
                            team.setLeader(team.getMembers().iterator().next());
                        }
                        broadcastUpdate(team, player.getServer());
                    }
                }
                vars.setTeamId(null);
                vars.setTeamName("");
                mc.sayda.creraces.network.BoundaryHandler.sendTeamUpdate(player,
                        new mc.sayda.creraces.network.TeamUpdatePacket(Collections.emptyList(), false, null));
            }
        });
    }

    private static void applyTeamToPlayer(ServerPlayer player, RaceTeam team) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            vars.setTeamId(team.getId());
            vars.setTeamName(team.getName());
        });
    }

    public static Optional<RaceTeam> getTeam(UUID teamId) {
        return Optional.ofNullable(TEAMS.get(teamId));
    }

    public static void invitePlayer(ServerPlayer inviter, ServerPlayer invitee, UUID teamId) {
        PENDING_INVITES.put(invitee.getUUID(), teamId);
        String inviterName = inviter.getName().getString();
        String teamName = getTeam(teamId).map(RaceTeam::getName).orElse("a team");
        invitee.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                String.format("\u00A76%s invited you to join \u00A7b%s\u00A76! Use /creraces team to accept.",
                        inviterName, teamName)));
        syncInvite(invitee);
    }

    public static Optional<UUID> getPendingInvite(UUID invitee) {
        return Optional.ofNullable(PENDING_INVITES.get(invitee));
    }

    public static void clearInvite(UUID invitee) {
        PENDING_INVITES.remove(invitee);
    }

    public static Optional<RaceTeam> getPlayerTeam(ServerPlayer player) {
        return DataUtils.getVariables(player).flatMap(vars -> Optional.ofNullable(vars.getTeamId()))
                .flatMap(RaceTeamManager::getTeam);
    }

    public static void toggleFriendlyFire(ServerPlayer player) {
        getPlayerTeam(player).ifPresent(team -> {
            if (team.getLeader().equals(player.getUUID())) {
                team.setFriendlyFire(!team.isFriendlyFire());
                broadcastUpdate(team, player.getServer());
            }
        });
    }
}
