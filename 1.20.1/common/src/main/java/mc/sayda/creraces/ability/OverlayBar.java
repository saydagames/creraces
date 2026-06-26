package mc.sayda.creraces.ability;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public record OverlayBar(String sourceType, ResourceLocation sourceId, int max, int color, String label) {

    public double getValue(mc.sayda.creraces.capability.IPlayerVariables vars) {
        return switch (sourceType) {
            case "cooldown" -> vars.getCooldown(sourceId);
            case "state" -> vars.getPersistentState(sourceId);
            default -> 0;
        };
    }

    public static java.util.List<OverlayBar> collectOverlayBarsResult(com.google.gson.JsonElement element) {
        java.util.List<OverlayBar> bars = new java.util.ArrayList<>();
        collectOverlayBars(element, bars);
        return bars;
    }

    public static void collectOverlayBars(com.google.gson.JsonElement element, java.util.List<OverlayBar> bars) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("show_bar") && obj.get("show_bar").getAsBoolean()) {
                String type = obj.has("type") ? obj.get("type").getAsString() : "";
                int color = parseColor(obj, "bar_color");
                try {
                    if (type.equals("creraces:set_cooldown") && obj.has("id") && obj.has("value")) {
                        ResourceLocation sourceId = ResourceLocation.tryParse(obj.get("id").getAsString());
                        int max = obj.get("value").getAsInt();
                        String label = sourceId != null ? sourceId.getPath() : "";
                        if (obj.has("name")) label = obj.get("name").getAsString();
                        if (sourceId != null) bars.add(new OverlayBar("cooldown", sourceId, max, color, label));
                    } else if ((type.equals("creraces:modify_resource") || type.equals("creraces:modify_value"))
                            && obj.has("resource") && obj.has("bar_max")) {
                        String resource = obj.get("resource").getAsString();
                        int max = obj.get("bar_max").getAsInt();
                        String label = resource;
                        if (obj.has("name")) label = obj.get("name").getAsString();

                        // modify_resource uses "prefix:key" encoding; modify_value uses "state:key" or a plain name.
                        String sourceType;
                        ResourceLocation sourceId;
                        if (resource.startsWith("state:")) {
                            String key = resource.substring(6);
                            if (!key.contains(":")) key = "creraces:" + key;
                            sourceType = "state";
                            sourceId = ResourceLocation.tryParse(key);
                        } else {
                            int colonIdx = resource.indexOf(':');
                            if (colonIdx > 0) {
                                sourceType = resource.substring(0, colonIdx);
                                sourceId = ResourceLocation.tryParse(resource.substring(colonIdx + 1));
                            } else {
                                // Plain named resource (mana, energy, etc.) — no RL needed; skip bar.
                                sourceType = null;
                                sourceId = null;
                            }
                        }
                        if (sourceType != null && sourceId != null) {
                            bars.add(new OverlayBar(sourceType, sourceId, max, color, label));
                        }
                    }
                } catch (Exception ex) { /* skip malformed */ }
            }
            for (var field : obj.entrySet()) collectOverlayBars(field.getValue(), bars);
        } else if (element.isJsonArray()) {
            for (var child : element.getAsJsonArray()) collectOverlayBars(child, bars);
        }
    }

    public static int parseColor(JsonObject json, String key) {
        if (!json.has(key)) return 0xFFAAAAAA;
        var elem = json.get(key);
        if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
            String s = elem.getAsString();
            if (s.startsWith("0x") || s.startsWith("0X")) {
                String hex = s.substring(2);
                if (hex.length() == 6) hex = "FF" + hex;
                return (int) Long.parseLong(hex, 16);
            }
            if (s.startsWith("#")) {
                String hex = s.substring(1);
                if (hex.length() == 6) hex = "FF" + hex;
                return (int) Long.parseLong(hex, 16);
            }
        }
        return elem.getAsInt();
    }
}
