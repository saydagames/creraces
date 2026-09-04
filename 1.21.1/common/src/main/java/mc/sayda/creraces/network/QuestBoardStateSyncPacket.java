package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.block.entity.QuestBoardBlockEntity;
import mc.sayda.creraces.world.inventory.QuestBoardMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/**
 * S2C: pushed right after a Take/Abandon so an open Quest Board GUI updates its grayed-out
 * slots immediately, without needing a full menu reopen.
 */
public class QuestBoardStateSyncPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "quest_board_state_sync");

    private final boolean[] taken;
    private final boolean[] locked;

    public QuestBoardStateSyncPacket(boolean[] taken, boolean[] locked) {
        this.taken = taken;
        this.locked = locked;
    }

    public QuestBoardStateSyncPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.taken = new boolean[size];
        for (int i = 0; i < size; i++) taken[i] = buf.readBoolean();
        this.locked = new boolean[size];
        for (int i = 0; i < size; i++) locked[i] = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(taken.length);
        for (boolean b : taken) buf.writeBoolean(b);
        for (boolean b : locked) buf.writeBoolean(b);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                if (net.minecraft.client.Minecraft.getInstance().player != null
                        && net.minecraft.client.Minecraft.getInstance().player.containerMenu instanceof QuestBoardMenu menu) {
                    menu.applyTakenSync(this.taken);
                    menu.applyLockedSync(this.locked);
                }
            });
        });
    }

    /** If the player currently has a Quest Board GUI open, resyncs its taken[]/locked[] state. */
    public static void resyncIfOpen(ServerPlayer player) {
        if (player.containerMenu instanceof QuestBoardMenu menu) {
            var ids = new java.util.ArrayList<ResourceLocation>();
            for (int i = 0; i < menu.getSlotCount(); i++) ids.add(menu.getQuestId(i));
            boolean[] taken = QuestBoardBlockEntity.computeTaken(player, ids);
            boolean[] locked = QuestBoardBlockEntity.computeLocked(player, ids);
            BoundaryHandler.sendQuestBoardSync(player, new QuestBoardStateSyncPacket(taken, locked));
        }
    }
}
