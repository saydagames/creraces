package mc.sayda.creraces.ability;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.network.SyncAbilitiesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.HashMap;

/**
 * Handles loading of abilities from JSON files in data/creraces/abilities/
 */
public class AbilityManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final String FOLDER = "abilities";
    private static final Map<ResourceLocation, JsonElement> RAW_DATA = new HashMap<>();

    @Override
    @Nonnull
    protected Map<ResourceLocation, JsonElement> prepare(@Nonnull ResourceManager resourceManager,
            @Nonnull ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> files = GsonHelper.getJsonFiles(resourceManager, FOLDER);
        return files != null ? files : new HashMap<>();
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> data, @Nonnull ResourceManager resourceManager,
            @Nonnull ProfilerFiller profiler) {
        RAW_DATA.clear();
        RAW_DATA.putAll(data);
        internalApply(data);

        // Sync with clients if we are on a server
        var server = dev.architectury.utils.GameInstance.getServer();
        if (server != null) {
            broadcastSync();
        }
    }

    private static void internalApply(Map<ResourceLocation, JsonElement> data) {
        AbilityRegistry.clear();
        final int[] count = { 0 };

        data.forEach((id, element) -> {
            try {
                JsonObject jsonObject = element.getAsJsonObject();

                String nameStr = java.util.Objects
                        .requireNonNull(GsonHelper.getAsString(jsonObject, "name", id.getPath()));
                @SuppressWarnings("null")
                @Nonnull
                String nameStrFixed = nameStr;
                String descStr = java.util.Objects
                        .requireNonNull(GsonHelper.getAsString(jsonObject, "description", ""));
                @SuppressWarnings("null")
                @Nonnull
                String descStrFixed = descStr;
                String typeStr = java.util.Objects.requireNonNull(GsonHelper.getAsString(jsonObject, "type", "ACTIVE"));
                @SuppressWarnings("null")
                @Nonnull
                String typeStrFixed = typeStr;
                String iconStr = java.util.Objects.requireNonNull(
                        GsonHelper.getAsString(jsonObject, "icon", "minecraft:textures/item/barrier.png"));
                @SuppressWarnings("null")
                @Nonnull
                String iconStrFixed = iconStr;
                int cooldown = GsonHelper.getAsInt(jsonObject, "cooldown", 0);
                int cost = GsonHelper.getAsInt(jsonObject, "cost", 0);
                boolean persistent = GsonHelper.getAsBoolean(jsonObject, "persistent", false);

                java.util.List<ResourceLocation> allowedRaces = new java.util.ArrayList<>();
                if (jsonObject.has("race")) {
                    JsonElement raceElem = jsonObject.get("race");
                    if (raceElem.isJsonArray()) {
                        for (JsonElement e : raceElem.getAsJsonArray()) {
                            String rStr = e.getAsString();
                            ResourceLocation rl = ResourceLocation.tryParse(rStr);
                            if (rl != null)
                                allowedRaces.add(rl);
                            else
                                CreRaces.LOGGER.warn("Ability {} has malformed race ID: {}", id, rStr);
                        }
                    } else {
                        String rStr = raceElem.getAsString();
                        ResourceLocation rl = ResourceLocation.tryParse(rStr);
                        if (rl != null)
                            allowedRaces.add(rl);
                        else
                            CreRaces.LOGGER.warn("Ability {} has malformed race ID: {}", id, rStr);
                    }
                }

                java.util.List<mc.sayda.creraces.engine.ActionRegistry.RaceAction> onActivate = new java.util.ArrayList<>();
                if (jsonObject.has("actions")) {
                    for (JsonElement e : jsonObject.getAsJsonArray("actions")) {
                        onActivate.add(mc.sayda.creraces.engine.ActionRegistry.fromJson(e.getAsJsonObject()));
                    }
                }

                java.util.List<mc.sayda.creraces.engine.ActionRegistry.RaceAction> onDeactivate = new java.util.ArrayList<>();
                if (jsonObject.has("on_deactivate")) {
                    for (JsonElement e : jsonObject.getAsJsonArray("on_deactivate")) {
                        onDeactivate.add(mc.sayda.creraces.engine.ActionRegistry.fromJson(e.getAsJsonObject()));
                    }
                }

                // Remote Documentation
                if (jsonObject.has("remote_description")) {
                    mc.sayda.creraces.util.RemoteDocConfig remoteConfig = mc.sayda.creraces.util.RemoteDocConfig
                            .fromJson(jsonObject.getAsJsonObject("remote_description"));
                    if (remoteConfig != null) {
                        AbilityRegistry.registerRemoteDoc(id, remoteConfig);
                    }
                }

                if (jsonObject.has("remote_full_description")) {
                    mc.sayda.creraces.util.RemoteDocConfig remoteConfig = mc.sayda.creraces.util.RemoteDocConfig
                            .fromJson(jsonObject.getAsJsonObject("remote_full_description"));
                    if (remoteConfig != null) {
                        AbilityRegistry.registerRemoteFullDoc(id, remoteConfig);
                    }
                }

                ResourceLocation iconRl = ResourceLocation.tryParse(iconStrFixed);
                if (iconRl == null) {
                    CreRaces.LOGGER.warn("Ability {} has malformed icon: {}. Falling back to barrier.", id,
                            iconStrFixed);
                    iconRl = new ResourceLocation("minecraft", "textures/item/barrier.png");
                }

                Ability ability = new Ability(
                        id,
                        Component.translatable(nameStrFixed),
                        Component.translatable(descStrFixed),
                        AbilityType.valueOf(typeStrFixed.toUpperCase()),
                        iconRl,
                        cooldown,
                        cost,
                        persistent,
                        allowedRaces,
                        onActivate,
                        onDeactivate);

                AbilityRegistry.register(ability);
                count[0]++;
            } catch (Exception e) {
                CreRaces.LOGGER.error("Failed to load ability {}: {}", id, e.getMessage());
            }
        });

        CreRaces.LOGGER.info("Applied {} abilities.", count[0]);
    }

    public static void syncFromServer(Map<ResourceLocation, JsonElement> data) {
        internalApply(data);
    }

    /**
     * Creates a SyncAbilitiesPacket from the current cached raw data.
     * Used to sync to individual players on login.
     */
    public static SyncAbilitiesPacket createSyncPacket() {
        Map<ResourceLocation, String> stringData = new HashMap<>();
        RAW_DATA.forEach((id, element) -> stringData.put(id, element.toString()));
        return new SyncAbilitiesPacket(stringData);
    }

    public static void broadcastSync() {
        var server = dev.architectury.utils.GameInstance.getServer();

        if (server == null)
            return;

        Map<ResourceLocation, String> stringData = new HashMap<>();
        RAW_DATA.forEach((id, element) -> stringData.put(id, element.toString()));
        SyncAbilitiesPacket pkt = new SyncAbilitiesPacket(stringData);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            mc.sayda.creraces.network.BoundaryHandler.syncAbilitiesToPlayer(player, pkt);
        }
    }
}
