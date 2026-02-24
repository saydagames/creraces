package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to update racial customizations.
 */
public class SetCustomizationPacket {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "set_customization");
    private final Map<String, String> customizations;

    public SetCustomizationPacket(Map<String, String> customizations) {
        this.customizations = customizations;
    }

    public SetCustomizationPacket(FriendlyByteBuf buf) {
        this.customizations = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            this.customizations.put(buf.readUtf(), buf.readUtf());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(customizations.size());
        customizations.forEach((k, v) -> {
            buf.writeUtf(k);
            buf.writeUtf(v);
        });
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
                DataUtils.getVariables(player).ifPresent(vars -> {
                    customizations.forEach(vars::setCustomization);
                    // Apply to cosmetic system
                    ResourceLocation raceId = vars.getRace();
                    if (raceId.getNamespace().equals("minecraft")) {
                        raceId = new ResourceLocation(CreRaces.MODID, raceId.getPath());
                    }
                    mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(raceId);
                    if (race != null) {
                        mc.sayda.creraces.race.CosmeticIncidents.applyCustomizations(player, vars.getCustomizations(),
                                race);

                        // Explicitly sync to Twilight Lib trackers
                        var addons = mc.sayda.twilight_lib.capabilities.DataUtils.getAddonsData(player);
                        if (addons != null) {
                            mc.sayda.twilight_lib.network.NetworkHandler.sendAddonsToAll(
                                    new mc.sayda.twilight_lib.network.SyncAddonsPacket(
                                            player.getUUID(), addons.getActiveAddons(), addons.getAllAddonTints()));
                        }
                    }
                    // Sync back to tracking players
                    BoundaryHandler.resyncForAllTrackers(player);
                    mc.sayda.creraces.race.AttributeIncidents.eikiJudgment(player);
                });
            }
        });
    }
}
