package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.function.Supplier;
import java.util.Objects;

/**
 * Packet sent from server to client to update team member info.
 */
public class TeamUpdatePacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "team_update");

    private final List<MemberInfo> members;
    private final boolean friendlyFire;
    private final String invitedTeamName;

    public static class MemberInfo {
        private final UUID uuid;
        private final String name;
        private final mc.sayda.creraces.team.RaceTeamManager.Role role;

        public MemberInfo(UUID uuid, String name, mc.sayda.creraces.team.RaceTeamManager.Role role) {
            this.uuid = uuid;
            this.name = name;
            this.role = role;
        }

        public UUID uuid() {
            return uuid;
        }

        public String name() {
            return name;
        }

        public mc.sayda.creraces.team.RaceTeamManager.Role role() {
            return role;
        }
    }

    public TeamUpdatePacket(List<MemberInfo> members, boolean friendlyFire, String invitedTeamName) {
        this.members = members;
        this.friendlyFire = friendlyFire;
        this.invitedTeamName = invitedTeamName != null ? invitedTeamName : "";
    }

    public TeamUpdatePacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.members = new ArrayList<>(size);
        int teamMax = mc.sayda.creraces.config.CreRacesConfig.NETWORK_TEAM_NAME_MAX_LEN.get();
        for (int i = 0; i < size; i++) {
            this.members.add(new MemberInfo(buf.readUUID(), buf.readUtf(teamMax),
                    buf.readEnum(mc.sayda.creraces.team.RaceTeamManager.Role.class)));
        }
        this.friendlyFire = buf.readBoolean();
        this.invitedTeamName = buf.readUtf(teamMax);
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

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                mc.sayda.creraces.client.screen.RaceTeamScreen.update(members, friendlyFire, invitedTeamName);
            });
        });
    }
}
