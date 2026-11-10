package mc.sayda.creraces.race;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A customization option for a race (tails, colors, ...): an "id", a "type" ("property", or "tint"
 * for a "#RRGGBB" value that tints its addon), the "options", a "default" and "hidden". "addon" is
 * an addon always shown with it, and "addons" maps each value to the addons it shows. Per-race
 * defaults come from the race's creraces:race_defaults.
 */
public class RaceCustomization {
    private final String id;
    private final String type;
    private final String addonId;
    private final JsonObject addonData;
    private final List<String> options;
    private final String defaultValue;
    private final Map<String, String> raceDefaults;
    private final boolean hidden;

    public RaceCustomization(@Nonnull String id, @Nonnull String type, String addonId, JsonObject addonData,
            @Nonnull List<String> options, @Nonnull String defaultValue,
            Map<String, String> raceDefaults, boolean hidden) {
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

    public List<String> options() {
        return options;
    }

    public String defaultValue() {
        return defaultValue;
    }

    /** The race-specific default when one is configured, otherwise the general default. */
    public String getDefaultValue(ResourceLocation raceId) {
        if (raceId != null && raceDefaults.containsKey(raceId.toString())) {
            return raceDefaults.get(raceId.toString());
        }
        return defaultValue;
    }

    public boolean hidden() {
        return hidden;
    }

    public static RaceCustomization fromJson(JsonObject json) {
        return fromJson(json, null);
    }

    public static RaceCustomization fromJson(JsonObject json, @Nullable Map<String, String> externalDefaults) {
        List<String> options = new ArrayList<>();
        if (json.has("options")) {
            json.getAsJsonArray("options").forEach(e -> options.add(e.getAsString()));
        }

        Map<String, String> raceDefaults = new HashMap<>();
        if (externalDefaults != null) {
            raceDefaults.putAll(externalDefaults);
        }

        String type = GsonHelper.getAsString(json, "type", "property");
        if (!type.equals("property") && !type.equals("tint")) {
            CreRaces.LOGGER.error("Unknown customization type '{}' (property, tint) in {}", type, json);
        }
        return new RaceCustomization(
                GsonHelper.getAsString(json, "id", "unknown"),
                type,
                GsonHelper.getNullableString(json, "addon", null),
                json.has("addons") ? json.getAsJsonObject("addons") : null,
                options,
                GsonHelper.getAsString(json, "default", ""),
                raceDefaults,
                GsonHelper.getAsBoolean(json, "hidden", false));
    }
}
