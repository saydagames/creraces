package mc.sayda.creraces.race;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.architectury.utils.GameInstance;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.OverlayBar;
import mc.sayda.creraces.engine.GState;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.TraitRegistry.RaceTrait;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.SyncRacesPacket;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.RemoteDocConfig;
import mc.sayda.creraces.util.RemoteDocFetcher;
import mc.sayda.creraces.util.WikiUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Loads race JSONs from data/creraces/races/. Keys with a namespace ("creraces:...") are race
 * properties; unprefixed keys are trait categories.
 */
public class RaceManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final String FOLDER = "races";
    private static final Set<String> FOOD_CATEGORIES = Set.of(
            "meat", "vegetable", "fruit", "grain", "sweet", "dairy", "seafood", "fishes");

    private static volatile Map<ResourceLocation, JsonElement> lastRawData = new HashMap<>();

    public static SyncRacesPacket createSyncPacket() {
        Map<ResourceLocation, String> data = new HashMap<>();
        lastRawData.forEach((id, element) -> data.put(id, element.toString()));
        return new SyncRacesPacket(data);
    }

    @Override
    @Nonnull
    protected Map<ResourceLocation, JsonElement> prepare(@Nonnull ResourceManager resourceManager,
            @Nonnull ProfilerFiller profiler) {
        CreRaces.LOGGER.info("RaceManager: Preparing data reload...");
        Map<ResourceLocation, JsonElement> files = GsonHelper.getJsonFiles(resourceManager, FOLDER);
        return files != null ? files : new HashMap<>();
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> data, @Nonnull ResourceManager resourceManager,
            @Nonnull ProfilerFiller profiler) {
        CreRaces.LOGGER.info("RaceManager: Applying data reload ({} files found)", data.size());
        lastRawData = data;
        RemoteDocFetcher.clearCache();
        syncFromServer(data);

        var server = GameInstance.getServer();
        if (server != null) {
            var pkt = createSyncPacket();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                BoundaryHandler.syncRacesToPlayer(player, pkt);
            }
        }
    }

    public static void syncFromServer(Map<ResourceLocation, JsonElement> data) {
        RaceRegistry.clear();

        Map<ResourceLocation, JsonObject> resolvedData = new HashMap<>();
        data.forEach((id, element) -> {
            if (element.isJsonObject()) {
                try {
                    resolvedData.put(id, resolveInheritance(id, data, new HashSet<>()));
                } catch (Exception e) {
                    CreRaces.LOGGER.error("Failed to resolve inheritance for race {}: {}", id, e.getMessage());
                    resolvedData.put(id, element.getAsJsonObject());
                }
            }
        });

        int count = 0;
        for (Map.Entry<ResourceLocation, JsonObject> entry : resolvedData.entrySet()) {
            try {
                RaceRegistry.register(parseRace(entry.getKey(), entry.getValue()));
                count++;
            } catch (Exception e) {
                CreRaces.LOGGER.error("Failed to load race {}: ", entry.getKey(), e);
            }
        }

        CreRaces.LOGGER.info("Loaded {} races.", count);
        for (Race race : RaceRegistry.getAll()) {
            if (race.passives() != null && race.passives().liquidSpeedMultiplier().base() != 1.0) {
                CreRaces.LOGGER.debug("Race {} has liquid speed multiplier: {}", race.id(),
                        race.passives().liquidSpeedMultiplier().base());
            }
        }
    }

    private static Race parseRace(ResourceLocation id, JsonObject json) {
        String path = Objects.requireNonNull(id.getPath());
        String nameStr = Objects.requireNonNull(GsonHelper.getAsString(json, "creraces:name", path));
        String descStr = GsonHelper.getAsString(json, "creraces:description", "");

        ResourceLocation icon = parseLocation(json, "creraces:icon", "minecraft:textures/item/barrier.png");
        ResourceLocation portrait = parseLocation(json, "creraces:portrait", "creraces:textures/screens/race.png");
        ResourceLocation splash = parseLocation(json, "creraces:splash", "creraces:textures/screens/unknown_splash.png");
        String nameTextureStr = GsonHelper.getNullableString(json, "creraces:name_texture", null);
        ResourceLocation nameTexture = nameTextureStr != null ? ResourceLocation.tryParse(nameTextureStr) : null;
        ResourceLocation bgTexture = parseLocation(json, "creraces:bg_texture", "creraces:textures/screens/selection_bg.png");

        double index = GsonHelper.getAsDouble(json, "creraces:index", Double.MAX_VALUE);
        int baseAp = GsonHelper.getAsInt(json, "creraces:base_ap", 0);
        int baseAd = GsonHelper.getAsInt(json, "creraces:base_ad", 0);
        int baseAh = GsonHelper.getAsInt(json, "creraces:base_ah", 0);
        int baseCr = GsonHelper.getAsInt(json, "creraces:base_cr", 0);
        ResourceType resourceType = parseResourceType(id, json);
        RaceScale scale = RaceScale.fromJson(json.get("creraces:scale"));
        int difficulty = GsonHelper.getAsInt(json, "creraces:difficulty", 0);

        int splashX = GsonHelper.getAsInt(json, "creraces:splash_x", 15);
        int splashY = GsonHelper.getAsInt(json, "creraces:splash_y", -10);
        int splashW = GsonHelper.getAsInt(json, "creraces:splash_w", 141);
        int splashH = GsonHelper.getAsInt(json, "creraces:splash_h", 199);

        int nameTexX = GsonHelper.getAsInt(json, "creraces:name_tex_x", 44);
        int nameTexY = GsonHelper.getAsInt(json, "creraces:name_tex_y", -35);
        int nameTexW = GsonHelper.getAsInt(json, "creraces:name_tex_w", 86);
        int nameTexH = GsonHelper.getAsInt(json, "creraces:name_tex_h", 20);

        List<RaceCustomization> customizations = parseCustomizations(json);
        List<ResourceLocation> startingAbilities = parseIdList(json, "creraces:starting_abilities");
        List<ResourceLocation> startingItems = parseIdList(json, "creraces:starting_items");
        List<RaceTrait> traits = parseTraits(json);

        GState gState = GState.BOTH;
        String gStateStr = GsonHelper.getNullableString(json, "creraces:gstate", null);
        if (gStateStr != null) {
            gState = GState.fromString(gStateStr);
        }

        registerRemoteDocs(id, nameStr, json);

        String selectionDimStr = GsonHelper.getNullableString(json, "creraces:selection_dimension", null);
        ResourceLocation selectionDim = selectionDimStr != null ? ResourceLocation.tryParse(selectionDimStr) : null;
        double[] selectionPos = parsePosition(json, "creraces:selection_pos");

        // Default respawn point, used when the player has no bed or anchor spawn.
        String respawnDimStr = GsonHelper.getNullableString(json, "creraces:respawn_dimension", null);
        ResourceLocation respawnDim = respawnDimStr != null ? ResourceLocation.tryParse(respawnDimStr) : null;
        double[] respawnPos = parsePosition(json, "creraces:respawn_pos");

        return new Race.Builder(id, Component.translatable(nameStr))
                .description(Component.translatable(descStr))
                .icon(icon)
                .portrait(portrait)
                .splash(splash)
                .nameTexture(nameTexture)
                .parentRaces(parseParentRaces(json))
                .index(index)
                .stats(baseAp, baseAd, baseAh, baseCr)
                .scale(scale)
                .resource(resourceType)
                .bgTexture(bgTexture)
                .difficulty(difficulty)
                .splashDimensions(splashX, splashY, splashW, splashH)
                .nameBoxDimensions(nameTexX, nameTexY, nameTexW, nameTexH)
                .customizations(customizations)
                .startingAbilities(startingAbilities)
                .startingItems(startingItems)
                .passives(parsePassives(json))
                .traits(traits)
                .overlayBars(OverlayBar.collectOverlayBarsResult(json))
                .isSpirit(GsonHelper.getAsBoolean(json, "creraces:is_spirit", false))
                .isTiny(GsonHelper.getAsBoolean(json, "creraces:is_tiny", false))
                .isAquatic(GsonHelper.getAsBoolean(json, "creraces:is_aquatic", false))
                .isUndead(GsonHelper.getAsBoolean(json, "creraces:is_undead", false))
                .selectable(GsonHelper.getAsBoolean(json, "creraces:selectable", true))
                .gState(gState)
                .state(Race.RaceState.fromString(GsonHelper.getNullableString(json, "creraces:state", "FINISHED")))
                .selectionDimension(selectionDim)
                .selectionPos(selectionPos)
                .respawnDimension(respawnDim)
                .respawnPos(respawnPos)
                .biomePreview(GsonHelper.getAsBoolean(json, "creraces:territory_biome_preview", false))
                .claimValidBiomes(parseStringList(json, "creraces:territory_valid_biomes"))
                .claimBiomeThreshold(GsonHelper.getAsFloat(json, "creraces:territory_biome_threshold", 0.5f))
                .enableTerritory(GsonHelper.getAsBoolean(json, "creraces:enable_territory", false))
                .build();
    }

    /** Parses a texture location, falling back when the key is missing or malformed. */
    private static ResourceLocation parseLocation(JsonObject json, String key, String fallback) {
        ResourceLocation location = ResourceLocation.tryParse(GsonHelper.getAsString(json, key, fallback));
        return location != null ? location : ResourceLocation.tryParse(fallback);
    }

    private static ResourceType parseResourceType(ResourceLocation id, JsonObject json) {
        String typeStr = GsonHelper.getAsString(json, "creraces:resource_type", "NONE");
        if (typeStr.contains(":"))
            typeStr = typeStr.substring(typeStr.indexOf(':') + 1);
        try {
            return ResourceType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            CreRaces.LOGGER.warn("Race {} has unknown resource type: {}", id, typeStr);
            return ResourceType.NONE;
        }
    }

    /**
     * creraces:race_defaults maps a race id to per-customization defaults. Each customization
     * keeps only the entries for its own id.
     */
    private static List<RaceCustomization> parseCustomizations(JsonObject json) {
        Map<String, Map<String, String>> defaultsByRace = new HashMap<>();
        JsonElement defaultsElem = json.get("creraces:race_defaults");
        if (defaultsElem != null && defaultsElem.isJsonObject()) {
            for (Map.Entry<String, JsonElement> raceEntry : defaultsElem.getAsJsonObject().entrySet()) {
                if (raceEntry.getValue().isJsonObject()) {
                    Map<String, String> defaults = new HashMap<>();
                    for (Map.Entry<String, JsonElement> defEntry : raceEntry.getValue().getAsJsonObject().entrySet()) {
                        defaults.put(defEntry.getKey(), defEntry.getValue().getAsString());
                    }
                    defaultsByRace.put(raceEntry.getKey(), defaults);
                }
            }
        }

        List<RaceCustomization> customizations = new ArrayList<>();
        if (json.has("creraces:customization")) {
            for (JsonElement custElem : json.getAsJsonArray("creraces:customization")) {
                JsonObject custObj = custElem.getAsJsonObject();
                String custId = GsonHelper.getAsString(custObj, "id", "unknown");

                Map<String, String> custDefaults = new HashMap<>();
                defaultsByRace.forEach((raceId, defaults) -> {
                    if (defaults.containsKey(custId)) {
                        custDefaults.put(raceId, defaults.get(custId));
                    }
                });

                customizations.add(RaceCustomization.fromJson(custObj, custDefaults));
            }
        }
        return customizations;
    }

    /** Collects every trait in unprefixed category keys, plus the explicit creraces:traits list. */
    private static List<RaceTrait> parseTraits(JsonObject json) {
        List<RaceTrait> traits = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            if (key.contains(":"))
                continue;

            JsonElement value = entry.getValue();
            if (value.isJsonArray()) {
                JsonArray array = value.getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    JsonElement e = array.get(i);
                    if (e.isJsonObject()) {
                        traits.add(TraitRegistry.fromJson(e.getAsJsonObject(), key + ":" + i));
                    }
                }
            } else if (value.isJsonObject()) {
                JsonObject obj = value.getAsJsonObject();
                if (obj.has("type")) {
                    traits.add(TraitRegistry.fromJson(obj, key + ":0"));
                }
            }
        }

        if (json.has("creraces:traits") && json.get("creraces:traits").isJsonArray()) {
            JsonArray coreTraits = json.getAsJsonArray("creraces:traits");
            for (int i = 0; i < coreTraits.size(); i++) {
                JsonElement e = coreTraits.get(i);
                if (e.isJsonObject()) {
                    traits.add(TraitRegistry.fromJson(e.getAsJsonObject(), "creraces:traits:" + i));
                }
            }
        }
        return traits;
    }

    private static void registerRemoteDocs(ResourceLocation id, String nameStr, JsonObject json) {
        String descriptionKey = "race.creraces." + id.getPath() + ".description";
        if (json.has("creraces:wiki_page")) {
            String wikiPage = GsonHelper.getAsString(json, "creraces:wiki_page", "");
            RaceRegistry.registerRemoteDoc(id, RemoteDocConfig.fromWikiPage(wikiPage,
                    RemoteDocConfig.INFODOC_SELECTOR, descriptionKey));
            RaceRegistry.registerRemotePassive(id, RemoteDocConfig.fromWikiPage(wikiPage,
                    RemoteDocConfig.PASSIVE_SELECTOR, "race.creraces." + id.getPath() + ".passive"));
        }

        if (json.has("creraces:remote_description")) {
            RaceRegistry.registerRemoteDoc(id, new RemoteDocConfig(
                    Objects.requireNonNull(WikiUtils.getRaceUrl(Component.literal(nameStr))),
                    RemoteDocConfig.RACE_DESCRIPTION_SELECTOR, descriptionKey));
        }

        if (json.has("creraces:remote_passive") && json.get("creraces:remote_passive").isJsonObject()) {
            RemoteDocConfig remoteConfig = RemoteDocConfig.fromJson(json.getAsJsonObject("creraces:remote_passive"));
            if (remoteConfig != null) {
                RaceRegistry.registerRemotePassive(id, remoteConfig);
            }
        }
    }

    /** Reads an {"x", "y", "z"} object, or returns null if the key is absent. */
    @Nullable
    private static double[] parsePosition(JsonObject json, String key) {
        JsonElement elem = json.get(key);
        if (elem == null || !elem.isJsonObject())
            return null;
        JsonObject pos = elem.getAsJsonObject();
        return new double[] { pos.get("x").getAsDouble(), pos.get("y").getAsDouble(), pos.get("z").getAsDouble() };
    }

    private static List<ResourceLocation> parseParentRaces(JsonObject json) {
        List<ResourceLocation> parentRaces = new ArrayList<>();
        String singleParent = GsonHelper.getNullableString(json, "creraces:parent_race", null);
        if (singleParent != null) {
            ResourceLocation parentId = ResourceLocation.tryParse(singleParent);
            if (parentId != null)
                parentRaces.add(parentId);
        }
        if (json.has("creraces:parent_races") && json.get("creraces:parent_races").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("creraces:parent_races")) {
                ResourceLocation parentId = ResourceLocation.tryParse(e.getAsString());
                if (parentId != null && !parentRaces.contains(parentId))
                    parentRaces.add(parentId);
            }
        }
        return parentRaces;
    }

    /** Merges each parent's fully resolved JSON under the race's own, child values winning. */
    private static JsonObject resolveInheritance(ResourceLocation id, Map<ResourceLocation, JsonElement> data,
            Set<ResourceLocation> visited) {
        if (!visited.add(id)) {
            // Already on this inheritance path: a cycle, so stop and use the raw JSON.
            return data.get(id).getAsJsonObject().deepCopy();
        }

        JsonObject current = data.get(id).getAsJsonObject().deepCopy();

        String singleParent = GsonHelper.getNullableString(current, "creraces:parent_race", null);
        if (singleParent != null) {
            ResourceLocation parentId = ResourceLocation.tryParse(singleParent);
            if (parentId != null && data.containsKey(parentId)) {
                current = mergeRaces(resolveInheritance(parentId, data, visited), current);
            } else if (parentId != null) {
                CreRaces.LOGGER.warn("Race {} references missing parent race: {}", id, parentId);
            }
        }

        if (current.has("creraces:parent_races") && current.get("creraces:parent_races").isJsonArray()) {
            for (JsonElement e : current.getAsJsonArray("creraces:parent_races")) {
                ResourceLocation parentId = ResourceLocation.tryParse(e.getAsString());
                if (parentId != null && data.containsKey(parentId)) {
                    current = mergeRaces(resolveInheritance(parentId, data, visited), current);
                } else if (parentId != null) {
                    CreRaces.LOGGER.warn("Race {} references missing multi-parent race: {}", id, parentId);
                }
            }
        }

        return current;
    }

    private static JsonObject mergeRaces(JsonObject parent, JsonObject child) {
        JsonObject merged = parent.deepCopy();

        // Selection metadata belongs to the parent itself and is never inherited.
        merged.remove("creraces:selectable");
        merged.remove("creraces:parent_race");
        merged.remove("creraces:parent_races");
        merged.remove("creraces:index");

        for (Map.Entry<String, JsonElement> entry : child.entrySet()) {
            String key = entry.getKey();
            JsonElement childVal = entry.getValue();
            JsonElement parentVal = merged.get(key);

            if (parentVal != null && childVal.isJsonObject() && parentVal.isJsonObject()) {
                // Objects (passives, trait categories) merge one level deep.
                JsonObject parentObj = parentVal.getAsJsonObject();
                for (Map.Entry<String, JsonElement> childEntry : childVal.getAsJsonObject().entrySet()) {
                    parentObj.add(childEntry.getKey(), childEntry.getValue());
                }
            } else if (parentVal != null && childVal.isJsonArray() && parentVal.isJsonArray()
                    && isAppendedList(key)) {
                parentVal.getAsJsonArray().addAll(childVal.getAsJsonArray());
            } else if (parentVal != null && childVal.isJsonArray() && parentVal.isJsonArray()
                    && (key.equals("creraces:customization") || key.equals("customization"))) {
                mergeCustomizationsById(parentVal.getAsJsonArray(), childVal.getAsJsonArray());
            } else {
                merged.add(key, childVal);
            }
        }

        return merged;
    }

    /** Lists whose child entries are added to the parent's rather than replacing them. */
    private static boolean isAppendedList(String key) {
        return key.endsWith("traits")
                || key.equals("creraces:starting_abilities") || key.equals("starting_abilities")
                || key.equals("creraces:starting_items") || key.equals("starting_items")
                || key.equals("creraces:race_addons") || key.equals("race_addons")
                || !key.contains(":");
    }

    /** A child customization replaces the parent's entry with the same id, otherwise it is appended. */
    private static void mergeCustomizationsById(JsonArray parentArray, JsonArray childArray) {
        for (JsonElement childElem : childArray) {
            if (!childElem.isJsonObject())
                continue;
            JsonObject childObj = childElem.getAsJsonObject();
            String childId = GsonHelper.getNullableString(childObj, "id", null);
            if (childId == null) {
                parentArray.add(childElem);
                continue;
            }

            boolean replaced = false;
            for (int i = 0; i < parentArray.size(); i++) {
                JsonElement parentElem = parentArray.get(i);
                if (parentElem.isJsonObject()
                        && childId.equals(GsonHelper.getNullableString(parentElem.getAsJsonObject(), "id", null))) {
                    parentArray.set(i, childObj);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                parentArray.add(childElem);
            }
        }
    }

    private static List<String> parseStringList(JsonObject json, String key) {
        List<String> result = new ArrayList<>();
        if (json.has(key) && json.get(key).isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray(key)) {
                result.add(e.getAsString());
            }
        }
        return result;
    }

    /** Parses a list of resource ids, silently skipping malformed ones. */
    private static List<ResourceLocation> parseIdList(JsonObject json, String key) {
        List<ResourceLocation> result = new ArrayList<>();
        for (String entry : parseStringList(json, key)) {
            ResourceLocation id = ResourceLocation.tryParse(Objects.requireNonNull(entry));
            if (id != null)
                result.add(id);
        }
        return result;
    }

    private static List<String> parseStandardizedIdList(JsonObject json, String key) {
        List<String> result = new ArrayList<>();
        for (String entry : parseStringList(json, key)) {
            result.add(standardizeId(entry));
        }
        return result;
    }

    private static Race.Passives parsePassives(JsonObject p) {
        // burns_in_sunlight: true means once a second; a number is the interval in ticks.
        int sunlightBurnInterval = parseInterval(p, "creraces:burns_in_sunlight", 20, -1);
        // can_breathe_on_land: false suffocates on a 1-tick interval; a number is the interval in ticks.
        int landSuffocationInterval = parseInterval(p, "creraces:can_breathe_on_land", -1, 1);

        Race.EntitySpawnData spawnOnDeath = null;
        JsonElement spawnElement = p.get("creraces:spawn_on_death");
        if (spawnElement != null && spawnElement.isJsonObject()) {
            JsonObject spawn = spawnElement.getAsJsonObject();
            spawnOnDeath = new Race.EntitySpawnData(
                    GsonHelper.getAsString(spawn, "entity_type", ""),
                    GsonHelper.getAsString(spawn, "nbt", "{}"),
                    GsonHelper.getAsInt(spawn, "count", 1));
        }

        return new Race.Passives(
                // Breathing & Environmental
                GsonHelper.getAsBoolean(p, "creraces:can_breathe_underwater", false),
                landSuffocationInterval,
                sunlightBurnInterval,
                parseStandardizedIdList(p, "creraces:immune_to_damage"),
                parseStandardizedIdList(p, "creraces:negate_effects"),

                // Vision & Perception
                GsonHelper.getAsBoolean(p, "creraces:water_vision", false),
                GsonHelper.getAsBoolean(p, "creraces:lava_vision", false),

                // Movement & Physics
                GsonHelper.getAsBoolean(p, "creraces:can_fly", false),
                ScalingValue.fromJson(p, "creraces:liquid_speed_multiplier", 1.0),
                GsonHelper.getAsBoolean(p, "creraces:unaffected_by_water", false),
                GsonHelper.getAsBoolean(p, "creraces:unaffected_by_lava", false),
                GsonHelper.getAsBoolean(p, "creraces:cannot_sprint", false),

                // Health & Regeneration
                GsonHelper.getAsBoolean(p, "creraces:no_natural_regeneration", false),
                ScalingValue.fromJson(p, "creraces:regeneration_multiplier", 1.0),

                // Combat & Damage
                GsonHelper.getAsBoolean(p, "creraces:immune_to_knockback", false),
                ScalingValue.fromJson(p, "creraces:invulnerability_ticks_multiplier", 1.0),

                // Food & Hunger
                GsonHelper.getAsBoolean(p, "creraces:no_hunger", false),
                GsonHelper.getAsBoolean(p, "creraces:no_hunger_drain", false),
                ScalingValue.fromJson(p, "creraces:fixed_hunger", 0.0),
                parseStandardizedIdList(p, "creraces:blocked_food_types"),
                parseStandardizedIdList(p, "creraces:allowed_food_types"),
                GsonHelper.getAsBoolean(p, "creraces:can_eat_when_full", false),

                // Social & Interaction
                parseStandardizedIdList(p, "creraces:hated_by_entities"),
                parseStandardizedIdList(p, "creraces:respected_by_entities"),
                parseStandardizedIdList(p, "creraces:defended_by_entities"),

                // Special Mechanics
                spawnOnDeath,
                GsonHelper.getAsBoolean(p, "creraces:can_command_socials", false));
    }

    /** Reads a boolean-or-number interval field; -1 means disabled. */
    private static int parseInterval(JsonObject json, String key, int whenTrue, int whenFalse) {
        JsonElement el = json.get(key);
        if (el == null || !el.isJsonPrimitive())
            return -1;
        if (el.getAsJsonPrimitive().isBoolean())
            return el.getAsBoolean() ? whenTrue : whenFalse;
        if (el.getAsJsonPrimitive().isNumber())
            return el.getAsInt();
        return -1;
    }

    /**
     * Normalises an id or #tag to a namespaced form, defaulting to minecraft. Food category
     * keywords (also accepted as #keyword or #creraces:keyword) come back as the bare keyword.
     */
    private static String standardizeId(String id) {
        if (id == null || id.isEmpty())
            return "";
        String stripped = id.startsWith("#") ? id.substring(1) : id;
        String keyword = stripped.startsWith("creraces:") ? stripped.substring("creraces:".length()) : stripped;
        if (FOOD_CATEGORIES.stream().anyMatch(keyword::equalsIgnoreCase))
            return keyword.toLowerCase();

        if (id.startsWith("#")) {
            return "#" + (stripped.contains(":") ? stripped : "minecraft:" + stripped);
        }
        return id.contains(":") ? id : "minecraft:" + id;
    }
}
