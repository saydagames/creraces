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
public record RaceCustomization(
                @Nonnull String id,
                @Nonnull String type, // "addon", "property", "tint"
                String addonId, // simple/legacy ID
                JsonObject addonData, // Raw mapping/pattern data for integrated cosmetics
                @Nonnull List<String> options,
                @Nonnull String defaultValue,
                boolean hidden) {

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
