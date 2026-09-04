package mc.sayda.creraces.block.entity;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import mc.sayda.creraces.engine.WorldState;
import mc.sayda.creraces.item.QuestScrollItem;
import mc.sayda.creraces.quest.Quest;
import mc.sayda.creraces.quest.QuestRegistry;
import mc.sayda.creraces.quest.QuestSessionRegistry;
import mc.sayda.creraces.registry.ModBlocks;
import mc.sayda.creraces.world.inventory.QuestBoardMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class QuestBoardBlockEntity extends BlockEntity implements ExtendedMenuProvider {
    public static final int TIERS = 5;
    public static final int PER_TIER = 2;
    public static final int SLOT_COUNT = TIERS * PER_TIER;

    /** Per-open snapshot from computeOfferedIds(), so createMenu and saveExtraData see the same list. */
    private List<ResourceLocation> pendingOfferedIds = List.of();

    public QuestBoardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.QUEST_BOARD_ENTITY.get(), pos, state);
        //mc.sayda.creraces.CreRaces.LOGGER.info("[CreRaces] Quest Board block entity loaded at {}", pos);
    }

    /**
     * Picks PER_TIER quests per tier for {@code player}. Active quests stay pinned to their
     * slot; cooldown quests are excluded unless there aren't enough alternatives. Locked in
     * per (player, in-world day) via QuestSessionRegistry, so repeat opens reuse the same
     * layout until the next day rolls a fresh one.
     */
    public static List<ResourceLocation> computeOfferedIds(Player player) {
        long currentDay = WorldState.currentDay(player.level());
        List<ResourceLocation> cached = QuestSessionRegistry.getBoardSlots(player.getUUID(), currentDay);
        if (cached != null) return cached;

        List<ResourceLocation> result = new ArrayList<>();

        for (int tier = 1; tier <= TIERS; tier++) {
            final int t = tier;
            List<ResourceLocation> pool = QuestRegistry.getAll().stream()
                    .filter(q -> q.tier() == t)
                    .map(Quest::id)
                    .sorted(Comparator.comparing(ResourceLocation::toString))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

            List<ResourceLocation> chosen = new ArrayList<>();
            for (ResourceLocation id : pool) {
                if (chosen.size() >= PER_TIER) break;
                if (QuestScrollItem.hasActiveScroll(player, id)) chosen.add(id);
            }

            List<ResourceLocation> available = new ArrayList<>(pool);
            available.removeAll(chosen);
            available.removeIf(id -> QuestSessionRegistry.isOnCooldown(player.getUUID(), id, currentDay));
            Collections.shuffle(available);
            for (ResourceLocation id : available) {
                if (chosen.size() >= PER_TIER) break;
                chosen.add(id);
            }

            // Cooldowns emptied the pool below what's needed: fall back to filling with
            // cooldown quests too rather than showing fewer than PER_TIER slots.
            if (chosen.size() < PER_TIER) {
                List<ResourceLocation> fallback = new ArrayList<>(pool);
                fallback.removeAll(chosen);
                Collections.shuffle(fallback);
                for (ResourceLocation id : fallback) {
                    if (chosen.size() >= PER_TIER) break;
                    chosen.add(id);
                }
            }

            result.addAll(chosen);
        }
        QuestSessionRegistry.setBoardSlots(player.getUUID(), currentDay, result);
        return result;
    }

    public static boolean[] computeTaken(Player player, List<ResourceLocation> ids) {
        boolean[] taken = new boolean[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            taken[i] = QuestScrollItem.hasActiveScroll(player, ids.get(i));
        }
        return taken;
    }

    /** True per slot if that quest is on this player's abandon/expiry cooldown - takeable again once it lifts, matching the check in TakeQuestPacket. */
    public static boolean[] computeLocked(Player player, List<ResourceLocation> ids) {
        long currentDay = WorldState.currentDay(player.level());
        boolean[] locked = new boolean[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            locked[i] = QuestSessionRegistry.isOnCooldown(player.getUUID(), ids.get(i), currentDay);
        }
        return locked;
    }

    /** Called by QuestBoardBlock.use() before opening the menu, so createMenu/saveExtraData agree. */
    public void prepareOfferedIds(Player player) {
        this.pendingOfferedIds = computeOfferedIds(player);
    }

    public List<ResourceLocation> getPendingOfferedIds() {
        return pendingOfferedIds;
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
        buf.writeVarInt(pendingOfferedIds.size());
        for (ResourceLocation id : pendingOfferedIds) buf.writeResourceLocation(id);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.creraces.quest_board");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, @Nonnull Inventory playerInv, @Nonnull Player player) {
        List<ResourceLocation> ids = pendingOfferedIds.isEmpty() ? computeOfferedIds(player) : pendingOfferedIds;
        boolean[] taken = computeTaken(player, ids);
        boolean[] locked = computeLocked(player, ids);
        return new QuestBoardMenu(syncId, playerInv, worldPosition, ids, taken, locked);
    }
}
