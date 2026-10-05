package mc.sayda.creraces.team;

import com.mojang.authlib.GameProfile;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.TeamUpdatePacket;
import mc.sayda.creraces.util.CombatUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages race teams and party logic on the server.
 */
@SuppressWarnings("null")
public class RaceTeamManager {
    private static final Map<UUID, RaceTeam> TEAMS = new ConcurrentHashMap<>();
    private static final Map<UUID, InviteData> PENDING_INVITES = new ConcurrentHashMap<>(); // Invitee -> InviteData
    // Players kicked while offline; their stale team data is cleared when they next join.
    private static final Set<UUID> PENDING_REMOVALS = ConcurrentHashMap.newKeySet();
    private static final int INVITE_EXPIRY_TICKS = 1200;

    public enum Role {
        MEMBER, OFFICER, LEADER
    }

    public static void broadcastUpdate(RaceTeam team, MinecraftServer server) {
        List<TeamUpdatePacket.MemberInfo> memberInfos = new ArrayList<>();
        for (UUID memberId : team.getMembers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(Objects.requireNonNull(memberId));
            String name;
            if (member != null) {
                name = member.getName().getString();
            } else {
                name = server.getProfileCache()
                        .get(memberId)
                        .map(GameProfile::getName)
                        .orElse("Unknown");
            }
            memberInfos.add(new TeamUpdatePacket.MemberInfo(
                    memberId,
                    name,
                    team.getRole(memberId)));
        }

        TeamUpdatePacket pkt = new TeamUpdatePacket(memberInfos,
                team.isFriendlyFire(), null);

        for (UUID memberId : team.getMembers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(Objects.requireNonNull(memberId));
            if (member != null) {
                BoundaryHandler.sendTeamUpdate(member, pkt);
            }
        }
    }

    public static void syncInvite(ServerPlayer player) {
        String inviteTeamName = getPendingInvite(player.getUUID())
                .flatMap(RaceTeamManager::getTeam)
                .map(RaceTeam::getName).orElse(null);
        BoundaryHandler.sendTeamUpdate(player,
                new TeamUpdatePacket(Collections.emptyList(), false, inviteTeamName));
    }

    public static class RaceTeam {
        private final UUID id;
        private String name;
        private UUID leader;
        private boolean friendlyFire = false;
        private final Set<UUID> members = ConcurrentHashMap.newKeySet();
        private final Map<UUID, Role> memberRoles = new ConcurrentHashMap<>();

        public RaceTeam(UUID id, String name, UUID leader) {
            this.id = id;
            this.name = name;
            this.leader = leader;
            this.members.add(leader);
            this.memberRoles.put(leader, Role.LEADER);
        }

        public UUID getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = clampTeamName(name);
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

        public Role getRole(UUID uuid) {
            return memberRoles.getOrDefault(uuid, Role.MEMBER);
        }

        public void setRole(UUID uuid, Role role) {
            if (role == Role.LEADER) {
                // Demote old leader
                memberRoles.put(leader, Role.OFFICER);
                leader = uuid;
            }
            memberRoles.put(uuid, role);
        }

        public Map<UUID, Role> getMemberRoles() {
            return memberRoles;
        }
    }

    public static boolean canHurt(LivingEntity victim,
            LivingEntity attacker) {
        if (victim == attacker) {
            return true;
        }

        // Use root owner resolution to handle players, tamed animals, and servants
        Player vOwner = CombatUtils.getRootOwner(victim);
        Player aOwner = CombatUtils.getRootOwner(attacker);

        if (vOwner != null && aOwner != null) {
            // Same owner: a player and their own pets or servants never hurt each other.
            if (vOwner.getUUID().equals(aOwner.getUUID())) {
                return false;
            }

            IPlayerVariables vVars = DataUtils.getVariables(vOwner).orElse(null);
            IPlayerVariables aVars = DataUtils.getVariables(aOwner).orElse(null);

            if (vVars != null && aVars != null) {
                UUID vTeam = vVars.getTeamId();
                UUID aTeam = aVars.getTeamId();

                if (vTeam != null && vTeam.equals(aTeam)) {
                    RaceTeam team = TEAMS.get(vTeam);
                    return team != null && team.isFriendlyFire();
                }
                // Different teams, or either side has none: always hostile.
                return true;
            }

            // Only reached if a player somehow has no variables.
            if (vOwner.isAlliedTo(aOwner)) {
                return false;
            }
        }

        // Fallback when either side has no player owner: a pet never hurts its direct owner.
        if (victim instanceof OwnableEntity ownable && ownable.getOwner() == attacker) {
            return false;
        }

        return true;
    }

