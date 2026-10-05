package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.screen.RaceTeamScreen;
import mc.sayda.creraces.team.RaceTeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** S2C: the player's team roster, friendly-fire setting and pending invite. */
public class TeamUpdatePacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "team_update");

    // Fixed protocol limits rather than the config, so a client with stricter settings than the
    // server still decodes the packet.
    private static final int MAX_MEMBERS = 1024;
    private static final int NAME_MAX_LEN = 256;

    private final List<MemberInfo> members;
    private final boolean friendlyFire;
    private final String invitedTeamName;

    public record MemberInfo(UUID uuid, String name, RaceTeamManager.Role role) {
    }

    public TeamUpdatePacket(List<MemberInfo> members, boolean friendlyFire, String invitedTeamName) {
        this.members = members;
        this.friendlyFire = friendlyFire;
        this.invitedTeamName = invitedTeamName != null ? invitedTeamName : "";
    }

    public TeamUpdatePacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        if (size < 0 || size > MAX_MEMBERS) throw new IllegalStateException("Oversized team packet: " + size);
        this.members = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.members.add(new MemberInfo(buf.readUUID(), buf.readUtf(NAME_MAX_LEN),
                    buf.readEnum(RaceTeamManager.Role.class)));
        }
        this.friendlyFire = buf.readBoolean();
        this.invitedTeamName = buf.readUtf(NAME_MAX_LEN);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(members.size());
        for (MemberInfo member : members) {
            buf.writeUUID(member.uuid());
            buf.writeUtf(member.name());
            buf.writeEnum(Objects.requireNonNull(member.role()));
        }
        buf.writeBoolean(friendlyFire);
        buf.writeUtf(invitedTeamName);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> EnvExecutor.runInEnv(Env.CLIENT,
                () -> () -> RaceTeamScreen.update(members, friendlyFire, invitedTeamName)));
    }
}
