package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import io.netty.buffer.Unpooled;

import java.util.function.Supplier;

public class UpdateGStatePacket {
    public static final net.minecraft.resources.ResourceLocation ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            CreRaces.MODID, "update_gstate");

    private final int gState;

    public UpdateGStatePacket(int gState) {
        this.gState = gState;
    }

    public UpdateGStatePacket(FriendlyByteBuf buf) {
        this.gState = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(gState);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
                DataUtils.getVariables(player).ifPresent(vars -> {
                    // Server-side enforcement: reject if the race forces a specific gState.
                    // The client UI should already prevent this, but we validate here too.
                    if (vars.hasChosenRace()) {
                        mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
                        if (race != null && race.getGState() != mc.sayda.creraces.engine.GState.BOTH) {
                            // Race is forced - re-apply the correct forced state and ignore the client
                            // request.
                            mc.sayda.creraces.race.CosmeticIncidents.applyGStateCosmetics(player, race, vars);
                            BoundaryHandler.resyncVariables(player, player);
                            return;
                        }
                        // Race allows choice - apply and re-run cosmetics.
                        vars.setGState(this.gState);
                        if (race != null) {
                            mc.sayda.creraces.race.CosmeticIncidents.applyGStateCosmetics(player, race, vars);
                        }
                    } else {
                        vars.setGState(this.gState);
                        mc.sayda.creraces.race.CosmeticIncidents.applyGStateAddons(player);
                    }
                    BoundaryHandler.resyncVariables(player, player);
                });
            }
        });
    }

}
