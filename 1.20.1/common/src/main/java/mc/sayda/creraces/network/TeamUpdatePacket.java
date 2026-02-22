package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.function.Supplier;

/**
 * Packet sent from server to client to update team member info.
 */
public class TeamUpdatePacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "team_update");

    private final List<MemberInfo> members;
    private final boolean friendlyFire;
    private final String invitedTeamName;

    public static record MemberInfo(UUID uuid, String name, boolean isLeader) {
    }

    public TeamUpdatePacket(List<MemberInfo> members, boolean friendlyFire, String invitedTeamName) {
        this.members = members;
        this.friendlyFire = friendlyFire;
        this.invitedTeamName = invitedTeamName != null ? invitedTeamName : "";
    }

    public TeamUpdatePacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.members = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.members.add(new MemberInfo(buf.readUUID(), buf.readUtf(), buf.readBoolean()));
        }
        this.friendlyFire = buf.readBoolean();
        this.invitedTeamName = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(members.size());
        for (MemberInfo member : members) {
            buf.writeUUID(member.uuid());
            buf.writeUtf(member.name());
            buf.writeBoolean(member.isLeader());
        }
        buf.writeBoolean(friendlyFire);
        buf.writeUtf(invitedTeamName);
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                mc.sayda.creraces.client.screen.RaceTeamScreen.update(members, friendlyFire, invitedTeamName);
            });
        });
    }
}
