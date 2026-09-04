package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.item.QuestScrollItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/** Works from anywhere, not just at the board - the scroll itself is the source of truth. */
public class AbandonQuestPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "abandon_quest");

    private final ResourceLocation questId;

    public AbandonQuestPacket(ResourceLocation questId) {
        this.questId = questId;
    }

    public AbandonQuestPacket(FriendlyByteBuf buf) {
        this.questId = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(questId);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> {
            if (!(ctx.get().getPlayer() instanceof ServerPlayer sp)) return;
            if (QuestScrollItem.removeActiveScroll(sp, questId)) {
                QuestBoardStateSyncPacket.resyncIfOpen(sp);
            }
        });
    }
}
