package mc.sayda.creraces.race;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import mc.sayda.twilight_lib.capabilities.IAddons;
import mc.sayda.twilight_lib.capabilities.AddonsData;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import mc.sayda.creraces.engine.traits.AddonTrait;

/**
 * Bridges CreRaces racial customizations with Twilight Lib's cosmetic system.
 */
public class CosmeticIncidents {

    public static void applyCustomizations(net.minecraft.world.entity.player.Player player, Map<String, String> custMap,
            Race race) {
        if (race == null)
            return;

        IAddons addonsRaw = DataUtils.getAddonsData(player);
        if (!(addonsRaw instanceof AddonsData addons))
            return;

        // 1. Clear ALL existing racial addons
        clearRacialAddons(addons);

        // 2. Integrated Application (Auto-apply from Customization definitions)
        for (RaceCustomization cust : race.customization()) {
            String value = custMap.getOrDefault(cust.id(), cust.defaultValue());

            // Handle addon mapping (Integrated Cosmetics)
            if (cust.addonData() != null) {
                applyAddonData(player, cust.addonData(), value, custMap, addons);
            }

            // Handle simple addonId (legacy/fixed)
            if (cust.addonId() != null) {
                addons.setActiveAddon(cust.addonId(), true, true);

                // If the type is 'tint', apply it as a color to this fixed addon
                if (cust.type().equals("tint")) {
                    addons.setAddonTint(cust.addonId(), parseHex(value, 0xFFFFFF));
                }
            }

            // If it's a tint customization, we can't auto-apply it to "nothing".
            // It will be picked up by resolveColor in traits or mapping data.
        }

        // 3. Trait Application (Still used for fixed/complex logic)
        for (var trait : race.traits()) {
            if (trait instanceof AddonTrait addonTrait) {
                var condition = addonTrait.getCondition();
                if (condition != null && !condition.evaluate(player, null, null, null)) {
                    continue;
                }

                String addonId = resolvePlaceholders(addonTrait.getAddonId(), custMap);
                addons.setActiveAddon(addonId, true, true);

                if (addonTrait.getTint() != null && !addonTrait.getTint().isEmpty()) {
                    int tintColor = resolveColor(addonTrait.getTint(), custMap);
                    addons.setAddonTint(addonId, tintColor);
                }
            }
        }
    }

    private static void applyAddonData(net.minecraft.world.entity.player.Player player, JsonObject data, String value,
            Map<String, String> custMap, AddonsData addons) {
        // Can be a direct map: { "standard": "id", "alt": "id" }
        // Or a complex map: { "standard": { "id": "id", "tint": "{color}" } }
        // Or a list (Iterate until first match): { "alt": [ { "id": "alt_id",
        // "condition": {...} }, "standard_id" ] }
        if (data.has(value)) {
            JsonElement element = data.get(value);
            if (element.isJsonArray()) {
                // List of possible mappings
                for (JsonElement e : element.getAsJsonArray()) {
                    if (applyPossibleAddon(player, e, custMap, addons)) {
                        // If the entry has a condition, treat the whole list as a fallback chain and
                        // stop here.
                        // If it's a simple string or object WITHOUT a condition, assume it's a bundle
                        // and keep applying.
                        if (e.isJsonObject() && e.getAsJsonObject().has("condition")) {
                            break;
                        }
                    }
                }
            } else {
                applyPossibleAddon(player, element, custMap, addons);
            }
        }

        // Support for "pattern": "prefix_{self}_{color}"
        if (data.has("pattern")) {
            String pattern = data.get("pattern").getAsString();
            String addonId = pattern.replace("{self}", value);
            addonId = resolvePlaceholders(addonId, custMap);
            addons.setActiveAddon(addonId, true, true);
        }
    }

    private static boolean applyPossibleAddon(net.minecraft.world.entity.player.Player player, JsonElement element,
            Map<String, String> custMap, AddonsData addons) {
        if (element.isJsonPrimitive()) {
            addons.setActiveAddon(resolvePlaceholders(element.getAsString(), custMap), true, true);
            return true;
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            // Check condition if present
            if (obj.has("condition")) {
                mc.sayda.creraces.engine.condition.Condition cond = mc.sayda.creraces.engine.condition.Condition
                        .fromJson(obj.getAsJsonObject("condition"));
                if (!cond.evaluate(player, null, null, null)) {
                    return false;
                }
            }

            if (obj.has("id")) {
                String id = resolvePlaceholders(obj.get("id").getAsString(), custMap);
                addons.setActiveAddon(id, true, true);
                if (obj.has("tint")) {
                    int tint = resolveColor(obj.get("tint").getAsString(), custMap);
                    addons.setAddonTint(id, tint);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Public method to clear all racial addons from a player.
     * Used during race reset.
     */
    public static void clearAllRacialAddons(net.minecraft.world.entity.player.Player player) {
        IAddons addonsRaw = DataUtils.getAddonsData(player);
        if (addonsRaw instanceof AddonsData addons) {
            clearRacialAddons(addons);
        }
    }

    private static void clearRacialAddons(AddonsData addons) {
        Set<String> toRemove = new HashSet<>();
        for (String active : addons.getActiveAddons()) {
            if (active.startsWith("twilight_lib:")) {
                toRemove.add(active);
            }
        }
        for (String id : toRemove) {
            addons.forceUnequipAddon(id);
        }
    }

    public static String resolvePlaceholders(String template, Map<String, String> custMap) {
        String result = template;
        for (Map.Entry<String, String> entry : custMap.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private static int resolveColor(String colorSource, Map<String, String> custMap) {
        if (colorSource.startsWith("{") && colorSource.endsWith("}")) {
            String key = colorSource.substring(1, colorSource.length() - 1);
            String value = custMap.getOrDefault(key, "#FFFFFF");
            return parseHex(value, 0xFFFFFF);
        }
        return parseHex(colorSource, 0xFFFFFF);
    }

    private static int parseHex(String hex, int def) {
        try {
            if (hex.startsWith("#")) {
                return Integer.parseInt(hex.substring(1), 16);
            }
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
