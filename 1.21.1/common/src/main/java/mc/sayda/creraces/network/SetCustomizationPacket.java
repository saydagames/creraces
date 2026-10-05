package mc.sayda.creraces.network;

import dev.architectury.networking.NetworkManager;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.race.AttributeIncidents;
import mc.sayda.creraces.race.CosmeticIncidents;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceCustomization;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.registry.ModItems;
import mc.sayda.twilight_lib.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/** C2S: applies the racial customizations picked in the mirror screen. */
public class SetCustomizationPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "set_customization");

    private static final int MAX_ENTRIES = 256;
    private static final int KEY_MAX_LEN = 128;
    private static final int VALUE_MAX_LEN = 4096;

    private final Map<String, String> customizations;

    public SetCustomizationPacket(Map<String, String> customizations) {
        this.customizations = customizations;
    }

    public SetCustomizationPacket(FriendlyByteBuf buf) {
        this.customizations = new HashMap<>();
        int size = buf.readInt();
        if (size < 0 || size > MAX_ENTRIES) throw new IllegalStateException("Oversized customization packet: " + size);
        for (int i = 0; i < size; i++) {
            this.customizations.put(buf.readUtf(KEY_MAX_LEN), buf.readUtf(VALUE_MAX_LEN));
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
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race == null)
                    return;

                Set<String> validKeys = new HashSet<>();
                for (RaceCustomization cust : race.customization()) {
                    validKeys.add(cust.id());
                }

                // A player can't have more customization keys than their race defines
                if (customizations.size() > validKeys.size()) {
                    CreRaces.LOGGER.warn("Player {} sent oversized customization packet ({} entries, max {})",
                            player.getName().getString(), customizations.size(), validKeys.size());
                    return;
                }

                int maxLen = CreRacesConfig.CUSTOMIZATION_VALUE_MAX_LENGTH.get();
                customizations.forEach((key, value) -> {
                    if (!validKeys.contains(key)) {
                        CreRaces.LOGGER.warn("Player {} sent invalid customization key: '{}'",
                                player.getName().getString(), key);
                        return;
                    }
                    String clampedValue = maxLen > 0 && value.length() > maxLen ? value.substring(0, maxLen) : value;
                    vars.setCustomization(key, clampedValue);
                });

                CosmeticIncidents.applyCustomizations(player, vars.getCustomizations(), race);

                // Twilight Lib keeps its own addon state, so trackers need its sync packet too
                var addons = mc.sayda.twilight_lib.capabilities.DataUtils.getAddonsData(player);
                if (addons != null) {
                    NetworkHandler.sendAddonsToAll(CosmeticIncidents.createSyncPacket(
                            player.getUUID(), addons.getActiveAddons(),
                            CosmeticIncidents.getExternalGrantsRobust(addons),
                            addons.getAllAddonTints()));
                }

                BoundaryHandler.resyncForAllTrackers(player);
                AttributeIncidents.eikiJudgment(player);

                if (!player.getAbilities().instabuild) {
                    consumeMirror(player);
                }
            });
        });
    }

    /**
     * Uses up one mirror item. The glass-break sound only plays when one was actually consumed,
     * since the mirror screen is also reachable without the item.
     */
    private static void consumeMirror(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Objects.requireNonNull(ModItems.MIRROR.get()))) {
                stack.shrink(1);
                player.level().playSound(null, player.blockPosition(),
                        SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.0f);
                return;
            }
        }
    }
}
