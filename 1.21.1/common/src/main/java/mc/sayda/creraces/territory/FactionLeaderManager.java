package mc.sayda.creraces.territory;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which player is the leader of each faction group.
 * Leader status is transient (not persisted); the first online member of a
 * faction group is elected leader; on disconnect, leadership passes to the next
 * online member. If no members are online, the next player to attempt a
 * territorial action is elected automatically.
 */
public class FactionLeaderManager {

    private static final Map<String, UUID> GROUP_LEADERS = new ConcurrentHashMap<>();

    public static @Nullable String getFactionGroup(Player player) {
        return DataUtils.getVariables(player)
                .map(IPlayerVariables::getRace)
                .map(raceId -> {
                    Race race = RaceRegistry.get(raceId);
                    return race != null ? race.factionGroup() : null;
                })
                .orElse(null);
    }

    public static boolean isLeader(Player player) {
        String group = getFactionGroup(player);
        if (group == null) return false;
        UUID leader = GROUP_LEADERS.get(group);
        return player.getUUID().equals(leader);
    }

    /**
     * Called on player join. Elects the player as faction leader if no leader
     * currently exists for their faction group. Only notifies them if other
     * faction members are online -- solo auto-election is silent.
     */
    public static void onPlayerJoin(ServerPlayer player) {
        String group = getFactionGroup(player);
        if (group == null) return;
        UUID before = GROUP_LEADERS.get(group);
        GROUP_LEADERS.computeIfAbsent(group, g -> player.getUUID());
        UUID after = GROUP_LEADERS.get(group);
        if (!player.getUUID().equals(after)) return;
        if (before != null) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        boolean othersOnline = server.getPlayerList().getPlayers().stream()
                .anyMatch(p -> !p.getUUID().equals(player.getUUID()) && group.equals(getFactionGroup(p)));
        if (othersOnline) {
            player.displayClientMessage(
                    Component.translatable("msg.creraces.faction.leader_assigned"), false);
        }
    }

    /**
     * Called on player disconnect. If the leaving player was leader, the next
     * online faction member is elected, or the leader slot is cleared if none remain.
     */
    public static void onPlayerLeave(ServerPlayer player, MinecraftServer server) {
        String group = getFactionGroup(player);
        if (group == null) return;
        UUID current = GROUP_LEADERS.get(group);
        if (!player.getUUID().equals(current)) return;
        passLeadership(player, group, server);
    }

    /**
     * Called while the player still has their old race. A leader whose new race takes them out of
     * the faction group (or who is reset to no race) hands leadership on as if they had left.
     */
    public static void onRaceChange(ServerPlayer player, @Nullable Race newRace) {
        String group = getFactionGroup(player);
        if (group == null || !player.getUUID().equals(GROUP_LEADERS.get(group))) return;
        if (newRace != null && group.equals(newRace.factionGroup())) return;
        passLeadership(player, group, player.server);
    }

    private static void passLeadership(ServerPlayer leader, String group, MinecraftServer server) {
        GROUP_LEADERS.remove(group);
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            if (online.getUUID().equals(leader.getUUID())) continue;
            if (group.equals(getFactionGroup(online))) {
                GROUP_LEADERS.put(group, online.getUUID());
                online.displayClientMessage(
                        Component.translatable("msg.creraces.faction.leader_assigned"), false);
                break;
            }
        }
    }

    /**
     * Ensures a leader exists for this player's faction group, electing them if
     * no leader is currently set. Call before any leader-gated action so that a
     * lone player isn't blocked from claiming when the server just started.
     */
    public static void electIfAbsent(ServerPlayer player) {
        String group = getFactionGroup(player);
        if (group == null) return;
        if (!GROUP_LEADERS.containsKey(group)) {
            onPlayerJoin(player);
        }
    }

    public static void clear() {
        GROUP_LEADERS.clear();
    }
}
