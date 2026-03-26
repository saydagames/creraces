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
                    // Size guard: a player can't have more customization keys than their race
                    // defines
                    mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
                    if (race == null)
                        return;

                    // Build the set of valid customization keys for this race
                    java.util.Set<String> validKeys = new java.util.HashSet<>();
                    for (mc.sayda.creraces.race.RaceCustomization cust : race.customization()) {
                        validKeys.add(cust.id());
                    }

                    // Reject oversized packets outright
                    if (customizations.size() > validKeys.size()) {
                        CreRaces.LOGGER.warn("Player {} sent oversized customization packet ({} entries, max {})",
                                player.getName().getString(), customizations.size(), validKeys.size());
                        return;
                    }

                    customizations.forEach((key, value) -> {
                        // Only accept keys that are defined in this race's customizations
                        if (!validKeys.contains(key)) {
                            CreRaces.LOGGER.warn("Player {} sent invalid customization key: '{}'",
                                    player.getName().getString(), key);
                            return;
                        }
                        // Clamp value length to prevent exploits
                        String clampedValue = value;
                        int maxLen = mc.sayda.creraces.config.CreRacesConfig.CUSTOMIZATION_VALUE_MAX_LENGTH.get();
                        if (maxLen > 0 && clampedValue.length() > maxLen) {
                            clampedValue = clampedValue.substring(0, maxLen);
                        }
                        vars.setCustomization(key, clampedValue);
                    });

                    // Apply to cosmetic system
                    mc.sayda.creraces.race.CosmeticIncidents.applyCustomizations(player, vars.getCustomizations(),
                            race);

                    // Explicitly sync to Twilight Lib trackers
                    var addons = mc.sayda.twilight_lib.capabilities.DataUtils.getAddonsData(player);
                    if (addons != null) {
                        mc.sayda.twilight_lib.network.NetworkHandler.sendAddonsToAll(
                                mc.sayda.creraces.race.CosmeticIncidents.createSyncPacket(
                                        player.getUUID(), addons.getActiveAddons(), 
                                        mc.sayda.creraces.race.CosmeticIncidents.getExternalGrantsRobust(addons),
                                        addons.getAllAddonTints()));
                    }

                    // Sync back to tracking players
                    BoundaryHandler.resyncForAllTrackers(player);
                    mc.sayda.creraces.race.AttributeIncidents.eikiJudgment(player);

                    // Consume the mirror item
                    if (!player.getAbilities().instabuild) {
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
                            if (stack.is(java.util.Objects.requireNonNull(mc.sayda.creraces.registry.ModItems.MIRROR.get()))) {
                                stack.shrink(1);
                                break;
                            }
                        }
                    }
                });
            }
        });
    }
}
