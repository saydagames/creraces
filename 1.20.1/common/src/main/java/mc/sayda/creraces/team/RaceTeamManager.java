package mc.sayda.creraces.team;

import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages race teams and party logic on the server.
 */
public class RaceTeamManager {
    private static final Map<UUID, RaceTeam> TEAMS = new ConcurrentHashMap<>();
    private static final Map<UUID, InviteData> PENDING_INVITES = new ConcurrentHashMap<>(); // Invitee -> InviteData

    public enum Role {
        MEMBER, OFFICER, LEADER
    }

    public static void broadcastUpdate(RaceTeam team, net.minecraft.server.MinecraftServer server) {
        List<mc.sayda.creraces.network.TeamUpdatePacket.MemberInfo> memberInfos = new ArrayList<>();
        for (UUID memberId : team.getMembers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(Objects.requireNonNull(memberId));
            String name = member != null ? member.getName().getString() : "Unknown";
            memberInfos.add(new mc.sayda.creraces.network.TeamUpdatePacket.MemberInfo(
                    memberId,
                    name,
                    team.getRole(memberId)));
        }

        mc.sayda.creraces.network.TeamUpdatePacket pkt = new mc.sayda.creraces.network.TeamUpdatePacket(memberInfos,
                team.isFriendlyFire(), null);

        for (UUID memberId : team.getMembers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(Objects.requireNonNull(memberId));
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
            int maxLen = 16; // NETWORK_TEAM_NAME_MAX_LEN default
            if (name.length() > maxLen) {
                this.name = name.substring(0, maxLen);
            } else {
                this.name = name;
            }
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

    public static boolean canHurt(net.minecraft.world.entity.LivingEntity victim,
            net.minecraft.world.entity.LivingEntity attacker) {
        if (victim == attacker) {
            return true; // Note: Changed from false to true to allow fruitful sacrifice to target the
                         // user themselves
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
        int maxLen = 16;
        String finalName = name.length() > maxLen ? name.substring(0, maxLen) : name;
        RaceTeam team = new RaceTeam(teamId, finalName, leader.getUUID());
        TEAMS.put(teamId, team);

        applyTeamToPlayer(leader, team);
        broadcastUpdate(team, leader.getServer());
        return team;
    }

    public static void joinTeam(ServerPlayer player, UUID teamId) {
        RaceTeam team = TEAMS.get(teamId);
        if (team != null) {
            team.getMembers().add(player.getUUID());
            team.setRole(player.getUUID(), Role.MEMBER);
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
                    team.getMemberRoles().remove(player.getUUID());
                    if (team.getMembers().isEmpty()) {
                        TEAMS.remove(teamId);
                    } else {
                        if (team.getLeader().equals(player.getUUID())) {
                            UUID next = team.getMembers().iterator().next();
                            team.setLeader(next);
                            team.setRole(next, Role.LEADER);
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
        RaceTeam team = TEAMS.get(teamId);
        if (team == null)
            return;

        Role role = team.getRole(inviter.getUUID());
        if (role != Role.LEADER && role != Role.OFFICER) {
            inviter.sendSystemMessage(Objects
                    .requireNonNull(net.minecraft.network.chat.Component.translatable("msg.creraces.team.no_perm")));
            return;
        }

        PENDING_INVITES.put(invitee.getUUID(), new InviteData(teamId, inviter.getServer().getTickCount()));
        String inviterName = inviter.getName().getString();
        String teamName = team.getName();
        invitee.sendSystemMessage(Objects.requireNonNull(net.minecraft.network.chat.Component.literal(
                Objects.requireNonNull(
                        String.format("\u00A76%s invited you to join \u00A7b%s\u00A76! Use /creraces team to accept.",
                                inviterName, teamName)))));
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
            } else {
                player.sendSystemMessage(Objects.requireNonNull(
                        net.minecraft.network.chat.Component.translatable("msg.creraces.team.no_perm")));
            }
        });
    }

    public static void promoteMember(ServerPlayer actor, UUID targetId, MinecraftServer server) {
        getPlayerTeam(actor).ifPresent(team -> {
            if (team.getRole(actor.getUUID()) != Role.LEADER) {
                actor.sendSystemMessage(Objects.requireNonNull(
                        net.minecraft.network.chat.Component.translatable("msg.creraces.team.no_perm")));
                return;
            }
            if (!team.getMembers().contains(targetId))
                return;

            Role current = team.getRole(targetId);
            if (current == Role.MEMBER) {
                team.setRole(targetId, Role.OFFICER);
            } else if (current == Role.OFFICER) {
                team.setRole(targetId, Role.LEADER);
                // Actor is demoted to officer by setRole(LEADER)
            }
            broadcastUpdate(team, server);
        });
    }

    public static void demoteMember(ServerPlayer actor, UUID targetId, MinecraftServer server) {
        getPlayerTeam(actor).ifPresent(team -> {
            if (team.getRole(actor.getUUID()) != Role.LEADER) {
                actor.sendSystemMessage(Objects.requireNonNull(
                        net.minecraft.network.chat.Component.translatable("msg.creraces.team.no_perm")));
                return;
            }
            if (!team.getMembers().contains(targetId))
                return;

            Role current = team.getRole(targetId);
            if (current == Role.OFFICER) {
                team.setRole(targetId, Role.MEMBER);
            } else if (current == Role.LEADER) {
                // Leader cannot demote themselves easily here, usually they leave or promote
                // another
                actor.sendSystemMessage(
                        Objects.requireNonNull(net.minecraft.network.chat.Component
                                .translatable("msg.creraces.team.cannot_demote_leader")));
                return;
            }
            broadcastUpdate(team, server);
        });
    }

    public static void tick(MinecraftServer server) {
        int currentTick = server.getTickCount();
        // Clear invites older than 5 minutes (6000 ticks)
        PENDING_INVITES.entrySet().removeIf(entry -> (currentTick - entry.getValue().timestamp) > 6000);
    }

    private static class InviteData {
        final UUID teamId;
        final int timestamp;

        InviteData(UUID teamId, int timestamp) {
            this.teamId = teamId;
            this.timestamp = timestamp;
        }
    }

    // ─── Persistence ─────────────────────────────────────────────────────────

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SAVE_FILE_NAME = "creraces_teams.json";

    public static void save(MinecraftServer server) {
        Path savePath = server
                .getWorldPath(Objects.requireNonNull(net.minecraft.world.level.storage.LevelResource.ROOT))
                .resolve(SAVE_FILE_NAME);
        JsonArray teamsArray = new JsonArray();
        TEAMS.values().forEach(team -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", team.getId().toString());
            obj.addProperty("name", team.getName());
            obj.addProperty("leader", team.getLeader().toString());
            obj.addProperty("friendlyFire", team.isFriendlyFire());
            JsonArray members = new JsonArray();
            team.getMembers().forEach(m -> {
                JsonObject mObj = new JsonObject();
                mObj.addProperty("uuid", m.toString());
                mObj.addProperty("role", team.getRole(m).name());
                members.add(mObj);
            });
            obj.add("members", members);
            teamsArray.add(obj);
        });
        try (Writer w = Files.newBufferedWriter(savePath)) {
            GSON.toJson(teamsArray, w);
            CreRaces.LOGGER.info("[CreRaces] Saved {} team(s) to {}", TEAMS.size(), savePath);
        } catch (IOException e) {
            CreRaces.LOGGER.error("[CreRaces] Failed to save teams: {}", e.getMessage());
        }
    }

    public static void load(MinecraftServer server) {
        Path savePath = server
                .getWorldPath(Objects.requireNonNull(net.minecraft.world.level.storage.LevelResource.ROOT))
                .resolve(SAVE_FILE_NAME);
        if (!Files.exists(savePath))
            return;
        try (Reader r = Files.newBufferedReader(savePath)) {
            JsonArray teamsArray = GSON.fromJson(r, JsonArray.class);
            if (teamsArray == null)
                return;
            TEAMS.clear();
            teamsArray.forEach(elem -> {
                JsonObject obj = elem.getAsJsonObject();
                UUID id = UUID.fromString(obj.get("id").getAsString());
                String name = obj.get("name").getAsString();
                UUID leader = UUID.fromString(obj.get("leader").getAsString());
                boolean ff = obj.has("friendlyFire") && obj.get("friendlyFire").getAsBoolean();
                RaceTeam team = new RaceTeam(id, name, leader);
                team.setFriendlyFire(ff);
                team.getMembers().clear();
                team.getMemberRoles().clear();
                obj.getAsJsonArray("members").forEach(mElem -> {
                    JsonObject mObj = mElem.getAsJsonObject();
                    UUID mUuid = UUID.fromString(mObj.get("uuid").getAsString());
                    Role role = Role.valueOf(mObj.get("role").getAsString());
                    team.getMembers().add(mUuid);
                    team.getMemberRoles().put(mUuid, role);
                });
                TEAMS.put(id, team);

                // Re-wire teamId/teamName into any already-online players (e.g. after /reload)
                team.getMembers().forEach(memberId -> {
                    ServerPlayer online = server.getPlayerList().getPlayer(Objects.requireNonNull(memberId));
                    if (online != null)
                        applyTeamToPlayer(online, team);
                });
            });
            CreRaces.LOGGER.info("[CreRaces] Loaded {} team(s) from {}", TEAMS.size(), savePath);
        } catch (Exception e) {
            CreRaces.LOGGER.error("[CreRaces] Failed to load teams: {}", e.getMessage());
        }
    }
}
