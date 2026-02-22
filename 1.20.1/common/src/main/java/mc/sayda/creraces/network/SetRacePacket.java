package mc.sayda.creraces.network;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.RaceIncidents;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to finalize race selection.
 */
public class SetRacePacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "set_race");
    private final ResourceLocation raceId;

    public SetRacePacket(ResourceLocation raceId) {
        this.raceId = raceId;
    }

    public SetRacePacket(FriendlyByteBuf buf) {
        this.raceId = buf.readResourceLocation();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.raceId);
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        dev.architectury.networking.NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            net.minecraft.world.entity.player.Player player = context.getPlayer();
            if (player instanceof ServerPlayer sp) {
                DataUtils.getVariables(sp).ifPresent(vars -> {
                    // Logic check: only allow if player hasn't chosen yet or has permission
                    if (!vars.hasChosenRace() || sp.hasPermissions(2)) {
                        if (RaceRegistry.get(raceId) != null) {
                            RaceIncidents.transformPlayer(sp, raceId);
                            CreRaces.LOGGER.info("Player {} chose race: {}", sp.getName().getString(), raceId);
                        } else {
                            CreRaces.LOGGER.error("Player {} attempted to set invalid race: {}",
                                    sp.getName().getString(), raceId);
                        }
                    }
                });
            }
        });
    }
}
