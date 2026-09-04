package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Syncs the entire QuestRegistry (in JSON form) from server to client.
 */
public class SyncQuestsPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "sync_quests");
    private final Map<ResourceLocation, String> questData;

    public SyncQuestsPacket(Map<ResourceLocation, String> questData) {
        this.questData = questData;
    }

    public SyncQuestsPacket(FriendlyByteBuf buf) {
        this.questData = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            this.questData.put(buf.readResourceLocation(), buf.readUtf(262144));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(questData.size());
        questData.forEach((id, json) -> {
            buf.writeResourceLocation(id);
            buf.writeUtf(json, 262144);
        });
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                mc.sayda.creraces.client.ClientAccess.handleQuestSync(this.questData);
            });
        });
    }
}
