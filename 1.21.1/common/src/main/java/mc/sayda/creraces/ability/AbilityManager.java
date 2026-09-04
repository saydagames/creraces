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
        mc.sayda.creraces.CreRaces.LOGGER.info("AbilityManager: Preparing data reload...");
        Map<ResourceLocation, JsonElement> files = GsonHelper.getJsonFiles(resourceManager, FOLDER);
        return files != null ? files : new HashMap<>();
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> data, @Nonnull ResourceManager resourceManager,
            @Nonnull ProfilerFiller profiler) {
        mc.sayda.creraces.CreRaces.LOGGER.info("AbilityManager: Applying data reload ({} files found)", data.size());
        RAW_DATA.clear();
        RAW_DATA.putAll(data);
        mc.sayda.creraces.util.RemoteDocFetcher.clearCache();
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

                String path = java.util.Objects.requireNonNull(id.getPath());
                String nameStr = java.util.Objects.requireNonNull(GsonHelper.getAsString(jsonObject, "creraces:name", path));
                String descStr = GsonHelper.getAsString(jsonObject, "creraces:description", "");
                String typeStr = GsonHelper.getAsString(jsonObject, "creraces:type", "ACTIVE");
                String iconStr = GsonHelper.getAsString(jsonObject, "creraces:icon", "minecraft:textures/item/barrier.png");
                
                int cooldown = GsonHelper.getAsInt(jsonObject, "creraces:cooldown", 0);
                int cost = GsonHelper.getAsInt(jsonObject, "creraces:cost", 0);
                boolean persistent = GsonHelper.getAsBoolean(jsonObject, "creraces:persistent", false);

                java.util.List<ResourceLocation> allowedRaces = new java.util.ArrayList<>();
                if (jsonObject.has("creraces:race")) {
                    JsonElement raceElem = jsonObject.get("creraces:race");
                    if (raceElem.isJsonArray()) {
                        for (JsonElement e : raceElem.getAsJsonArray()) {
                            String rStr = java.util.Objects.requireNonNull(e.getAsString());
                            ResourceLocation rl = ResourceLocation.tryParse(rStr);
                            if (rl != null)
                                allowedRaces.add(rl);
                            else
                                CreRaces.LOGGER.warn("Ability {} has malformed race ID: {}", id, rStr);
                        }
                    } else {
                        String rStr = java.util.Objects.requireNonNull(raceElem.getAsString());
                        ResourceLocation rl = ResourceLocation.tryParse(rStr);
                        if (rl != null)
                            allowedRaces.add(rl);
                        else
                            CreRaces.LOGGER.warn("Ability {} has malformed race ID: {}", id, rStr);
                    }
                }

                java.util.List<mc.sayda.creraces.engine.ActionRegistry.RaceAction> onActivate = new java.util.ArrayList<>();
                if (jsonObject.has("creraces:actions")) {
                    for (JsonElement e : jsonObject.getAsJsonArray("creraces:actions")) {
                        onActivate.add(mc.sayda.creraces.engine.ActionRegistry.fromJson(e.getAsJsonObject()));
                    }
                }

                java.util.List<mc.sayda.creraces.engine.ActionRegistry.RaceAction> onDeactivate = new java.util.ArrayList<>();
                if (jsonObject.has("creraces:on_deactivate")) {
                    for (JsonElement e : jsonObject.getAsJsonArray("creraces:on_deactivate")) {
                        onDeactivate.add(mc.sayda.creraces.engine.ActionRegistry.fromJson(e.getAsJsonObject()));
                    }
                }

                // Remote Documentation
                if (jsonObject.has("creraces:wiki_page") && !jsonObject.get("creraces:wiki_page").isJsonNull()) {
                    String wikiPage = jsonObject.get("creraces:wiki_page").getAsString();
                    
                    AbilityRegistry.registerRemoteDoc(id, mc.sayda.creraces.util.RemoteDocConfig.fromWikiPage(wikiPage,
                            mc.sayda.creraces.util.RemoteDocConfig.INFODOC_SELECTOR, descStr));
                    AbilityRegistry.registerRemoteFullDoc(id,
                            mc.sayda.creraces.util.RemoteDocConfig.fromWikiPage(wikiPage,
                                    mc.sayda.creraces.util.RemoteDocConfig.HEADERDOC_SELECTOR, descStr));
                }

                if (jsonObject.has("creraces:remote_description")) {
                    mc.sayda.creraces.util.RemoteDocConfig remoteConfig = mc.sayda.creraces.util.RemoteDocConfig
                            .fromJson(jsonObject.getAsJsonObject("creraces:remote_description"));
                    if (remoteConfig != null) {
                        AbilityRegistry.registerRemoteDoc(id, remoteConfig);
                    }
                }

                if (jsonObject.has("creraces:remote_full_description")) {
                    mc.sayda.creraces.util.RemoteDocConfig remoteConfig = mc.sayda.creraces.util.RemoteDocConfig
                            .fromJson(jsonObject.getAsJsonObject("creraces:remote_full_description"));
                    if (remoteConfig != null) {
                        AbilityRegistry.registerRemoteFullDoc(id, remoteConfig);
                    }
                }

                ResourceLocation iconRl = ResourceLocation.tryParse(iconStr);
                if (iconRl == null) {
                    CreRaces.LOGGER.warn("Ability {} has malformed icon: {}. Falling back to barrier.", id,
                            iconStr);
                    iconRl = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/barrier.png");
                }

                AbilityType type = AbilityType.ACTIVE;
                try {
                    type = AbilityType.valueOf(typeStr.toUpperCase());
                } catch (Exception e) {
                    CreRaces.LOGGER.warn("Ability {} has unknown type: {}", id, typeStr);
                }

                mc.sayda.creraces.engine.condition.Condition condition = (player, target, slot, interactionPos) -> true;
                if (jsonObject.has("creraces:condition")) {
                    condition = mc.sayda.creraces.engine.condition.Condition.fromJson(jsonObject.getAsJsonObject("creraces:condition"));
                }

                String conditionFailMessage = jsonObject.has("creraces:condition_fail_message")
                        ? jsonObject.get("creraces:condition_fail_message").getAsString()
                        : null;

                java.util.List<mc.sayda.creraces.ability.OverlayBar> overlayBars = new java.util.ArrayList<>();
                mc.sayda.creraces.ability.OverlayBar.collectOverlayBars(jsonObject, overlayBars);

                Ability ability = new Ability(
                        id,
                        Component.translatable(nameStr),
                        Component.translatable(descStr),
                        type,
                        iconRl,
                        cooldown,
                        cost,
                        persistent,
                        allowedRaces,
                        onActivate,
                        onDeactivate,
                        condition,
                        conditionFailMessage,
                        overlayBars);

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
