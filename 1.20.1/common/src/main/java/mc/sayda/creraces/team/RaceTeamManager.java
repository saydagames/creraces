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
@SuppressWarnings("null")
public class RaceTeamManager {
    private static final Map<UUID, RaceTeam> TEAMS = new ConcurrentHashMap<>();
    private static final Map<UUID, InviteData> PENDING_INVITES = new ConcurrentHashMap<>(); // Invitee -> InviteData
    private static final Set<UUID> PENDING_REMOVALS = ConcurrentHashMap.newKeySet(); // Players kicked while offline

    public enum Role {
        MEMBER, OFFICER, LEADER
    }

    public static void broadcastUpdate(RaceTeam team, net.minecraft.server.MinecraftServer server) {
        List<mc.sayda.creraces.network.TeamUpdatePacket.MemberInfo> memberInfos = new ArrayList<>();
        for (UUID memberId : team.getMembers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(Objects.requireNonNull(memberId));
            String name;
            if (member != null) {
                name = member.getName().getString();
            } else {
                name = server.getProfileCache()
                        .get(memberId)
                        .map(com.mojang.authlib.GameProfile::getName)
                        .orElse("Unknown");
            }
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
            int maxLen = mc.sayda.creraces.config.CreRacesConfig.NETWORK_TEAM_NAME_MAX_LEN.get();
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
            return true;
        }

        // Use root owner resolution to handle players, tamed animals, and servants
        Player vOwner = mc.sayda.creraces.util.CombatUtils.getRootOwner(victim);
        Player aOwner = mc.sayda.creraces.util.CombatUtils.getRootOwner(attacker);

        if (vOwner != null && aOwner != null) {
            // Same instance -> Allied (handled by Minecraft usually, but good to be explicit for servants)
            if (vOwner.getUUID().equals(aOwner.getUUID())) {
                return false;
            }

            IPlayerVariables vVars = DataUtils.getVariables(vOwner).orElse(null);
            IPlayerVariables aVars = DataUtils.getVariables(aOwner).orElse(null);

            if (vVars != null && aVars != null) {
                UUID vTeam = vVars.getTeamId();
                UUID aTeam = aVars.getTeamId();

                if (vTeam != null && vTeam.equals(aTeam)) {
                    // Shared Team ID: Follow Friendly Fire toggle
                    RaceTeam team = TEAMS.get(vTeam);
                    if (team != null && team.isFriendlyFire()) {
                        return true;
                    }
                    return false;
                } else {
                    // Different teams or missing team: Absolute Authority (Treat as enemies)
                    return true;
                }
            }

            // Fallback for cases without variables (should not happen for players)
            if (vOwner.isAlliedTo(aOwner)) {
                return false;
            }
        }

        // Final fallback for entities with owners that aren't players (if any)
        if (victim instanceof net.minecraft.world.entity.OwnableEntity ownable && ownable.getOwner() == attacker) {
            return false;
        }

        return true;
    }

    public static RaceTeam createTeam(ServerPlayer leader, String name) {
        // Force leave current team if any
        leaveTeam(leader);

        UUID teamId = UUID.randomUUID();
        int maxLen = mc.sayda.creraces.config.CreRacesConfig.NETWORK_TEAM_NAME_MAX_LEN.get();
        String finalName = name.length() > maxLen ? name.substring(0, maxLen) : name;
        RaceTeam team = new RaceTeam(teamId, finalName, leader.getUUID());
        TEAMS.put(teamId, team);

        applyTeamToPlayer(leader, team);
        broadcastUpdate(team, leader.getServer());
        save(leader.getServer());
        leader.sendSystemMessage(
                net.minecraft.network.chat.Component.translatable("msg.creraces.team.created", finalName));
        return team;
    }

