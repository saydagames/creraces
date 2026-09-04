package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.block.QuestBoardBlock;
import mc.sayda.creraces.block.entity.QuestBoardBlockEntity;
import mc.sayda.creraces.engine.WorldState;
import mc.sayda.creraces.item.QuestScrollItem;
import mc.sayda.creraces.quest.Quest;
import mc.sayda.creraces.quest.QuestRegistry;
import mc.sayda.creraces.quest.QuestSessionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class TakeQuestPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "take_quest");

    private final BlockPos boardPos;
    private final ResourceLocation questId;

    public TakeQuestPacket(BlockPos boardPos, ResourceLocation questId) {
        this.boardPos = boardPos;
        this.questId = questId;
    }

    public TakeQuestPacket(FriendlyByteBuf buf) {
        this.boardPos = buf.readBlockPos();
        this.questId = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(boardPos);
        buf.writeResourceLocation(questId);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> {
            if (!(ctx.get().getPlayer() instanceof ServerPlayer sp)) return;
            if (!(sp.level().getBlockEntity(boardPos) instanceof QuestBoardBlockEntity be)) return;
            if (!QuestBoardBlock.isIntact(sp.level(), sp.level().getBlockState(boardPos), boardPos)) return;
            if (!be.getPendingOfferedIds().contains(questId)) return;
            if (QuestScrollItem.hasActiveScroll(sp, questId)) return;
            if (QuestSessionRegistry.isOnCooldown(sp.getUUID(), questId, WorldState.currentDay(sp.level()))) return;

            Quest quest = QuestRegistry.get(questId);
            if (quest == null) return;

            sp.getInventory().placeItemBackInInventory(QuestScrollItem.create(quest, sp));
            QuestBoardStateSyncPacket.resyncIfOpen(sp);
        });
    }
}