    public static RaceTeam createTeam(ServerPlayer leader, String name) {
        // Force leave current team if any
        leaveTeam(leader);

        UUID teamId = UUID.randomUUID();
        String finalName = clampTeamName(name);
        RaceTeam team = new RaceTeam(teamId, finalName, leader.getUUID());
        TEAMS.put(teamId, team);

        applyTeamToPlayer(leader, team);
        broadcastUpdate(team, leader.getServer());
        save(leader.getServer());
        leader.sendSystemMessage(
                Component.translatable("msg.creraces.team.created", finalName));
        return team;
    }

    public static void joinTeam(ServerPlayer player, UUID teamId) {
        RaceTeam team = TEAMS.get(teamId);
        if (team != null) {
            if (team.getMembers().size() >= CreRacesConfig.TEAM_MAX_SIZE.get()) {
                player.sendSystemMessage(Component.translatable("msg.creraces.team.full"));
                return;
            }

            // Force leave current team if any
            leaveTeam(player);

            team.getMembers().add(player.getUUID());
            team.setRole(player.getUUID(), Role.MEMBER);
            applyTeamToPlayer(player, team);
            broadcastUpdate(team, player.getServer());
            save(player.getServer());
            player.sendSystemMessage(
                    Component.translatable("msg.creraces.team.joined", team.getName()));
        }
    }

    public static void leaveTeam(ServerPlayer player) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            UUID teamId = vars.getTeamId();
            if (teamId != null) {
                RaceTeam team = TEAMS.get(teamId);
                if (team != null) {
                    team.getMembers().remove(player.getUUID());
                    team.getMemberRoles().remove(player.getUUID());
                    if (team.getMembers().isEmpty()) {
                        TEAMS.remove(teamId);
                    } else {
                        if (team.getLeader().equals(player.getUUID())) {
                            // Prioritize promoting an OFFICER (excluding the leaving player)
                            UUID next = team.getMemberRoles().entrySet().stream()
                                    .filter(e -> e.getValue() == Role.OFFICER && !e.getKey().equals(player.getUUID()))
                                    .map(Map.Entry::getKey)
                                    .findFirst()
                                    .orElseGet(() -> team.getMembers().stream()
                                            .filter(m -> !m.equals(player.getUUID()))
                                            .findFirst()
                                            .orElse(null));

                            if (next != null) {
                                // setLeader first, or setRole would re-add the departing leader as an officer.
                                team.setLeader(next);
                                team.setRole(next, Role.LEADER);
                            }
                        }
                        broadcastUpdate(team, player.getServer());
                    }
                    save(player.getServer());
                }
                clearTeamData(player, vars);
                player.sendSystemMessage(Component.translatable("msg.creraces.team.left"));
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
        RaceTeam team = TEAMS.get(teamId);
        if (team == null)
            return;

        Role role = team.getRole(inviter.getUUID());
        if (role != Role.LEADER && role != Role.OFFICER) {
            sendNoPermission(inviter);
            return;
        }

        PENDING_INVITES.put(invitee.getUUID(), new InviteData(teamId, inviter.getServer().getTickCount()));
        invitee.sendSystemMessage(Component.translatable("msg.creraces.team.invite_received",
                inviter.getName().getString(), team.getName()));
        inviter.sendSystemMessage(Component.translatable("msg.creraces.team.invited",
                invitee.getName().getString()));
        syncInvite(invitee);
    }

