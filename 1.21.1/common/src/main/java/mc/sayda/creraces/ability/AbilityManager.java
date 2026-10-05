package mc.sayda.creraces.ability;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.architectury.utils.GameInstance;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ActionRegistry.RaceAction;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.RemoteDocConfig;
import mc.sayda.creraces.util.RemoteDocFetcher;
import mc.sayda.creraces.network.SyncAbilitiesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

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
        CreRaces.LOGGER.info("AbilityManager: Preparing data reload...");
        Map<ResourceLocation, JsonElement> files = GsonHelper.getJsonFiles(resourceManager, FOLDER);
        return files != null ? files : new HashMap<>();
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> data, @Nonnull ResourceManager resourceManager,
            @Nonnull ProfilerFiller profiler) {
        CreRaces.LOGGER.info("AbilityManager: Applying data reload ({} files found)", data.size());
        RAW_DATA.clear();
        RAW_DATA.putAll(data);
        RemoteDocFetcher.clearCache();
        internalApply(data);
        broadcastSync();
    }

    private static void internalApply(Map<ResourceLocation, JsonElement> data) {
        AbilityRegistry.clear();
        int count = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            try {
                AbilityRegistry.register(parseAbility(entry.getKey(), entry.getValue().getAsJsonObject()));
                count++;
            } catch (Exception e) {
                CreRaces.LOGGER.error("Failed to load ability {}: {}", entry.getKey(), e.getMessage());
            }
        }

        CreRaces.LOGGER.info("Applied {} abilities.", count);
    }

    private static Ability parseAbility(ResourceLocation id, JsonObject json) {
        String path = Objects.requireNonNull(id.getPath());
        String nameStr = Objects.requireNonNull(GsonHelper.getAsString(json, "creraces:name", path));
        String descStr = GsonHelper.getAsString(json, "creraces:description", "");
        String typeStr = GsonHelper.getAsString(json, "creraces:type", "ACTIVE");
        String iconStr = GsonHelper.getAsString(json, "creraces:icon", "minecraft:textures/item/barrier.png");

        int cooldown = GsonHelper.getAsInt(json, "creraces:cooldown", 0);
        int cost = GsonHelper.getAsInt(json, "creraces:cost", 0);
        boolean persistent = GsonHelper.getAsBoolean(json, "creraces:persistent", false);

        List<ResourceLocation> allowedRaces = parseAllowedRaces(id, json);
        List<RaceAction> onActivate = parseActions(json, "creraces:actions");
        List<RaceAction> onDeactivate = parseActions(json, "creraces:on_deactivate");

        registerRemoteDocs(id, descStr, json);

        ResourceLocation icon = ResourceLocation.tryParse(iconStr);
        if (icon == null) {
            CreRaces.LOGGER.warn("Ability {} has malformed icon: {}. Falling back to barrier.", id, iconStr);
            icon = ResourceLocation.tryParse("minecraft:textures/item/barrier.png");
        }

        AbilityType type = AbilityType.ACTIVE;
        try {
            type = AbilityType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            CreRaces.LOGGER.warn("Ability {} has unknown type: {}", id, typeStr);
        }

        Condition condition = (player, target, slot, interactionPos) -> true;
        if (json.has("creraces:condition")) {
            condition = Condition.fromJson(json.getAsJsonObject("creraces:condition"));
        }

        String conditionFailMessage = json.has("creraces:condition_fail_message")
                ? json.get("creraces:condition_fail_message").getAsString()
                : null;

        List<OverlayBar> overlayBars = new ArrayList<>();
        OverlayBar.collectOverlayBars(json, overlayBars);

        return new Ability(id, Component.translatable(nameStr), Component.translatable(descStr), type, icon,
                cooldown, cost, persistent, allowedRaces, onActivate, onDeactivate, condition,
                conditionFailMessage, overlayBars);
    }

    /** creraces:race is either a single race id or an array of them. */
    private static List<ResourceLocation> parseAllowedRaces(ResourceLocation id, JsonObject json) {
        List<ResourceLocation> allowedRaces = new ArrayList<>();
        if (!json.has("creraces:race"))
            return allowedRaces;

        JsonElement raceElem = json.get("creraces:race");
        List<JsonElement> entries = new ArrayList<>();
        if (raceElem.isJsonArray()) {
            raceElem.getAsJsonArray().forEach(entries::add);
        } else {
            entries.add(raceElem);
        }

        for (JsonElement e : entries) {
            String raceStr = Objects.requireNonNull(e.getAsString());
            ResourceLocation raceId = ResourceLocation.tryParse(raceStr);
            if (raceId != null)
                allowedRaces.add(raceId);
            else
                CreRaces.LOGGER.warn("Ability {} has malformed race ID: {}", id, raceStr);
        }
        return allowedRaces;
    }

    private static List<RaceAction> parseActions(JsonObject json, String key) {
        List<RaceAction> actions = new ArrayList<>();
        if (json.has(key)) {
            for (JsonElement e : json.getAsJsonArray(key)) {
                actions.add(ActionRegistry.fromJson(e.getAsJsonObject()));
            }
        }
        return actions;
    }

    private static void registerRemoteDocs(ResourceLocation id, String descStr, JsonObject json) {
        if (json.has("creraces:wiki_page") && !json.get("creraces:wiki_page").isJsonNull()) {
            String wikiPage = json.get("creraces:wiki_page").getAsString();
            AbilityRegistry.registerRemoteDoc(id,
                    RemoteDocConfig.fromWikiPage(wikiPage, RemoteDocConfig.INFODOC_SELECTOR, descStr));
            AbilityRegistry.registerRemoteFullDoc(id,
                    RemoteDocConfig.fromWikiPage(wikiPage, RemoteDocConfig.HEADERDOC_SELECTOR, descStr));
        }

        if (json.has("creraces:remote_description")) {
            RemoteDocConfig remoteConfig = RemoteDocConfig.fromJson(json.getAsJsonObject("creraces:remote_description"));
            if (remoteConfig != null) {
                AbilityRegistry.registerRemoteDoc(id, remoteConfig);
            }
        }

        if (json.has("creraces:remote_full_description")) {
            RemoteDocConfig remoteConfig = RemoteDocConfig.fromJson(json.getAsJsonObject("creraces:remote_full_description"));
            if (remoteConfig != null) {
                AbilityRegistry.registerRemoteFullDoc(id, remoteConfig);
            }
        }
    }

    public static void syncFromServer(Map<ResourceLocation, JsonElement> data) {
        internalApply(data);
    }

    /** Builds the ability sync packet from the raw JSON of the last reload, e.g. for a joining player. */
    public static SyncAbilitiesPacket createSyncPacket() {
        Map<ResourceLocation, String> stringData = new HashMap<>();
        RAW_DATA.forEach((id, element) -> stringData.put(id, element.toString()));
        return new SyncAbilitiesPacket(stringData);
    }

    public static void broadcastSync() {
        var server = GameInstance.getServer();
        if (server == null)
            return;

        SyncAbilitiesPacket pkt = createSyncPacket();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BoundaryHandler.syncAbilitiesToPlayer(player, pkt);
        }
    }
}
