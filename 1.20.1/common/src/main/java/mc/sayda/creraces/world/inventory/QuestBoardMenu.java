package mc.sayda.creraces.world.inventory;

import mc.sayda.creraces.quest.Quest;
import mc.sayda.creraces.quest.QuestRegistry;
import mc.sayda.creraces.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A slot-less picker UI for the Quest Board's 10 offered quests. "Taken" state is derived
 * per-viewing-player (see QuestBoardBlockEntity.computeTaken) and pushed fresh whenever it
 * changes via QuestBoardStateSyncPacket, rather than requiring a full menu reopen.
 */
public class QuestBoardMenu extends AbstractContainerMenu {

    private final BlockPos boardPos;
    private final List<ResourceLocation> questIds;
    private final boolean[] taken;
    private final boolean[] locked;

    public QuestBoardMenu(int syncId, Inventory playerInv, BlockPos boardPos, List<ResourceLocation> questIds,
            boolean[] taken, boolean[] locked) {
        super(ModMenuTypes.QUEST_BOARD.get(), syncId);
        this.boardPos = boardPos;
        this.questIds = questIds;
        this.taken = taken;
        this.locked = locked;
    }

    public QuestBoardMenu(int syncId, Inventory playerInv, FriendlyByteBuf buf) {
        this(syncId, playerInv, buf.readBlockPos(), readQuestIds(buf), new boolean[QuestBoardMenuConstants.SLOT_COUNT],
                new boolean[QuestBoardMenuConstants.SLOT_COUNT]);
    }

    private static List<ResourceLocation> readQuestIds(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ResourceLocation> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) ids.add(buf.readResourceLocation());
        return ids;
    }

    public BlockPos getBoardPos() {
        return boardPos;
    }

    public ResourceLocation getQuestId(int slot) {
        return questIds.get(slot);
    }

    public int getSlotCount() {
        return questIds.size();
    }

    public boolean isTaken(int slot) {
        return slot < taken.length && taken[slot];
    }

    /** True if this slot's quest is on the player's abandon/expiry cooldown - takeable again once it lifts. */
    public boolean isLocked(int slot) {
        return slot < locked.length && locked[slot];
    }

    public int getTier(int slot) {
        Quest quest = QuestRegistry.get(getQuestId(slot));
        return quest != null ? quest.tier() : 0;
    }

    public void applyTakenSync(boolean[] newTaken) {
        System.arraycopy(newTaken, 0, this.taken, 0, Math.min(newTaken.length, this.taken.length));
    }

    public void applyLockedSync(boolean[] newLocked) {
        System.arraycopy(newLocked, 0, this.locked, 0, Math.min(newLocked.length, this.locked.length));
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(boardPos)) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static final class QuestBoardMenuConstants {
        static final int SLOT_COUNT = mc.sayda.creraces.block.entity.QuestBoardBlockEntity.SLOT_COUNT;
    }
}