    public static void joinTeam(ServerPlayer player, UUID teamId) {
        RaceTeam team = TEAMS.get(teamId);
        if (team != null) {
            if (team.getMembers().size() >= mc.sayda.creraces.config.CreRacesConfig.TEAM_MAX_SIZE.get()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.creraces.team.full"));
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
                    net.minecraft.network.chat.Component.translatable("msg.creraces.team.joined", team.getName()));
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
                                            .orElse(null)); // Should not happen as members is not empty
                            
                            if (next != null) {
                                team.setLeader(next);
                                team.setRole(next, Role.LEADER);
                            }
                        }
                        broadcastUpdate(team, player.getServer());
                    }
                    save(player.getServer());
                }
                vars.setTeamId(null);
                vars.setTeamName("");
                mc.sayda.creraces.network.BoundaryHandler.sendTeamUpdate(player,
                        new mc.sayda.creraces.network.TeamUpdatePacket(Collections.emptyList(), false, null));
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.creraces.team.left"));
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
        invitee.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.creraces.team.invite_received",
                inviter.getName().getString(), team.getName()));
        inviter.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.creraces.team.invited",
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
            String targetName = server.getPlayerList().getPlayer(targetId) != null
                    ? server.getPlayerList().getPlayer(targetId).getName().getString()
                    : "Unknown";
            Role newRole = team.getRole(targetId);
            actor.sendSystemMessage(
                    net.minecraft.network.chat.Component.translatable("msg.creraces.team.promoted", targetName,
                            net.minecraft.network.chat.Component
                                    .translatable("gui.creraces.team.role." + newRole.name().toLowerCase())));
            broadcastUpdate(team, server);
            save(server);
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
            String targetName = server.getPlayerList().getPlayer(targetId) != null
                    ? server.getPlayerList().getPlayer(targetId).getName().getString()
                    : "Unknown";
            Role newRole = team.getRole(targetId);
            actor.sendSystemMessage(
                    net.minecraft.network.chat.Component.translatable("msg.creraces.team.demoted", targetName,
                            net.minecraft.network.chat.Component
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
                actor.sendSystemMessage(Objects.requireNonNull(
                        net.minecraft.network.chat.Component.translatable("msg.creraces.team.no_perm")));
                return;
            }

            if (actorRole == Role.OFFICER && targetRole != Role.MEMBER) {
                actor.sendSystemMessage(Objects.requireNonNull(
                        net.minecraft.network.chat.Component.translatable("msg.creraces.team.no_perm")));
                return;
            }

            if (actor.getUUID().equals(targetId)) {
                return; // Use leave instead
            }

            team.getMembers().remove(targetId);
            team.getMemberRoles().remove(targetId);

            ServerPlayer target = server.getPlayerList().getPlayer(targetId);
            if (target != null) {
                DataUtils.getVariables(target).ifPresent(vars -> {
                    vars.setTeamId(null);
                    vars.setTeamName("");
                    mc.sayda.creraces.network.BoundaryHandler.sendTeamUpdate(target,
                            new mc.sayda.creraces.network.TeamUpdatePacket(Collections.emptyList(), false, null));
                });
                target.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.creraces.team.kicked"));
            } else {
                // target is offline, mark for removal on next join
                PENDING_REMOVALS.add(targetId);
            }

            String targetName = target != null ? target.getName().getString() : targetId.toString();
            actor.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.creraces.team.actor_kicked",
                    targetName));
            broadcastUpdate(team, server);
            save(server);
        });
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 == 0) {
            int currentTick = server.getTickCount();
            PENDING_INVITES.entrySet().removeIf(entry -> currentTick - entry.getValue().timestamp > 1200);
        }
    }

    public static void handlePlayerJoin(ServerPlayer player) {
        if (PENDING_REMOVALS.remove(player.getUUID())) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                vars.setTeamId(null);
                vars.setTeamName("");
                mc.sayda.creraces.network.BoundaryHandler.sendTeamUpdate(player,
                        new mc.sayda.creraces.network.TeamUpdatePacket(Collections.emptyList(), false, null));
                CreRaces.LOGGER.info("Cleared stale team data for kicked player: {}", player.getName().getString());
            });
        }
        
        // Broadcast arrival to team
        getPlayerTeam(player).ifPresent(team -> {
            broadcastUpdate(team, player.getServer());
        });
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
            CreRaces.LOGGER.info("Saved {} team(s) to {}", TEAMS.size(), savePath);
        } catch (IOException e) {
            CreRaces.LOGGER.error("Failed to save teams: {}", e.getMessage());
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
                try {
                    JsonObject obj = elem.getAsJsonObject();
                    if (!obj.has("id") || !obj.has("name") || !obj.has("leader")) {
                        mc.sayda.creraces.CreRaces.LOGGER.warn("RaceTeamManager: skipping malformed team entry (missing id/name/leader)");
                        return;
                    }
                    UUID id = UUID.fromString(obj.get("id").getAsString());
                    String name = obj.get("name").getAsString();
                    UUID leader = UUID.fromString(obj.get("leader").getAsString());
                    boolean ff = obj.has("friendlyFire") && obj.get("friendlyFire").getAsBoolean();
                    RaceTeam team = new RaceTeam(id, name, leader);
                    team.setFriendlyFire(ff);
                    team.getMembers().clear();
                    team.getMemberRoles().clear();
                    if (obj.has("members") && obj.get("members").isJsonArray()) {
                        obj.getAsJsonArray("members").forEach(mElem -> {
                            try {
                                JsonObject mObj = mElem.getAsJsonObject();
                                UUID mUuid = UUID.fromString(mObj.get("uuid").getAsString());
                                Role role = Role.valueOf(mObj.get("role").getAsString());
                                team.getMembers().add(mUuid);
                                team.getMemberRoles().put(mUuid, role);
                            } catch (Exception e) {
                                mc.sayda.creraces.CreRaces.LOGGER.warn("RaceTeamManager: skipping malformed team member: {}", e.getMessage());
                            }
                        });
                    }
                    TEAMS.put(id, team);

                    // Re-wire teamId/teamName into any already-online players (e.g. after /reload)
                    team.getMembers().forEach(memberId -> {
                        ServerPlayer online = server.getPlayerList().getPlayer(Objects.requireNonNull(memberId));
                        if (online != null)
                            applyTeamToPlayer(online, team);
                    });
                } catch (Exception e) {
                    mc.sayda.creraces.CreRaces.LOGGER.warn("RaceTeamManager: failed to load team entry: {}", e.getMessage());
                }
            });
            CreRaces.LOGGER.info("Loaded {} team(s) from {}", TEAMS.size(), savePath);
        } catch (Exception e) {
            CreRaces.LOGGER.error("Failed to load teams: {}", e.getMessage());
        }
    }
}
