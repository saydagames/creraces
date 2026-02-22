package mc.sayda.creraces.race;

import com.google.gson.JsonObject;
import mc.sayda.creraces.util.GsonHelper;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a customization option for a race (e.g. tails, colors).
 * Defined in the "customization" array of a race JSON.
 */
public record RaceCustomization(
                @Nonnull String id,
                @Nonnull String type, // "addon", "property", "tint"
                String addonId, // e.g. "twilight-lib:kitsune_tails" (optional, for type "addon")
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
                                options,
                                json.has("defaultValue") ? json.get("defaultValue").getAsString()
                                                : (json.has("default") ? json.get("default").getAsString() : ""),
                                GsonHelper.getAsBoolean(json, "hidden", false));
        }
}