    public static Optional<UUID> getPendingInvite(UUID invitee) {
        return Optional.ofNullable(PENDING_INVITES.get(invitee)).map(data -> data.teamId);
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
            Role role = team.getRole(player.getUUID());
            if (role == Role.LEADER || role == Role.OFFICER) {
                team.setFriendlyFire(!team.isFriendlyFire());
                broadcastUpdate(team, player.getServer());
                save(player.getServer());
            } else {
                sendNoPermission(player);
            }
        });
    }

    public static void promoteMember(ServerPlayer actor, UUID targetId, MinecraftServer server) {
        getPlayerTeam(actor).ifPresent(team -> {
            if (team.getRole(actor.getUUID()) != Role.LEADER) {
                sendNoPermission(actor);
                return;
            }
            if (!team.getMembers().contains(targetId))
                return;

            Role current = team.getRole(targetId);
            if (current == Role.MEMBER) {
                team.setRole(targetId, Role.OFFICER);
            } else if (current == Role.OFFICER) {
                team.setRole(targetId, Role.LEADER);
                // setRole(LEADER) demotes the acting leader to officer.
            }
            String targetName = onlineNameOr(server, targetId, "Unknown");
            Role newRole = team.getRole(targetId);
            actor.sendSystemMessage(
                    Component.translatable("msg.creraces.team.promoted", targetName,
                            Component
                                    .translatable("gui.creraces.team.role." + newRole.name().toLowerCase())));
            broadcastUpdate(team, server);
            save(server);
        });
    }

    public static void demoteMember(ServerPlayer actor, UUID targetId, MinecraftServer server) {
        getPlayerTeam(actor).ifPresent(team -> {
            if (team.getRole(actor.getUUID()) != Role.LEADER) {
                sendNoPermission(actor);
                return;
            }
            if (!team.getMembers().contains(targetId))
                return;

            Role current = team.getRole(targetId);
            if (current == Role.OFFICER) {
                team.setRole(targetId, Role.MEMBER);
            } else if (current == Role.LEADER) {
                // A leader steps down by promoting someone else or leaving.
                actor.sendSystemMessage(
                        Objects.requireNonNull(Component
                                .translatable("msg.creraces.team.cannot_demote_leader")));
                return;
            }
            String targetName = onlineNameOr(server, targetId, "Unknown");
            Role newRole = team.getRole(targetId);
            actor.sendSystemMessage(
                    Component.translatable("msg.creraces.team.demoted", targetName,
                            Component
                                    .translatable("gui.creraces.team.role." + newRole.name().toLowerCase())));
            broadcastUpdate(team, server);
            save(server);
        });
    }

    public static void kickMember(ServerPlayer actor, UUID targetId, MinecraftServer server) {
        getPlayerTeam(actor).ifPresent(team -> {
            Role actorRole = team.getRole(actor.getUUID());
            Role targetRole = team.getRole(targetId);

            if (actorRole == Role.MEMBER) {
                sendNoPermission(actor);
                return;
            }

            if (actorRole == Role.OFFICER && targetRole != Role.MEMBER) {
                sendNoPermission(actor);
                return;
            }

            // Leaving is its own action.
            if (actor.getUUID().equals(targetId)) {
                return;
            }

            team.getMembers().remove(targetId);
            team.getMemberRoles().remove(targetId);

            ServerPlayer target = server.getPlayerList().getPlayer(targetId);
            if (target != null) {
                DataUtils.getVariables(target).ifPresent(vars -> clearTeamData(target, vars));
                target.sendSystemMessage(Component.translatable("msg.creraces.team.kicked"));
            } else {
                PENDING_REMOVALS.add(targetId);
            }

            String targetName = target != null ? target.getName().getString() : targetId.toString();
            actor.sendSystemMessage(Component.translatable("msg.creraces.team.actor_kicked",
                    targetName));
            broadcastUpdate(team, server);
            save(server);
        });
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 == 0) {
            int currentTick = server.getTickCount();
            PENDING_INVITES.entrySet().removeIf(entry -> currentTick - entry.getValue().timestamp > INVITE_EXPIRY_TICKS);
        }
    }

    public static void handlePlayerJoin(ServerPlayer player) {
        if (PENDING_REMOVALS.remove(player.getUUID())) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                clearTeamData(player, vars);
                CreRaces.LOGGER.info("Cleared stale team data for kicked player: {}", player.getName().getString());
            });
        }

        getPlayerTeam(player).ifPresent(team -> broadcastUpdate(team, player.getServer()));
    }

    private static void sendNoPermission(ServerPlayer player) {
        player.sendSystemMessage(Objects.requireNonNull(Component.translatable("msg.creraces.team.no_perm")));
    }

    /** Clears the player's team fields and tells their client they are no longer in a team. */
    private static void clearTeamData(ServerPlayer player, IPlayerVariables vars) {
        vars.setTeamId(null);
        vars.setTeamName("");
        BoundaryHandler.sendTeamUpdate(player, new TeamUpdatePacket(Collections.emptyList(), false, null));
    }

    private static String clampTeamName(String name) {
        int maxLen = CreRacesConfig.NETWORK_TEAM_NAME_MAX_LEN.get();
        return name.length() > maxLen ? name.substring(0, maxLen) : name;
    }

    private static String onlineNameOr(MinecraftServer server, UUID playerId, String fallback) {
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        return online != null ? online.getName().getString() : fallback;
    }

    private static class InviteData {
        final UUID teamId;
        final int timestamp;

        InviteData(UUID teamId, int timestamp) {
            this.teamId = teamId;
            this.timestamp = timestamp;
        }
    }

    // Persistence: vanilla SavedData stored in <world>/data/creraces_teams.dat.

    private static final String DATA_ID = "creraces_teams";

    // Holds no state of its own: TEAMS is the source of truth and this is only the handle to mark dirty.
    private static Data CURRENT_DATA;

    private static class Data extends SavedData {
        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag teamsList = new ListTag();
            TEAMS.values().forEach(team -> teamsList.add(serializeTeam(team)));
            tag.put("teams", teamsList);
            return tag;
        }
    }

    /** Marks the data dirty and flushes it immediately rather than waiting for the next autosave. */
    public static void save(MinecraftServer server) {
        if (CURRENT_DATA != null) {
            CURRENT_DATA.setDirty();
            server.overworld().getDataStorage().save();
        }
    }

    public static void load(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        CURRENT_DATA = overworld.getDataStorage().computeIfAbsent(
                tag -> { populateTeamsFromTag(server, tag); return new Data(); },
                Data::new,
                DATA_ID);
        CreRaces.LOGGER.info("Loaded {} team(s).", TEAMS.size());
    }

    private static CompoundTag serializeTeam(RaceTeam team) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", team.getId().toString());
        tag.putString("name", team.getName());
        tag.putString("leader", team.getLeader().toString());
        tag.putBoolean("friendlyFire", team.isFriendlyFire());
        ListTag members = new ListTag();
        team.getMembers().forEach(m -> {
            CompoundTag mTag = new CompoundTag();
            mTag.putString("uuid", m.toString());
            mTag.putString("role", team.getRole(m).name());
            members.add(mTag);
        });
        tag.put("members", members);
        return tag;
    }

    private static void populateTeamsFromTag(MinecraftServer server, CompoundTag tag) {
        TEAMS.clear();
        for (Tag t : tag.getList("teams", Tag.TAG_COMPOUND)) {
            try {
                loadTeamEntry(server, (CompoundTag) t);
            } catch (Exception e) {
                CreRaces.LOGGER.warn("RaceTeamManager: failed to load team entry: {}", e.getMessage());
            }
        }
    }

    private static void loadTeamEntry(MinecraftServer server, CompoundTag obj) {
        if (!obj.contains("id") || !obj.contains("name") || !obj.contains("leader")) {
            CreRaces.LOGGER.warn("RaceTeamManager: skipping malformed team entry (missing id/name/leader)");
            return;
        }
        UUID id = UUID.fromString(obj.getString("id"));
        String name = obj.getString("name");
        UUID leader = UUID.fromString(obj.getString("leader"));
        boolean ff = obj.getBoolean("friendlyFire");
        RaceTeam team = new RaceTeam(id, name, leader);
        team.setFriendlyFire(ff);
        team.getMembers().clear();
        team.getMemberRoles().clear();
        for (Tag t : obj.getList("members", Tag.TAG_COMPOUND)) {
            try {
                CompoundTag mObj = (CompoundTag) t;
                UUID mUuid = UUID.fromString(mObj.getString("uuid"));
                Role role = Role.valueOf(mObj.getString("role"));
                team.getMembers().add(mUuid);
                team.getMemberRoles().put(mUuid, role);
            } catch (Exception e) {
                CreRaces.LOGGER.warn("RaceTeamManager: skipping malformed team member: {}", e.getMessage());
            }
        }
        TEAMS.put(id, team);

        // Re-apply teamId/teamName to members who are already online, e.g. after /reload.
        team.getMembers().forEach(memberId -> {
            ServerPlayer online = server.getPlayerList().getPlayer(Objects.requireNonNull(memberId));
            if (online != null) applyTeamToPlayer(online, team);
        });
    }
}
