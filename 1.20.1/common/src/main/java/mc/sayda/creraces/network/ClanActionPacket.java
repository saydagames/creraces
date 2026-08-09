package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.territory.ClanData;
import mc.sayda.creraces.territory.DiplomacyStatus;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/**
 * C2S: client requests a clan (diplomacy) operation.
 */
@SuppressWarnings("null")
public class ClanActionPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "clan_action");

    public enum Action { VIEW, SET_RELATION }

    private final Action action;
    private ResourceLocation targetRace;
    private DiplomacyStatus status;

    public ClanActionPacket(Action action) { this.action = action; }

    public ClanActionPacket(Action action, ResourceLocation targetRace, DiplomacyStatus status) {
        this.action = action;
        this.targetRace = targetRace;
        this.status = status;
    }

    public ClanActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        if (buf.readBoolean()) this.targetRace = buf.readResourceLocation();
        if (buf.readBoolean()) this.status = buf.readEnum(DiplomacyStatus.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeBoolean(targetRace != null);
        if (targetRace != null) buf.writeResourceLocation(targetRace);
        buf.writeBoolean(status != null);
        if (status != null) buf.writeEnum(status);
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            ResourceLocation myRace = DataUtils.getVariables(player)
                    .map(IPlayerVariables::getRace)
                    .orElse(null);
            if (myRace == null) return;

            TerritoryManager tm = TerritoryManager.get();

            switch (action) {
                case VIEW -> {
                    // getClanOrEmpty returns a transient object if no entry exists,
                    // preventing the VIEW action from permanently inserting empty ClanData.
                    ClanData clan = tm.getClanOrEmpty(myRace);
                    BoundaryHandler.sendClanUpdate(player, ClanUpdatePacket.from(clan));
                    BoundaryHandler.sendOpenClanManage(player);
                }
                case SET_RELATION -> {
                    if (targetRace == null || status == null) return;
                    if (mc.sayda.creraces.race.RaceRegistry.get(targetRace) == null) return;
                    if (!mc.sayda.creraces.territory.FactionLeaderManager.isLeader(player)) return;
                    tm.setDiplomacy(myRace, targetRace, status);
                    ClanData clan = tm.getClanOrEmpty(myRace);
                    BoundaryHandler.sendClanUpdate(player, ClanUpdatePacket.from(clan));
                }
            }
        });
    }
}
