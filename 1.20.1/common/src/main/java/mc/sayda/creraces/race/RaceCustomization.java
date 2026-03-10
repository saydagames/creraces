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
        private final List<String> options;
        private final String defaultValue;
        private final boolean hidden;

        public RaceCustomization(@Nonnull String id, @Nonnull String type, String addonId, JsonObject addonData,
                        @Nonnull List<String> options, @Nonnull String defaultValue, boolean hidden) {
                this.id = id;
                this.type = type;
                this.addonId = addonId;
                this.addonData = addonData;
                this.options = options;
                this.defaultValue = defaultValue;
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

        public List<String> options() {
                return options;
        }

        public String defaultValue() {
                return defaultValue;
        }

        public boolean hidden() {
                return hidden;
        }

        public static RaceCustomization fromJson(JsonObject json) {
                List<String> options = new ArrayList<>();
                if (json.has("options")) {
                        json.getAsJsonArray("options").forEach(e -> options.add(e.getAsString()));
                }

                return new RaceCustomization(
                                GsonHelper.getAsString(json, "id", "unknown"),
                                GsonHelper.getAsString(json, "type", "property"),
                                GsonHelper.getAsString(json, "addonId", null),
                                json.has("addon_data") ? json.getAsJsonObject("addon_data")
                                                : (json.has("addons") ? json.getAsJsonObject("addons") : null), // support
                                                                                                                // both
                                                                                                                // keys
                                options,
                                json.has("defaultValue") ? json.get("defaultValue").getAsString()
                                                : (json.has("default") ? json.get("default").getAsString() : ""),
                                GsonHelper.getAsBoolean(json, "hidden", false));
        }
}
