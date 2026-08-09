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

    private static final Map<String, UUID> groupLeaders = new ConcurrentHashMap<>();

    public static @Nullable String getFactionGroup(Player player) {
        return DataUtils.getVariables(player)
                .map(IPlayerVariables::getRace)
                .map(raceId -> {
                    Race race = RaceRegistry.get(raceId);
                    return race != null ? race.effectiveFactionGroup() : null;
                })
                .orElse(null);
    }

    public static boolean isLeader(Player player) {
        String group = getFactionGroup(player);
        if (group == null) return false;
        UUID leader = groupLeaders.get(group);
        return player.getUUID().equals(leader);
    }

    public static @Nullable UUID getLeader(String group) {
        return groupLeaders.get(group);
    }

    /**
     * Called on player join. Elects the player as faction leader if no leader
     * currently exists for their faction group. Only notifies them if other
     * faction members are online -- solo auto-election is silent.
     */
    public static void onPlayerJoin(ServerPlayer player) {
        String group = getFactionGroup(player);
        if (group == null) return;
        UUID before = groupLeaders.get(group);
        groupLeaders.computeIfAbsent(group, g -> player.getUUID());
        UUID after = groupLeaders.get(group);
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
        UUID current = groupLeaders.get(group);
        if (!player.getUUID().equals(current)) return;

        groupLeaders.remove(group);
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            if (online.getUUID().equals(player.getUUID())) continue;
            if (group.equals(getFactionGroup(online))) {
                groupLeaders.put(group, online.getUUID());
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
        if (!groupLeaders.containsKey(group)) {
            onPlayerJoin(player);
        }
    }

    public static void clear() {
        groupLeaders.clear();
    }
}
