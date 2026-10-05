package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.ClientAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.Supplier;

/** S2C: every quest definition, as JSON, so the client can mirror the server's QuestRegistry. */
public class SyncQuestsPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "sync_quests");

    private final Map<ResourceLocation, String> questData;

    public SyncQuestsPacket(Map<ResourceLocation, String> questData) {
        this.questData = questData;
    }

    public SyncQuestsPacket(FriendlyByteBuf buf) {
        this.questData = JsonDefinitions.read(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        JsonDefinitions.write(buf, questData);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientAccess.handleQuestSync(this.questData)));
    }
}
