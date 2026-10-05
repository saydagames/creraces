package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.GState;
import mc.sayda.creraces.race.CosmeticIncidents;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/** C2S: the player switched their gState from the menu screen. */
public class UpdateGStatePacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "update_gstate");

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
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            DataUtils.getVariables(player).ifPresent(vars -> {
                if (vars.hasChosenRace()) {
                    Race race = RaceRegistry.get(vars.getRace());
                    // The UI hides the choice for races with a forced gState, but a crafted packet
                    // could still ask: re-apply the forced state and ignore the request.
                    if (race != null && race.getGState() != GState.BOTH) {
                        CosmeticIncidents.applyGStateCosmetics(player, race, vars);
                        BoundaryHandler.resyncVariables(player, player);
                        return;
                    }
                    vars.setGState(this.gState);
                    if (race != null) {
                        CosmeticIncidents.applyGStateCosmetics(player, race, vars);
                    }
                } else {
                    vars.setGState(this.gState);
                    CosmeticIncidents.applyGStateAddons(player);
                }
                BoundaryHandler.resyncVariables(player, player);
            });
        });
    }
}
