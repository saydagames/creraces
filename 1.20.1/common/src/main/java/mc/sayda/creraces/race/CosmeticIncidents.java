package mc.sayda.creraces.race;

import mc.sayda.twilight_lib.capabilities.DataUtils;
import mc.sayda.twilight_lib.capabilities.IAddons;
import mc.sayda.twilight_lib.capabilities.AddonsData;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

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

        // Clear ALL existing racial addons (fixes Harpy addon clearing bug)
        clearRacialAddons(addons);

        String raceId = race.id().getPath();

        // Apply addons defined in race traits
        for (var trait : race.traits()) {
            if (trait instanceof mc.sayda.creraces.engine.traits.AddonTrait addonTrait) {
                String addonId = addonTrait.getAddonId();
                String tintSource = addonTrait.getTint();

                // Resolve addon ID with customization placeholders
                addonId = resolvePlaceholders(addonId, custMap);

                // Apply addon
                addons.setActiveAddon(addonId, true, true);

                // Apply tint if specified
                if (tintSource != null && !tintSource.isEmpty()) {
                    int tintColor = resolveColor(tintSource, custMap);
                    addons.setAddonTint(addonId, tintColor);
                }
            }
        }

        // LEGACY: Keep hardcoded logic for now (will be removed after full JSON
        // migration)
        // Kitsune Logic
        if ("kitsune".equals(raceId)) {
            String color = custMap.getOrDefault("color", "white");
            // Sanity check: If it's a hex code, force back to a named color for
            // ResourceLocation
            if (color.startsWith("#")) {
                color = "white";
            }

            String earStyle = custMap.getOrDefault("ear_style", "standard");
            boolean hasSnout = "true".equals(custMap.getOrDefault("snout", "true"));
            boolean altTails = "alt".equals(custMap.getOrDefault("tail_style", "standard"));

            // Get tail count from stats, fallback to 1
            int tailCountVal = 1;
            try {
                if (custMap.containsKey("tail_count")) {
                    tailCountVal = Integer.parseInt(custMap.get("tail_count"));
                }
            } catch (Exception e) {
            }

            // Ear Style (Standard/Alt)
            String earId = "twilight_lib:kitsune_ears_" + color + ("alt".equals(earStyle) ? "_alt" : "");
            addons.setActiveAddon(earId, true, true);

            if (hasSnout) {
                addons.setActiveAddon("twilight_lib:kitsune_snout_" + color, true, true);
            }

            // Alt tails only exist for counts 3-5 in twilight-lib
            boolean supportsTailAlt = tailCountVal >= 3 && tailCountVal <= 5;

            String tailId = "twilight_lib:kitsune_tails_" + tailCountVal + "_" + color
                    + (altTails && supportsTailAlt ? "_alt" : "");
            addons.setActiveAddon(tailId, true, true);
        }

        // Harpy Logic
        if ("harpy".equals(raceId)) {
            String fColorStr = custMap.getOrDefault("feather_color", "#FFFFFF");
            String lColorStr = custMap.getOrDefault("leg_color", "#FFFF00");
            boolean altLegs = "alt".equals(custMap.getOrDefault("leg_style", "standard"));

            int fColor = parseHex(fColorStr, 0xFFFFFF);
            int lColor = parseHex(lColorStr, 0xFFFF00);

            // Addons
            String wingsId = "twilight_lib:harpy_wings";
            String thighsStandard = "twilight_lib:harpy_thighs";
            String thighsAlt = "twilight_lib:harpy_thighs_alt";
            String legsStandard = "twilight_lib:harpy_legs";
            String legsAlt = "twilight_lib:harpy_legs_alt";

            // Wings always active
            addons.setActiveAddon(wingsId, true, true);
            addons.setAddonTint(wingsId, fColor);

            // Legs/Thighs (Toggle)
            if (altLegs) {
                addons.setActiveAddon(legsAlt, true, true);
                addons.setAddonTint(legsAlt, lColor);
                addons.forceUnequipAddon(legsStandard);
                addons.setActiveAddon(thighsAlt, true, true);
                addons.setAddonTint(thighsAlt, fColor);
                addons.forceUnequipAddon(thighsStandard);
            } else {
                addons.setActiveAddon(legsStandard, true, true);
                addons.setAddonTint(legsStandard, lColor);
                addons.forceUnequipAddon(legsAlt);
                addons.setActiveAddon(thighsStandard, true, true);
                addons.setAddonTint(thighsStandard, fColor);
                addons.forceUnequipAddon(thighsAlt);
            }

            // Short Torso (Permanent)
            addons.setActiveAddon("twilight_lib:short_torso", true, true);
        }
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

    /**
     * Clears all racial addons (kitsune, harpy, etc.) from the player.
     * This fixes the bug where Harpy addons weren't being removed on race change.
     */
    private static void clearRacialAddons(AddonsData addons) {
        Set<String> toRemove = new HashSet<>();
        for (String active : addons.getActiveAddons()) {
            // Remove all twilight_lib addons that are racial (kitsune, harpy, etc.)
            if (active.startsWith("twilight_lib:kitsune_") ||
                    active.startsWith("twilight_lib:harpy_") ||
                    active.equals("twilight_lib:short_torso")) {
                toRemove.add(active);
            }
        }
        for (String id : toRemove) {
            addons.forceUnequipAddon(id);
        }
    }

    /**
     * Resolves placeholders in addon IDs using customization values.
     * Example: "twilight_lib:kitsune_ears_{color}" + color="yellow" =>
     * "twilight_lib:kitsune_ears_yellow"
     */
    private static String resolvePlaceholders(String template, Map<String, String> custMap) {
        String result = template;
        for (Map.Entry<String, String> entry : custMap.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /**
     * Resolves a color value from customization or hex string.
     * Supports both direct hex values (#FFFFFF) and customization keys.
     */
    private static int resolveColor(String colorSource, Map<String, String> custMap) {
        // Check if it's a customization key reference (e.g., "{feather_color}")
        if (colorSource.startsWith("{") && colorSource.endsWith("}")) {
            String key = colorSource.substring(1, colorSource.length() - 1);
            String value = custMap.getOrDefault(key, "#FFFFFF");
            return parseHex(value, 0xFFFFFF);
        }
        // Direct hex value
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
