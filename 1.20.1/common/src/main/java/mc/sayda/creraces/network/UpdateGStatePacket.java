package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import io.netty.buffer.Unpooled;

import java.util.function.Supplier;

public class UpdateGStatePacket {
    public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation(
            "creraces", "update_gstate");

    private final int gState;

    public UpdateGStatePacket(int gState) {
        this.gState = gState;
    }

    public UpdateGStatePacket(FriendlyByteBuf buf) {
        this.gState = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(gState);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(gState);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
                DataUtils.getVariables(player).ifPresent(vars -> {
                    vars.setGState(this.gState);
                    if (vars.hasChosenRace()) {
                        mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
                        if (race != null) {
                            mc.sayda.creraces.race.CosmeticIncidents.applyGStateCosmetics(player, race, vars);
                        }
                    } else {
                        mc.sayda.creraces.race.CosmeticIncidents.applyGStateAddons(player);
                    }
                    BoundaryHandler.resyncVariables(player, player);
                });
            }
        });
    }
}
