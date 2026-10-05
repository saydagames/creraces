package mc.sayda.creraces.ability;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.capability.IPlayerVariables;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A HUD bar tracking a cooldown or persistent state value, declared inline on an action with
 * "show_bar": true.
 */
public record OverlayBar(String sourceType, ResourceLocation sourceId, int max, int color, String label) {
    private static final int DEFAULT_COLOR = 0xFFAAAAAA;

    public double getValue(IPlayerVariables vars) {
        return switch (sourceType) {
            case "cooldown" -> vars.getCooldown(sourceId);
            case "state" -> vars.getPersistentState(sourceId);
            default -> 0;
        };
    }

    public static List<OverlayBar> collectOverlayBarsResult(JsonElement element) {
        List<OverlayBar> bars = new ArrayList<>();
        collectOverlayBars(element, bars);
        return bars;
    }

    /** Walks the whole JSON tree, so bars are found however deeply their action is nested. */
    public static void collectOverlayBars(JsonElement element, List<OverlayBar> bars) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("show_bar") && obj.get("show_bar").getAsBoolean()) {
                String type = obj.has("type") ? obj.get("type").getAsString() : "";
                int color = parseColor(obj, "bar_color");
                try {
                    OverlayBar bar = fromAction(obj, type, color);
                    if (bar != null) {
                        bars.add(bar);
                    }
                } catch (Exception ex) {
                    // Malformed bar fields just mean no bar.
                }
            }
            for (var field : obj.entrySet()) {
                collectOverlayBars(field.getValue(), bars);
            }
        } else if (element.isJsonArray()) {
            for (var child : element.getAsJsonArray()) {
                collectOverlayBars(child, bars);
            }
        }
    }

    @Nullable
    private static OverlayBar fromAction(JsonObject obj, String type, int color) {
        if (type.equals("creraces:set_cooldown") && obj.has("id") && obj.has("value")) {
            ResourceLocation sourceId = ResourceLocation.tryParse(obj.get("id").getAsString());
            if (sourceId == null)
                return null;
            String label = obj.has("label") ? obj.get("label").getAsString() : sourceId.getPath();
            return new OverlayBar("cooldown", sourceId, obj.get("value").getAsInt(), color, label);
        }

        if (type.equals("creraces:flight") && obj.has("exhaustion_cooldown") && obj.has("exhaustion_duration")) {
            ResourceLocation sourceId = ResourceLocation.tryParse(obj.get("exhaustion_cooldown").getAsString());
            if (sourceId == null)
                return null;
            String label = obj.has("label") ? obj.get("label").getAsString() : sourceId.getPath();
            return new OverlayBar("cooldown", sourceId, obj.get("exhaustion_duration").getAsInt(), color, label);
        }

        if ((type.equals("creraces:modify_resource") || type.equals("creraces:modify_value"))
                && obj.has("resource") && obj.has("bar_max")) {
            String resource = obj.get("resource").getAsString();
            int max = obj.get("bar_max").getAsInt();
            String label = obj.has("label") ? obj.get("label").getAsString() : resource;

            // modify_resource uses "prefix:key" encoding; modify_value uses "state:key" or a plain name.
            if (resource.startsWith("state:")) {
                String key = resource.substring("state:".length());
                if (!key.contains(":"))
                    key = "creraces:" + key;
                ResourceLocation sourceId = ResourceLocation.tryParse(key);
                return sourceId != null ? new OverlayBar("state", sourceId, max, color, label) : null;
            }
            int colonIdx = resource.indexOf(':');
            if (colonIdx > 0) {
                ResourceLocation sourceId = ResourceLocation.tryParse(resource.substring(colonIdx + 1));
                return sourceId != null
                        ? new OverlayBar(resource.substring(0, colonIdx), sourceId, max, color, label)
                        : null;
            }
            // Plain named resources (mana, energy, ...) get no overlay bar.
        }
        return null;
    }

    /** Accepts an int, or a "0xRRGGBB" / "#RRGGBB" string (alpha optional, defaulting to opaque). */
    public static int parseColor(JsonObject json, String key) {
        if (!json.has(key))
            return DEFAULT_COLOR;
        JsonElement elem = json.get(key);
        if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
            String s = elem.getAsString();
            String hex = null;
            if (s.startsWith("0x") || s.startsWith("0X")) {
                hex = s.substring(2);
            } else if (s.startsWith("#")) {
                hex = s.substring(1);
            }
            if (hex != null) {
                if (hex.length() == 6)
                    hex = "FF" + hex;
                return (int) Long.parseLong(hex, 16);
            }
        }
        return elem.getAsInt();
    }
}
