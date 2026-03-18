package mc.sayda.creraces.race;

import com.google.gson.JsonObject;
import mc.sayda.creraces.util.GsonHelper;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a customization option for a race (e.g. tails, colors).
 * Integrated version: Can carry raw JSON data for automatic cosmetic
 * application.
 */
public class RaceCustomization {
        private final String id;
        private final String type;
        private final String addonId;
        private final JsonObject addonData;
        private final java.util.List<String> options;
        private final String defaultValue;
        private final java.util.Map<String, String> raceDefaults;
        private final boolean hidden;

        public RaceCustomization(@Nonnull String id, @Nonnull String type, String addonId, JsonObject addonData,
                        @Nonnull java.util.List<String> options, @Nonnull String defaultValue,
                        java.util.Map<String, String> raceDefaults, boolean hidden) {
                this.id = id;
                this.type = type;
                this.addonId = addonId;
                this.addonData = addonData;
                this.options = options;
                this.defaultValue = defaultValue;
                this.raceDefaults = raceDefaults;
                this.hidden = hidden;
        }

        public String id() {
                return id;
        }

        public String type() {
                return type;
        }

        public String addonId() {
                return addonId;
        }

        public JsonObject addonData() {
                return addonData;
        }

        public java.util.List<String> options() {
                return options;
        }

        public String defaultValue() {
                return defaultValue;
        }

        public String getDefaultValue(net.minecraft.resources.ResourceLocation raceId) {
                if (raceId != null && raceDefaults.containsKey(raceId.toString())) {
                        return raceDefaults.get(raceId.toString());
                }
                return defaultValue;
        }

        public boolean hidden() {
                return hidden;
        }

        public static RaceCustomization fromJson(JsonObject json) {
                java.util.List<String> options = new java.util.ArrayList<>();
                if (json.has("options")) {
                        json.getAsJsonArray("options").forEach(e -> options.add(e.getAsString()));
                }

                java.util.Map<String, String> raceDefaults = new java.util.HashMap<>();
                if (json.has("creraces:race_defaults") || json.has("race_defaults")) {
                        JsonObject rdObj = json.has("creraces:race_defaults") ? json.getAsJsonObject("creraces:race_defaults")
                                        : json.getAsJsonObject("race_defaults");
                        for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : rdObj.entrySet()) {
                                raceDefaults.put(entry.getKey(), entry.getValue().getAsString());
                        }
                }

                return new RaceCustomization(
                                GsonHelper.getAsString(json, "id", "unknown"),
                                GsonHelper.getAsString(json, "type", "property"),
                                GsonHelper.getNullableString(json, "addonId", null),
                                json.has("addon_data") ? json.getAsJsonObject("addon_data")
                                                : (json.has("addons") ? json.getAsJsonObject("addons") : null), // support
                                                                                                                // both
                                                                                                                // keys
                                options,
                                json.has("defaultValue") ? json.get("defaultValue").getAsString()
                                                : (json.has("default") ? json.get("default").getAsString() : ""),
                                raceDefaults,
                                GsonHelper.getAsBoolean(json, "hidden", false));
        }

}
