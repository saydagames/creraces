package mc.sayda.creraces.race;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads race JSONs from data/creraces/races/
 */
public class RaceManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final String FOLDER = "races";
    private static Map<ResourceLocation, JsonElement> lastRawData = new java.util.HashMap<>();

    public static mc.sayda.creraces.network.SyncRacesPacket createSyncPacket() {
        Map<ResourceLocation, String> data = new java.util.HashMap<>();
        lastRawData.forEach((id, element) -> {
            data.put(id, element.toString());
        });
        return new mc.sayda.creraces.network.SyncRacesPacket(data);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nonnull
    protected Map<ResourceLocation, JsonElement> prepare(@Nonnull ResourceManager resourceManager,
            @Nonnull ProfilerFiller profiler) {
        return (Map<ResourceLocation, JsonElement>) (Map<?, ?>) GsonHelper.getJsonFiles(resourceManager, FOLDER);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> data, @Nonnull ResourceManager resourceManager,
            @Nonnull ProfilerFiller profiler) {
        lastRawData = data;
        mc.sayda.creraces.util.RemoteDocFetcher.clearCache();
        syncFromServer(data);

        // Sync to all online players
        var server = dev.architectury.utils.GameInstance.getServer();
        if (server != null) {
            var pkt = createSyncPacket();
            for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
                mc.sayda.creraces.network.BoundaryHandler.syncRacesToPlayer(player, pkt);
            }
        }
    }

    public static void syncFromServer(Map<ResourceLocation, JsonElement> data) {
        RaceRegistry.clear();
        final int[] count = { 0 };

        // 1. Pre-process inheritance
        Map<ResourceLocation, JsonObject> resolvedData = new java.util.HashMap<>();
        data.forEach((id, element) -> {
            if (element.isJsonObject()) {
                try {
                    resolvedData.put(id, resolveInheritance(id, data, new java.util.HashSet<>()));
                } catch (Exception e) {
                    CreRaces.LOGGER.error("Failed to resolve inheritance for race {}: {}", id, e.getMessage());
                    // Fallback to raw data if inheritance fails
                    resolvedData.put(id, element.getAsJsonObject());
                }
            }
        });

        // 2. Parse and Register
        resolvedData.forEach((id, jsonObject) -> {
            try {
                String nameStr = GsonHelper.getAsString(jsonObject, "creraces:name",
                        GsonHelper.getAsString(jsonObject, "name", id.getPath()));
                String descStr = GsonHelper.getAsString(jsonObject, "creraces:description",
                        GsonHelper.getAsString(jsonObject, "description", ""));

                // Portraits (Optional, default to barrier if missing)
                @javax.annotation.Nonnull
                String iconStr = GsonHelper.getAsString(jsonObject, "creraces:icon",
                        GsonHelper.getAsString(jsonObject, "icon", "minecraft:textures/item/barrier.png"));
                @SuppressWarnings("null")
                ResourceLocation icon = ResourceLocation.tryParse(iconStr);
                if (icon == null)
                    icon = new ResourceLocation("minecraft", "textures/item/barrier.png");

                @javax.annotation.Nonnull
                String portraitStr = GsonHelper.getAsString(jsonObject, "creraces:portrait",
                        GsonHelper.getAsString(jsonObject, "portrait", "creraces:textures/screens/race.png"));
                @SuppressWarnings("null")
                ResourceLocation portrait = ResourceLocation.tryParse(portraitStr);
                if (portrait == null)
                    portrait = new ResourceLocation("creraces", "textures/screens/race.png");

                @javax.annotation.Nonnull
                String splashStr = GsonHelper.getAsString(jsonObject, "creraces:splash",
                        GsonHelper.getAsString(jsonObject, "splash", "creraces:textures/screens/unknown_splash.png"));
                @SuppressWarnings("null")
                ResourceLocation splash = ResourceLocation.tryParse(splashStr);
                if (splash == null)
                    splash = new ResourceLocation("creraces", "textures/screens/unknown_splash.png");

                @javax.annotation.Nullable
                String nameTextureStr = GsonHelper.getNullableString(jsonObject, "creraces:name_texture",
                        GsonHelper.getNullableString(jsonObject, "name_texture", null));
                ResourceLocation nameTex = nameTextureStr != null ? ResourceLocation.tryParse(nameTextureStr) : null;

                @javax.annotation.Nonnull
                String bgTextureStr = GsonHelper.getAsString(jsonObject, "creraces:bg_texture",
                        GsonHelper.getAsString(jsonObject, "bg_texture", "creraces:textures/screens/selection_bg.png"));
                @SuppressWarnings("null")
                ResourceLocation bgTex = ResourceLocation.tryParse(bgTextureStr);
                if (bgTex == null)
                    bgTex = new ResourceLocation("creraces", "textures/screens/selection_bg.png");

                @javax.annotation.Nullable
                String parentRaceStr = GsonHelper.getNullableString(jsonObject, "creraces:parent_race",
                        GsonHelper.getNullableString(jsonObject, "parent_race", null));
                ResourceLocation parentRace = parentRaceStr != null ? ResourceLocation.tryParse(parentRaceStr) : null;

                double indexValue = GsonHelper.getAsDouble(jsonObject, "creraces:index",
                        GsonHelper.getAsDouble(jsonObject, "index", Double.MAX_VALUE));

                int baseAp = GsonHelper.getAsInt(jsonObject, "creraces:base_ap",
                        GsonHelper.getAsInt(jsonObject, "base_ap", 0));
                int baseAd = GsonHelper.getAsInt(jsonObject, "creraces:base_ad",
                        GsonHelper.getAsInt(jsonObject, "base_ad", 0));
                int baseAh = GsonHelper.getAsInt(jsonObject, "creraces:base_ah",
                        GsonHelper.getAsInt(jsonObject, "base_ah", 0));
                int baseCr = GsonHelper.getAsInt(jsonObject, "creraces:base_cr",
                        GsonHelper.getAsInt(jsonObject, "base_cr", 0));
                int maxResource = GsonHelper.getAsInt(jsonObject, "creraces:max_resource",
                        GsonHelper.getAsInt(jsonObject, "max_resource", 100));

                JsonElement scaleElem = jsonObject.has("creraces:scale") ? jsonObject.get("creraces:scale")
                        : jsonObject.get("scale");
                RaceScale scale = RaceScale.fromJson(scaleElem);

                String resourceTypeStr = GsonHelper.getAsString(jsonObject, "creraces:resource_type",
                        GsonHelper.getAsString(jsonObject, "resource_type", "NONE"));
                if (resourceTypeStr.contains(":"))
                    resourceTypeStr = resourceTypeStr.substring(resourceTypeStr.indexOf(':') + 1);

                ResourceType resourceType = ResourceType.NONE;
                try {
                    resourceType = ResourceType.valueOf(resourceTypeStr.toUpperCase());
                } catch (Exception e) {
                    CreRaces.LOGGER.warn("Race {} has unknown resource type: {}", id, resourceTypeStr);
                }

                int difficulty = GsonHelper.getAsInt(jsonObject, "creraces:difficulty",
                        GsonHelper.getAsInt(jsonObject, "difficulty", 0));

                // Splash dimensions
                int splashX = GsonHelper.getAsInt(jsonObject, "creraces:splash_x",
                        GsonHelper.getAsInt(jsonObject, "splash_x", 15));
                int splashY = GsonHelper.getAsInt(jsonObject, "creraces:splash_y",
                        GsonHelper.getAsInt(jsonObject, "splash_y", -10));
                int splashW = GsonHelper.getAsInt(jsonObject, "creraces:splash_w",
                        GsonHelper.getAsInt(jsonObject, "splash_w", 141));
                int splashH = GsonHelper.getAsInt(jsonObject, "creraces:splash_h",
                        GsonHelper.getAsInt(jsonObject, "splash_h", 199));

                // Name Dimensions
                int nameTexX = GsonHelper.getAsInt(jsonObject, "creraces:name_tex_x",
                        GsonHelper.getAsInt(jsonObject, "name_tex_x", 44));
                int nameTexY = GsonHelper.getAsInt(jsonObject, "creraces:name_tex_y",
                        GsonHelper.getAsInt(jsonObject, "name_tex_y", -35));
                int nameTexW = GsonHelper.getAsInt(jsonObject, "creraces:name_tex_w",
                        GsonHelper.getAsInt(jsonObject, "name_tex_w", 86));
                int nameTexH = GsonHelper.getAsInt(jsonObject, "creraces:name_tex_h",
                        GsonHelper.getAsInt(jsonObject, "name_tex_h", 20));

                // Customizations
                List<RaceCustomization> customizations = new ArrayList<>();
                JsonArray custArray = jsonObject.has("creraces:customization")
                        ? jsonObject.getAsJsonArray("creraces:customization")
                        : (jsonObject.has("customization") ? jsonObject.getAsJsonArray("customization") : null);
                if (custArray != null) {
                    for (JsonElement custElem : custArray) {
                        customizations.add(RaceCustomization.fromJson(custElem.getAsJsonObject()));
                    }
                }

                // Starting Items/Abilities
                List<ResourceLocation> startingAbilities = new ArrayList<>();
                JsonArray abilArray = jsonObject.has("creraces:starting_abilities")
                        ? jsonObject.getAsJsonArray("creraces:starting_abilities")
                        : (jsonObject.has("starting_abilities") ? jsonObject.getAsJsonArray("starting_abilities")
                                : null);
                if (abilArray != null) {
                    for (JsonElement e : abilArray) {
                        ResourceLocation rl = ResourceLocation.tryParse(e.getAsString());
                        if (rl != null)
                            startingAbilities.add(rl);
                    }
                }

                List<ResourceLocation> startingItems = new ArrayList<>();
                JsonArray itemArray = jsonObject.has("creraces:starting_items")
                        ? jsonObject.getAsJsonArray("creraces:starting_items")
                        : (jsonObject.has("starting_items") ? jsonObject.getAsJsonArray("starting_items") : null);
                if (itemArray != null) {
                    for (JsonElement e : itemArray) {
                        ResourceLocation rl = ResourceLocation.tryParse(e.getAsString());
                        if (rl != null)
                            startingItems.add(rl);
                    }
                }

                // Traits & Categories (Discovery)
                List<mc.sayda.creraces.engine.TraitRegistry.RaceTrait> traits = new ArrayList<>();
                for (java.util.Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                    String key = entry.getKey();
                    if (!key.contains(":")) { // Treat non-prefixed fields as trait categories
                        JsonElement value = entry.getValue();
                        if (value.isJsonArray()) {
                            JsonArray array = value.getAsJsonArray();
                            for (int i = 0; i < array.size(); i++) {
                                JsonElement e = array.get(i);
                                if (e.isJsonObject()) {
                                    traits.add(mc.sayda.creraces.engine.TraitRegistry.fromJson(e.getAsJsonObject(),
                                            key + ":" + i));
                                }
                            }
                        } else if (value.isJsonObject()) {
                            JsonObject obj = value.getAsJsonObject();
                            if (obj.has("type")) {
                                traits.add(mc.sayda.creraces.engine.TraitRegistry.fromJson(obj, key + ":0"));
                            }
                        }
                    }
                }
                // Legacy "traits" array support
                if (jsonObject.has("traits")) {
                    JsonArray array = jsonObject.getAsJsonArray("traits");
                    for (int i = 0; i < array.size(); i++) {
                        traits.add(mc.sayda.creraces.engine.TraitRegistry.fromJson(array.get(i).getAsJsonObject(),
                                "traits:" + i));
                    }
                }

                // gState
                mc.sayda.creraces.engine.GState gState = mc.sayda.creraces.engine.GState.BOTH;
                @javax.annotation.Nullable
                String gStateStr = GsonHelper.getNullableString(jsonObject, "creraces:gstate",
                        GsonHelper.getNullableString(jsonObject, "gstate", null));
                if (gStateStr != null) {
                    gState = mc.sayda.creraces.engine.GState.fromString(gStateStr);
                }

                // Remote Documentation
                if (jsonObject.has("creraces:wiki_page") || jsonObject.has("wiki_page")) {
                    String wikiPage = GsonHelper.getAsString(jsonObject, "creraces:wiki_page",
                            GsonHelper.getAsString(jsonObject, "wiki_page", ""));
                    RaceRegistry.registerRemoteDoc(id, mc.sayda.creraces.util.RemoteDocConfig.fromWikiPage(wikiPage,
                            mc.sayda.creraces.util.RemoteDocConfig.INFODOC_SELECTOR,
                            "race.creraces." + id.getPath() + ".description"));
                    RaceRegistry.registerRemotePassive(id, mc.sayda.creraces.util.RemoteDocConfig.fromWikiPage(wikiPage,
                            mc.sayda.creraces.util.RemoteDocConfig.PASSIVE_SELECTOR,
                            "race.creraces." + id.getPath() + ".passive"));
                }

                if (jsonObject.has("creraces:remote_description") || jsonObject.has("remote_description")) {
                    RaceRegistry.registerRemoteDoc(id, new mc.sayda.creraces.util.RemoteDocConfig(
                            mc.sayda.creraces.util.WikiUtils
                                    .getRaceUrl(net.minecraft.network.chat.Component.literal(nameStr)),
                            mc.sayda.creraces.util.RemoteDocConfig.RACE_DESCRIPTION_SELECTOR,
                            "race.creraces." + id.getPath() + ".description"));
                }

                if (jsonObject.has("creraces:remote_passive") || jsonObject.has("remote_passive")) {
                    JsonObject rpObj = jsonObject.has("creraces:remote_passive")
                            ? jsonObject.getAsJsonObject("creraces:remote_passive")
                            : jsonObject.getAsJsonObject("remote_passive");
                    mc.sayda.creraces.util.RemoteDocConfig remoteConfig = mc.sayda.creraces.util.RemoteDocConfig
                            .fromJson(rpObj);
                    if (remoteConfig != null) {
                        RaceRegistry.registerRemotePassive(id, remoteConfig);
                    }
                }

                Race race = new Race.Builder(id, Component.translatable(nameStr))
                        .description(Component.translatable(descStr))
                        .icon(icon)
                        .portrait(portrait)
                        .splash(splash)
                        .nameTexture(nameTex)
                        .parentRace(parentRace)
                        .index(indexValue)
                        .stats(baseAp, baseAd, baseAh, baseCr)
                        .scale(scale)
                        .resource(resourceType, maxResource)
                        .bgTexture(bgTex)
                        .difficulty(difficulty)
                        .splashDimensions(splashX, splashY, splashW, splashH)
                        .nameBoxDimensions(nameTexX, nameTexY, nameTexW, nameTexH)
                        .customizations(customizations)
                        .startingAbilities(startingAbilities)
                        .startingItems(startingItems)
                        .passives(parsePassives(jsonObject))
                        .traits(traits)
                        .isSpirit(GsonHelper.getAsBoolean(jsonObject, "creraces:is_spirit",
                                GsonHelper.getAsBoolean(jsonObject, "is_spirit", false)))
                        .isTiny(GsonHelper.getAsBoolean(jsonObject, "creraces:is_tiny",
                                GsonHelper.getAsBoolean(jsonObject, "is_tiny", false)))
                        .stacksAffectResource(GsonHelper.getAsBoolean(jsonObject, "creraces:stacks_affect_resource",
                                GsonHelper.getAsBoolean(jsonObject, "stacks_affect_resource", false)))
                        .gState(gState)
                        .build();

                RaceRegistry.register(race);
                count[0]++;
            } catch (Exception e) {
                CreRaces.LOGGER.error("Failed to load race {}: {}", id, e);
            }
        });

        CreRaces.LOGGER.info("Loaded {} races.", count[0]);
    }

    private static JsonObject resolveInheritance(ResourceLocation id, Map<ResourceLocation, JsonElement> data,
            java.util.Set<ResourceLocation> visited) {
        if (!visited.add(id)) {
            return data.get(id).getAsJsonObject().deepCopy(); // Circular dependency
        }

        JsonObject current = data.get(id).getAsJsonObject().deepCopy();
        @javax.annotation.Nullable
        String parentStr = GsonHelper.getNullableString(current, "creraces:parent_race",
                GsonHelper.getNullableString(current, "parent_race", null));

        if (parentStr != null) {
            ResourceLocation parentId = ResourceLocation.tryParse(parentStr);
            if (parentId != null && data.containsKey(parentId)) {
                JsonObject parentResolved = resolveInheritance(parentId, data, visited);
                return mergeRaces(parentResolved, current);
            } else if (parentId != null) {
                CreRaces.LOGGER.warn("Race {} references missing parent race: {}", id, parentId);
            } else {
                CreRaces.LOGGER.warn("Race {} has malformed parent_race: {}", id, parentStr);
            }
        }
        return current;
    }

    private static JsonObject mergeRaces(JsonObject parent, JsonObject child) {
        JsonObject merged = parent.deepCopy();
        for (java.util.Map.Entry<String, JsonElement> entry : child.entrySet()) {
            String key = entry.getKey();
            JsonElement childVal = entry.getValue();

            if (merged.has(key)) {
                JsonElement parentVal = merged.get(key);
                if (childVal.isJsonObject() && parentVal.isJsonObject()) {
                    // Deep merge objects (Passives or Custom Categories)
                    JsonObject parentObj = parentVal.getAsJsonObject();
                    JsonObject childObj = childVal.getAsJsonObject();
                    for (java.util.Map.Entry<String, JsonElement> childEntry : childObj.entrySet()) {
                        parentObj.add(childEntry.getKey(), childEntry.getValue());
                    }
                } else if (childVal.isJsonArray() && parentVal.isJsonArray()) {
                    // Append for specific lists
                    if (key.endsWith("traits") || key.equals("creraces:starting_abilities")
                            || key.equals("starting_abilities")
                            || key.equals("creraces:starting_items") || key.equals("starting_items")
                            || key.equals("creraces:race_addons") || key.equals("race_addons")
                            || !key.contains(":")) { // Categories append too
                        parentVal.getAsJsonArray().addAll(childVal.getAsJsonArray());
                    } else if (key.equals("creraces:customization") || key.equals("customization")) {
                        // Merge customizations by ID
                        JsonArray parentArray = parentVal.getAsJsonArray();
                        JsonArray childArray = childVal.getAsJsonArray();
                        for (JsonElement childElem : childArray) {
                            if (childElem.isJsonObject()) {
                                JsonObject childObj = childElem.getAsJsonObject();
                                @javax.annotation.Nullable
                                String childId = GsonHelper.getNullableString(childObj, "id", null);
                                if (childId != null) {
                                    boolean found = false;
                                    for (int i = 0; i < parentArray.size(); i++) {
                                        JsonElement pElem = parentArray.get(i);
                                        if (pElem.isJsonObject()) {
                                            JsonObject pObj = pElem.getAsJsonObject();
                                            @javax.annotation.Nullable
                                            String pId = GsonHelper.getNullableString(pObj, "id", null);
                                            if (childId.equals(pId)) {
                                                // Child overrides parent entry
                                                parentArray.set(i, childObj);
                                                found = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!found) {
                                        parentArray.add(childElem);
                                    }
                                } else {
                                    parentArray.add(childElem);
                                }
                            }
                        }
                    } else {
                        merged.add(key, childVal);
                    }

                } else {
                    merged.add(key, childVal);
                }
            } else {
                merged.add(key, childVal);
            }
        }
        return merged;
    }

    private static mc.sayda.creraces.race.Race.Passives parsePassives(JsonObject p) {
        // Support both creraces: prefixed and legacy flat keys
        List<String> immuneToDamageTypes = new ArrayList<>();
        JsonArray immuneArray = p.has("creraces:immune_to_damage") ? p.getAsJsonArray("creraces:immune_to_damage")
                : (p.has("immune_to_damage") ? p.getAsJsonArray("immune_to_damage") : null);
        if (immuneArray != null) {
            for (JsonElement e : immuneArray) {
                immuneToDamageTypes.add(standardizeId(e.getAsString()));
            }
        }

        List<String> negateEffects = new ArrayList<>();
        JsonArray negateArray = p.has("creraces:negate_effects") ? p.getAsJsonArray("creraces:negate_effects")
                : (p.has("negate_effects") ? p.getAsJsonArray("negate_effects") : null);
        if (negateArray != null) {
            for (JsonElement e : negateArray) {
                negateEffects.add(standardizeId(e.getAsString()));
            }
        }

        List<String> hatedBy = new ArrayList<>();
        JsonArray hatedArray = p.has("creraces:hated_by_entities") ? p.getAsJsonArray("creraces:hated_by_entities")
                : (p.has("hated_by_entities") ? p.getAsJsonArray("hated_by_entities") : null);
        if (hatedArray != null) {
            for (JsonElement e : hatedArray) {
                hatedBy.add(standardizeId(e.getAsString()));
            }
        }

        List<String> respectedBy = new ArrayList<>();
        JsonArray respectedArray = p.has("creraces:respected_by_entities")
                ? p.getAsJsonArray("creraces:respected_by_entities")
                : (p.has("respected_by_entities") ? p.getAsJsonArray("respected_by_entities") : null);
        if (respectedArray != null) {
            for (JsonElement e : respectedArray) {
                respectedBy.add(standardizeId(e.getAsString()));
            }
        }

        List<String> defendedBy = new ArrayList<>();
        JsonArray defendedArray = p.has("creraces:defended_by_entities")
                ? p.getAsJsonArray("creraces:defended_by_entities")
                : (p.has("defended_by_entities") ? p.getAsJsonArray("defended_by_entities") : null);
        if (defendedArray != null) {
            for (JsonElement e : defendedArray) {
                defendedBy.add(standardizeId(e.getAsString()));
            }
        }

        // Parse entity spawn data
        mc.sayda.creraces.race.Race.EntitySpawnData spawnOnDeath = null;
        JsonElement spawnElement = p.has("creraces:spawn_on_death") ? p.get("creraces:spawn_on_death")
                : p.get("spawn_on_death");
        if (spawnElement != null && spawnElement.isJsonObject()) {
            JsonObject spawn = spawnElement.getAsJsonObject();
            spawnOnDeath = new mc.sayda.creraces.race.Race.EntitySpawnData(
                    GsonHelper.getAsString(spawn, "entity_type"),
                    GsonHelper.getAsString(spawn, "nbt", "{}"),
                    GsonHelper.getAsInt(spawn, "count", 1));
        }

        // Food & Hunger
        boolean oldCanEatMeat = GsonHelper.getAsBoolean(p, "creraces:can_eat_meat",
                GsonHelper.getAsBoolean(p, "can_eat_meat", true));
        List<String> blockedFoodTypes = new ArrayList<>();
        JsonArray blockedArray = p.has("creraces:blocked_food_types") ? p.getAsJsonArray("creraces:blocked_food_types")
                : (p.has("blocked_food_types") ? p.getAsJsonArray("blocked_food_types") : null);
        if (blockedArray != null) {
            for (JsonElement e : blockedArray) {
                blockedFoodTypes.add(standardizeId(e.getAsString()));
            }
        } else if (!oldCanEatMeat) {
            blockedFoodTypes.add("meat");
        }

        List<String> allowedFoodTypes = new ArrayList<>();
        JsonArray allowedArray = p.has("creraces:allowed_food_types") ? p.getAsJsonArray("creraces:allowed_food_types")
                : (p.has("allowed_food_types") ? p.getAsJsonArray("allowed_food_types") : null);
        if (allowedArray != null) {
            for (JsonElement e : allowedArray) {
                allowedFoodTypes.add(standardizeId(e.getAsString()));
            }
        }

        return new mc.sayda.creraces.race.Race.Passives(
                // Breathing & Environmental
                GsonHelper.getAsBoolean(p, "creraces:can_breathe_underwater",
                        GsonHelper.getAsBoolean(p, "can_breathe_underwater", false)),
                GsonHelper.getAsBoolean(p, "creraces:can_breathe_on_land",
                        GsonHelper.getAsBoolean(p, "can_breathe_on_land", true)),
                GsonHelper.getAsBoolean(p, "creraces:burns_in_sunlight",
                        GsonHelper.getAsBoolean(p, "burns_in_sunlight", false)),
                immuneToDamageTypes,
                negateArray != null ? negateEffects : new ArrayList<>(), // Safety check

                // Vision & Perception
                GsonHelper.getAsBoolean(p, "creraces:night_vision",
                        GsonHelper.getAsBoolean(p, "night_vision", false)),
                GsonHelper.getAsBoolean(p, "creraces:water_vision",
                        GsonHelper.getAsBoolean(p, "water_vision", false)),
                GsonHelper.getAsBoolean(p, "creraces:lava_vision",
                        GsonHelper.getAsBoolean(p, "lava_vision", false)),

                // Movement & Physics
                GsonHelper.getAsBoolean(p, "creraces:can_fly",
                        GsonHelper.getAsBoolean(p, "can_fly", false)),
                mc.sayda.creraces.engine.ScalingValue.fromJson(p, "creraces:liquid_speed_multiplier",
                        p.has("liquid_speed_multiplier")
                                ? mc.sayda.creraces.engine.ScalingValue.fromJson(p, "liquid_speed_multiplier", 1.0)
                                        .base()
                                : 1.0),
                GsonHelper.getAsBoolean(p, "creraces:unaffected_by_water",
                        GsonHelper.getAsBoolean(p, "unaffected_by_water", false)),
                GsonHelper.getAsBoolean(p, "creraces:unaffected_by_lava",
                        GsonHelper.getAsBoolean(p, "unaffected_by_lava", false)),
                GsonHelper.getAsBoolean(p, "creraces:cannot_sprint",
                        GsonHelper.getAsBoolean(p, "cannot_sprint", false)),

                // Health & Regeneration
                GsonHelper.getAsBoolean(p, "creraces:no_natural_regeneration",
                        GsonHelper.getAsBoolean(p, "no_natural_regeneration", false)),
                mc.sayda.creraces.engine.ScalingValue.fromJson(p, "creraces:regeneration_multiplier",
                        p.has("regeneration_multiplier")
                                ? mc.sayda.creraces.engine.ScalingValue.fromJson(p, "regeneration_multiplier", 1.0)
                                        .base()
                                : 1.0),

                // Combat & Damage
                GsonHelper.getAsBoolean(p, "creraces:immune_to_knockback",
                        GsonHelper.getAsBoolean(p, "immune_to_knockback", false)),
                mc.sayda.creraces.engine.ScalingValue.fromJson(p, "creraces:invulnerability_ticks_multiplier",
                        p.has("invulnerability_ticks_multiplier")
                                ? mc.sayda.creraces.engine.ScalingValue
                                        .fromJson(p, "invulnerability_ticks_multiplier", 1.0).base()
                                : 1.0),

                // Food & Hunger
                GsonHelper.getAsBoolean(p, "creraces:no_hunger",
                        GsonHelper.getAsBoolean(p, "no_hunger", false)),
                GsonHelper.getAsBoolean(p, "creraces:no_hunger_drain",
                        GsonHelper.getAsBoolean(p, "no_hunger_drain", false)),
                mc.sayda.creraces.engine.ScalingValue.fromJson(p, "creraces:fixed_hunger",
                        p.has("fixed_hunger")
                                ? mc.sayda.creraces.engine.ScalingValue.fromJson(p, "fixed_hunger", 0.0)
                                        .base()
                                : 0.0),
                blockedFoodTypes,
                allowedFoodTypes,
                GsonHelper.getAsBoolean(p, "creraces:can_eat_when_full",
                        GsonHelper.getAsBoolean(p, "can_eat_when_full", false)),

                // Social & Interaction
                hatedBy,
                respectedBy,
                defendedBy,

                // Special Mechanics
                spawnOnDeath,
                GsonHelper.getAsBoolean(p, "creraces:can_command_socials",
                        GsonHelper.getAsBoolean(p, "can_command_socials", false)));
    }

    private static String standardizeId(String id) {
        if (id == null || id.isEmpty())
            return "";
        // Don't standardize special keywords
        if (id.equalsIgnoreCase("meat"))
            return "meat";

        // If it starts with #, it's a tag
        if (id.startsWith("#")) {
            String content = id.substring(1);
            return "#" + (content.contains(":") ? content : "minecraft:" + content);
        }
        return id.contains(":") ? id : "minecraft:" + id;
    }
}
