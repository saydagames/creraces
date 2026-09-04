package mc.sayda.creraces.quest;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session-scoped (in-memory only, not persisted - cleared on server restart and on player
 * disconnect) record of per-player quest state:
 * <ul>
 *   <li>ABANDONED - has this instance been abandoned or expired? The single source of truth
 *       an ACTIVE scroll checks against: abandoning doesn't hunt down one specific stack, it
 *       marks the pair here, and every ACTIVE scroll matching it (wherever it's found in the
 *       owner's inventory, not just the copy a packet happened to see) self-removes on the
 *       next check.</li>
 *   <li>COOLDOWN_UNTIL_DAY - the in-world day this quest becomes takeable again by this
 *       player after being abandoned/expired (equal in length to the quest's own duration).</li>
 *   <li>BOARD_DAY / BOARD_SLOTS - the Quest Board's per-player offered-quest layout, locked in
 *       once per in-world day rather than reshuffled on every board open.</li>
 * </ul>
 */
public final class QuestSessionRegistry {
    private static final Map<UUID, Set<ResourceLocation>> ABANDONED = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<ResourceLocation, Long>> COOLDOWN_UNTIL_DAY = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> BOARD_DAY = new ConcurrentHashMap<>();
    private static final Map<UUID, List<ResourceLocation>> BOARD_SLOTS = new ConcurrentHashMap<>();

    private QuestSessionRegistry() {
    }

    /**
     * This player's locked-in Quest Board layout for {@code currentDay}, or null if it hasn't
     * been rolled yet (first visit, or the in-world day has advanced since it last was). Once
     * set, a layout stays fixed for the rest of that day regardless of how many times the
     * board is reopened or how many of its quests get taken/abandoned - see
     * QuestBoardBlockEntity.computeOfferedIds.
     */
    public static List<ResourceLocation> getBoardSlots(UUID player, long currentDay) {
        Long day = BOARD_DAY.get(player);
        if (day == null || day != currentDay) return null;
        return BOARD_SLOTS.get(player);
    }

    public static void setBoardSlots(UUID player, long currentDay, List<ResourceLocation> slots) {
        BOARD_DAY.put(player, currentDay);
        BOARD_SLOTS.put(player, List.copyOf(slots));
    }

    public static void markAbandoned(UUID player, ResourceLocation questId) {
        ABANDONED.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).add(questId);
    }

    public static boolean isAbandoned(UUID player, ResourceLocation questId) {
        Set<ResourceLocation> set = ABANDONED.get(player);
        return set != null && set.contains(questId);
    }

    /** Called when a fresh scroll is issued for this (player, quest) pair, so a re-taken quest isn't immediately self-removed by a stale flag from a previous instance. */
    public static void clearAbandoned(UUID player, ResourceLocation questId) {
        Set<ResourceLocation> set = ABANDONED.get(player);
        if (set != null) set.remove(questId);
    }

    /** Blocks this (player, quest) slot from being re-taken until the given in-world day. */
    public static void startCooldown(UUID player, ResourceLocation questId, long untilDay) {
        COOLDOWN_UNTIL_DAY.computeIfAbsent(player, k -> new ConcurrentHashMap<>()).put(questId, untilDay);
    }

    public static boolean isOnCooldown(UUID player, ResourceLocation questId, long currentDay) {
        Map<ResourceLocation, Long> map = COOLDOWN_UNTIL_DAY.get(player);
        if (map == null) return false;
        Long until = map.get(questId);
        return until != null && currentDay < until;
    }

    /** Called on player disconnect to avoid unbounded growth across a long server session. */
    public static void clearPlayer(UUID player) {
        ABANDONED.remove(player);
        COOLDOWN_UNTIL_DAY.remove(player);
        BOARD_DAY.remove(player);
        BOARD_SLOTS.remove(player);
    }
}
