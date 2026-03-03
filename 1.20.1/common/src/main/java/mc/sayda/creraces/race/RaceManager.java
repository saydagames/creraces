package mc.sayda.creraces.race;

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
import java.util.Objects;

/**
 * Loads race JSONs from data/creraces/races/
 */
@SuppressWarnings("unchecked")
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
                    // We need an instance to call resolveInheritance (or make it static)
                    RaceManager manager = new RaceManager();
                    resolvedData.put(id, manager.resolveInheritance(id, data, new java.util.HashSet<>()));
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
                String nameStr = GsonHelper.getAsString(jsonObject, "name", id.getPath());
                String descStr = GsonHelper.getAsString(jsonObject, "description", "");

                // Portraits (Optional, default to barrier if missing)
                ResourceLocation icon = new ResourceLocation(
                        Objects.requireNonNull(
                                GsonHelper.getAsString(jsonObject, "icon", "minecraft:textures/item/barrier.png")));
                ResourceLocation portrait = new ResourceLocation(
                        Objects.requireNonNull(
                                GsonHelper.getAsString(jsonObject, "portrait", "creraces:textures/screens/race.png")));
                ResourceLocation splash = new ResourceLocation(
                        Objects.requireNonNull(GsonHelper.getAsString(jsonObject, "splash",
                                "creraces:textures/screens/unknown_splash.png")));

                String nameTextureStr = GsonHelper.getAsString(jsonObject, "name_texture", null);
                String parentRaceStr = GsonHelper.getAsString(jsonObject, "parent_race", null);
                ResourceLocation parentRace = parentRaceStr != null ? new ResourceLocation(parentRaceStr) : null;

                double legacyId = GsonHelper.getAsDouble(jsonObject, "legacy_id", 0.0);

                int baseAp = GsonHelper.getAsInt(jsonObject, "base_ap", 0);
                int baseAd = GsonHelper.getAsInt(jsonObject, "base_ad", 0);
                int baseAh = GsonHelper.getAsInt(jsonObject, "base_ah", 0);
                int baseCr = GsonHelper.getAsInt(jsonObject, "base_cr", 0);
                int maxResource = GsonHelper.getAsInt(jsonObject, "max_resource", 100);
                RaceScale scale = RaceScale.fromJson(jsonObject.get("scale"));
                String resourceTypeStr = GsonHelper.getAsString(jsonObject, "resource_type", "NONE");
                ResourceType resourceType = ResourceType.valueOf(resourceTypeStr.toUpperCase());

                String tierStr = GsonHelper.getAsString(jsonObject, "tier", "creraces:common");
                int difficulty = GsonHelper.getAsInt(jsonObject, "difficulty", 0);

                // Background texture (Optional)
                String bgTexStr = GsonHelper.getAsString(jsonObject, "bg_texture",
                        "creraces:textures/screens/selection_bg.png");

                // Splash dimensions
                int splashX = GsonHelper.getAsInt(jsonObject, "splash_x", 15);
                int splashY = GsonHelper.getAsInt(jsonObject, "splash_y", -10);
                int splashW = GsonHelper.getAsInt(jsonObject, "splash_w", 141);
                int splashH = GsonHelper.getAsInt(jsonObject, "splash_h", 199);

                // Name Dimensions
                int nameTexX = GsonHelper.getAsInt(jsonObject, "name_tex_x", 44);
                int nameTexY = GsonHelper.getAsInt(jsonObject, "name_tex_y", -35);
                int nameTexW = GsonHelper.getAsInt(jsonObject, "name_tex_w", 86);
                int nameTexH = GsonHelper.getAsInt(jsonObject, "name_tex_h", 20);

                // Customizations
                List<RaceCustomization> customizations = new ArrayList<>();
                if (jsonObject.has("customization")) {
                    for (JsonElement custElem : jsonObject.getAsJsonArray("customization")) {
                        customizations.add(RaceCustomization.fromJson(custElem.getAsJsonObject()));
                    }
                }

                // Starting Items/Abilities
                List<ResourceLocation> startingAbilities = new ArrayList<>();
                if (jsonObject.has("starting_abilities")) {
                    for (JsonElement e : jsonObject.getAsJsonArray("starting_abilities")) {
                        startingAbilities.add(new ResourceLocation(e.getAsString()));
                    }
                }

                List<ResourceLocation> startingItems = new ArrayList<>();
                if (jsonObject.has("starting_items")) {
                    for (JsonElement e : jsonObject.getAsJsonArray("starting_items")) {
                        startingItems.add(new ResourceLocation(e.getAsString()));
                    }
                }

                // Traits
                List<mc.sayda.creraces.engine.TraitRegistry.RaceTrait> traits = new ArrayList<>();
                if (jsonObject.has("traits")) {
                    for (JsonElement e : jsonObject.getAsJsonArray("traits")) {
                        traits.add(mc.sayda.creraces.engine.TraitRegistry.fromJson(e.getAsJsonObject()));
                    }
                }

                // Remote Documentation
                if (jsonObject.has("remote_description")) {
                    mc.sayda.creraces.util.RemoteDocConfig remoteConfig = mc.sayda.creraces.util.RemoteDocConfig
                            .fromJson(jsonObject.getAsJsonObject("remote_description"));
                    if (remoteConfig != null) {
                        RaceRegistry.registerRemoteDoc(id, remoteConfig);
                    }
                }

                if (jsonObject.has("remote_passive")) {
                    mc.sayda.creraces.util.RemoteDocConfig remoteConfig = mc.sayda.creraces.util.RemoteDocConfig
                            .fromJson(jsonObject.getAsJsonObject("remote_passive"));
                    if (remoteConfig != null) {
                        RaceRegistry.registerRemotePassive(id, remoteConfig);
                    }
                }

                Race race = new Race(
                        id,
                        Component.translatable(nameStr),
                        Component.translatable(descStr),
                        icon,
                        portrait,
                        splash,
                        nameTextureStr != null ? new ResourceLocation(nameTextureStr) : null,
                        parentRace,
                        legacyId,
                        baseAp,
                        baseAd,
                        baseAh,
                        baseCr,
                        scale,
                        maxResource,
                        resourceType,
                        new ResourceLocation(bgTexStr),
                        new ResourceLocation(tierStr),
                        difficulty,
                        splashX, splashY, splashW, splashH,
                        nameTexX, nameTexY, nameTexW, nameTexH,
                        customizations,
                        startingAbilities,
                        startingItems,
                        parsePassives(jsonObject),
                        traits,
                        GsonHelper.getAsBoolean(jsonObject, "is_spirit", false),
                        GsonHelper.getAsBoolean(jsonObject, "is_tiny", false),
                        GsonHelper.getAsBoolean(jsonObject, "stacks_affect_resource", false));

                RaceRegistry.register(race);
                count[0]++;
            } catch (Exception e) {
                CreRaces.LOGGER.error("Failed to load race {}: {}", id, e);
            }
        });

        CreRaces.LOGGER.info("Loaded {} races.", count[0]);
    }

    private JsonObject resolveInheritance(ResourceLocation id, Map<ResourceLocation, JsonElement> data,
            java.util.Set<ResourceLocation> visited) {
        if (!visited.add(id)) {
            return data.get(id).getAsJsonObject().deepCopy(); // Circular dependency
        }

        JsonObject current = data.get(id).getAsJsonObject().deepCopy();
        if (current.has("parent_race")) {
            String parentStr = current.get("parent_race").getAsString();
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

    private JsonObject mergeRaces(JsonObject parent, JsonObject child) {
        JsonObject merged = parent.deepCopy();
        for (java.util.Map.Entry<String, JsonElement> entry : child.entrySet()) {
            String key = entry.getKey();
            JsonElement childVal = entry.getValue();

            if (merged.has(key)) {
                JsonElement parentVal = merged.get(key);
                if (childVal.isJsonObject() && parentVal.isJsonObject()) {
                    // Passives or attributeModifiers - Merge fields
                    JsonObject parentObj = parentVal.getAsJsonObject();
                    JsonObject childObj = childVal.getAsJsonObject();
                    for (java.util.Map.Entry<String, JsonElement> childEntry : childObj.entrySet()) {
                        parentObj.add(childEntry.getKey(), childEntry.getValue());
                    }
                } else if (childVal.isJsonArray() && parentVal.isJsonArray()) {
                    // Append for specific lists
                    if (key.equals("traits") || key.equals("starting_abilities") || key.equals("starting_items")) {
                        parentVal.getAsJsonArray().addAll(childVal.getAsJsonArray());
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

    private static Race.Passives parsePassives(JsonObject json) {
        if (!json.has("passives")) {
            return Race.Passives.DEFAULT;
        }
        JsonObject p = json.getAsJsonObject("passives");

        // Parse damage type immunity list
        List<String> immuneToDamageTypes = new ArrayList<>();
        if (p.has("immune_to_damage_types")) {
            for (JsonElement e : p.getAsJsonArray("immune_to_damage_types")) {
                immuneToDamageTypes.add(e.getAsString());
            }
        }

        // Parse potion effect immunity list
        List<String> immuneToPotionEffects = new ArrayList<>();
        if (p.has("immune_to_potion_effects")) {
            for (JsonElement e : p.getAsJsonArray("immune_to_potion_effects")) {
                immuneToPotionEffects.add(e.getAsString());
            }
        }

        // Parse social interaction lists
        List<String> hatedByEntities = new ArrayList<>();
        if (p.has("hated_by_entities")) {
            for (JsonElement e : p.getAsJsonArray("hated_by_entities")) {
                hatedByEntities.add(e.getAsString());
            }
        }

        List<String> respectedByEntities = new ArrayList<>();
        if (p.has("respected_by_entities")) {
            for (JsonElement e : p.getAsJsonArray("respected_by_entities")) {
                respectedByEntities.add(e.getAsString());
            }
        }

        List<String> defendedByEntities = new ArrayList<>();
        if (p.has("defended_by_entities")) {
            for (JsonElement e : p.getAsJsonArray("defended_by_entities")) {
                defendedByEntities.add(e.getAsString());
            }
        }

        // Parse entity spawn data
        Race.EntitySpawnData spawnOnDeath = null;
        if (p.has("spawn_on_death")) {
            JsonObject spawn = p.getAsJsonObject("spawn_on_death");
            spawnOnDeath = new Race.EntitySpawnData(
                    GsonHelper.getAsString(spawn, "entity_type"),
                    GsonHelper.getAsString(spawn, "nbt", "{}"),
                    GsonHelper.getAsInt(spawn, "count", 1));
        }

        return new Race.Passives(
                // Breathing & Environmental
                GsonHelper.getAsBoolean(p, "can_breathe_underwater", false),
                GsonHelper.getAsBoolean(p, "can_breathe_on_land", true),
                GsonHelper.getAsBoolean(p, "burns_in_sunlight", false),
                immuneToDamageTypes,
                immuneToPotionEffects,

                // Vision & Perception
                GsonHelper.getAsBoolean(p, "night_vision", false),
                GsonHelper.getAsBoolean(p, "water_vision", false),
                GsonHelper.getAsBoolean(p, "lava_vision", false),

                // Movement & Physics
                mc.sayda.creraces.engine.ScalingValue.fromJson(p, "fall_damage_multiplier", 1.0),
                GsonHelper.getAsBoolean(p, "can_fly", false),
                mc.sayda.creraces.engine.ScalingValue.fromJson(p, "liquid_speed_multiplier", 1.0),
                GsonHelper.getAsBoolean(p, "unaffected_by_water", false),
                GsonHelper.getAsBoolean(p, "unaffected_by_lava", false),
                GsonHelper.getAsBoolean(p, "cannot_sprint", false),

                // Health & Regeneration
                GsonHelper.getAsBoolean(p, "no_natural_regeneration", false),
                mc.sayda.creraces.engine.ScalingValue.fromJson(p, "regeneration_multiplier", 1.0),

                // Combat & Damage
                GsonHelper.getAsBoolean(p, "immune_to_knockback", false),
                mc.sayda.creraces.engine.ScalingValue.fromJson(p, "invulnerability_ticks_multiplier", 1.0),

                // Food & Hunger
                GsonHelper.getAsBoolean(p, "no_hunger", false),
                GsonHelper.getAsBoolean(p, "no_hunger_drain", false),
                mc.sayda.creraces.engine.ScalingValue.fromJson(p, "fixed_hunger", 0.0),
                GsonHelper.getAsBoolean(p, "can_eat_meat", true),
                GsonHelper.getAsBoolean(p, "can_eat_when_full", false),

                // Social & Interaction
                hatedByEntities,
                respectedByEntities,
                defendedByEntities,

                // Equipment Restrictions
                GsonHelper.getAsBoolean(p, "cannot_wear_helmet", false),
                GsonHelper.getAsBoolean(p, "cannot_wear_chestplate", false),
                GsonHelper.getAsBoolean(p, "cannot_wear_leggings", false),
                GsonHelper.getAsBoolean(p, "cannot_wear_boots", false),

                // Special Mechanics
                spawnOnDeath);
    }
}
