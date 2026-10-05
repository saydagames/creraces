package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceIncidents;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.function.Supplier;

/** C2S: finalizes the player's race selection. */
public class SetRacePacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "set_race");

    private final ResourceLocation raceId;

    public SetRacePacket(ResourceLocation raceId) {
        this.raceId = raceId;
    }

    public SetRacePacket(FriendlyByteBuf buf) {
        this.raceId = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(Objects.requireNonNull(this.raceId));
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer sp)) return;
            DataUtils.getVariables(sp).ifPresent(vars -> {
                // The first pick is free; changing an existing race needs op
                if (vars.hasChosenRace() && !sp.hasPermissions(2)) return;

                Race race = RaceRegistry.get(raceId);
                if (race == null) {
                    CreRaces.LOGGER.warn("Player {} attempted to set unregistered race: {}",
                            sp.getName().getString(), raceId);
                    return;
                }

                RaceIncidents.transformPlayer(sp, raceId);
                CreRaces.LOGGER.info("Player {} chose race: {}", sp.getName().getString(), raceId);
                teleportToSelectionPoint(sp, vars, race);
            });
        });
    }

    /** Races may define a spot to warp to on selection; the player's old position becomes their return point. */
    private static void teleportToSelectionPoint(ServerPlayer sp, IPlayerVariables vars, Race race) {
        double[] selPos = race.selectionPos();
        if (selPos == null) return;

        ServerLevel targetLevel = sp.serverLevel();
        if (race.selectionDimension() != null) {
            ServerLevel dimLevel = sp.server.getLevel(ResourceKey.create(Registries.DIMENSION, race.selectionDimension()));
            if (dimLevel != null) targetLevel = dimLevel;
        }
        vars.setReturnDim(sp.level().dimension().location().toString());
        vars.setReturnX(sp.getX());
        vars.setReturnY(sp.getY());
        vars.setReturnZ(sp.getZ());
        sp.teleportTo(targetLevel, selPos[0], selPos[1], selPos[2], sp.getYRot(), sp.getXRot());
    }
}
