package mc.sayda.creraces.race;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import mc.sayda.twilight_lib.capabilities.IAddons;
import net.minecraft.nbt.CompoundTag;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.lang.reflect.Method;
import mc.sayda.creraces.engine.traits.AddonTrait;

/**
 * Bridges CreRaces racial customizations with Twilight Lib's cosmetic system.
 */
public class CosmeticIncidents {

    public static void applyCustomizations(net.minecraft.world.entity.player.Player player, Map<String, String> custMap,
            Race race) {
        if (race == null)
            return;

        IAddons addons = DataUtils.getAddonsData(player);
        if (addons == null)
            return;

        // 1. Clear ALL existing racial addons (Respects ownership/customizations)
        clearRacialAddons(player, addons);

        // If master toggle is off, stop here after clearing
        if (!mc.sayda.creraces.config.CreRacesConfig.RACE_ADDONS_ENABLED.get()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer) {
                mc.sayda.twilight_lib.network.NetworkHandler.sendAddonsToAll(
                        createSyncPacket(player.getUUID(), addons.getActiveAddons(),
                                getExternalGrantsRobust(addons), addons.getAllAddonTints()));
            }
            return;
        }

        // 2. Integrated Application (Auto-apply from Customization definitions)
        for (RaceCustomization cust : race.customization()) {
            String value = custMap.getOrDefault(cust.id(), cust.defaultValue());

            // Handle addon mapping (Integrated Cosmetics)
            if (cust.addonData() != null) {
                applyAddonData(player, cust.addonData(), value, custMap, addons);
            }

            // Handle simple addonId (legacy/fixed)
            if (cust.addonId() != null) {
                setAddonActiveRobust(addons, cust.addonId(), true, true);

                // If the type is 'tint', apply it as a color to this fixed addon
                if (cust.type().equals("tint")) {
                    addons.setAddonTint(cust.addonId(), parseHex(value, 0xFFFFFF));
                }
            }
        }

        // 3. Trait Application (Still used for fixed/complex logic)
        for (var trait : race.traits()) {
            if (trait instanceof AddonTrait addonTrait) {
                if (!addonTrait.isEnabled()) {
                    continue;
                }

                var condition = addonTrait.getCondition();
                if (condition != null && !condition.evaluate(player, null, null, null)) {
                    continue;
                }

                String addonId = resolvePlaceholders(addonTrait.getAddonId(), custMap, race);
                setAddonActiveRobust(addons, addonId, true, true);

                if (addonTrait.getTint() != null && !addonTrait.getTint().isEmpty()) {
                    int tintColor = resolveColor(addonTrait.getTint(), custMap);
                    addons.setAddonTint(addonId, tintColor);
                }
            }
        }

        // 4. Final re-application of lore aesthetics (Ensure chest addon isn't
        // stripped)
        if (player instanceof net.minecraft.server.level.ServerPlayer) {
            applyGStateAddons((net.minecraft.server.level.ServerPlayer) player, false);
        }

        // 5. Sync to all
        syncAddons(player);
    }

    public static void syncAddons(net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer))
            return;

        IAddons addons = DataUtils.getAddonsData(player);
        if (addons == null)
            return;

        mc.sayda.twilight_lib.network.NetworkHandler.sendAddonsToAll(
                createSyncPacket(player.getUUID(), addons.getActiveAddons(),
                        getExternalGrantsRobust(addons), addons.getAllAddonTints()));
    }

    private static void applyAddonData(net.minecraft.world.entity.player.Player player, JsonObject data,
            String value, Map<String, String> custMap, IAddons addons) {
        if (data == null || addons == null)
            return;

        if (data.has(value)) {
            JsonElement element = data.get(value);
            if (element.isJsonArray()) {
                for (JsonElement e : element.getAsJsonArray()) {
                    if (applyComplexAddon(e, value, custMap, addons)) {
                        if (e.isJsonObject() && e.getAsJsonObject().has("condition")) {
                            break;
                        }
                    }
                }
            } else {
                applyComplexAddon(element, value, custMap, addons);
            }
        }

        if (data.has("pattern")) {
            String pattern = data.get("pattern").getAsString();
            String addonId = pattern.replace("{self}", value);
            addonId = resolvePlaceholders(addonId, custMap, null);
            setAddonActiveRobust(addons, addonId, true, true);
        }
    }

    private static boolean applyComplexAddon(JsonElement element, String value,
            Map<String, String> custMap, IAddons addons) {
        if (element.isJsonPrimitive()) {
            setAddonActiveRobust(addons, resolvePlaceholders(element.getAsString(), custMap, null), true, true);
            return true;
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("id")) {
                String id = resolvePlaceholders(obj.get("id").getAsString(), custMap, null);
                setAddonActiveRobust(addons, id, true, true);
                if (obj.has("tint")) {
                    int tint = resolveColor(obj.get("tint").getAsString(), custMap);
                    addons.setAddonTint(id, tint);
                }
                return true;
            }
        }
        return false;
    }

    public static void clearAllRacialAddons(net.minecraft.world.entity.player.Player player) {
        IAddons addons = DataUtils.getAddonsData(player);
        if (addons != null) {
            clearRacialAddons(player, addons);
            syncAddons(player);
        }
    }

    private static void clearRacialAddons(net.minecraft.world.entity.player.Player player, IAddons addons) {
        Set<String> owned = addons.getAddons();

        for (String id : new HashSet<>(addons.getActiveAddons())) {
            // If the player doesn't permanently own the addon (supporter/admin grant),
            // we wipe it upon race reset. This covers both racial traits and Mirror
            // selections.
            if (!owned.contains(id)) {
                mc.sayda.creraces.CreRaces.LOGGER.info("[CreRaces] Deactivating non-owned addon on reset: {}", id);
                setAddonActiveRobust(addons, id, false, true);
            }
        }
    }

    public static String resolvePlaceholders(String template, Map<String, String> custMap) {
        return resolvePlaceholders(template, custMap, null);
    }

    public static String resolvePlaceholders(String template, Map<String, String> custMap,
            @javax.annotation.Nullable Race race) {
        String result = template;
        for (Map.Entry<String, String> entry : custMap.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        if (race != null && result.contains("{")) {
            for (RaceCustomization cust : race.customization()) {
                String placeholder = "{" + cust.id() + "}";
                if (result.contains(placeholder)) {
                    result = result.replace(placeholder, cust.defaultValue());
                }
            }
        }
        if (result.contains("{")) {
            result = result.replaceAll("\\{[^}]*\\}", "0");
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

    public static void applyGStateCosmetics(net.minecraft.server.level.ServerPlayer player, Race race,
            mc.sayda.creraces.capability.IPlayerVariables vars) {
        if (!mc.sayda.creraces.config.CreRacesConfig.GSTATE_ENABLED.get())
            return;

        if (race.getGState() == mc.sayda.creraces.engine.GState.FEMALE) {
            vars.setGState(1);
        } else if (race.getGState() == mc.sayda.creraces.engine.GState.MALE) {
            vars.setGState(0);
        }

        applyGStateAddons(player, true);
        vars.sync(player);
    }

    public static void applyGStateAddons(net.minecraft.server.level.ServerPlayer player) {
        applyGStateAddons(player, true);
    }

    public static void applyGStateAddons(net.minecraft.server.level.ServerPlayer player, boolean sync) {
        if (!mc.sayda.creraces.config.CreRacesConfig.GSTATE_ENABLED.get())
            return;

        if (!mc.sayda.creraces.config.CreRacesConfig.RACE_ADDONS_ENABLED.get())
            return;

        int effectiveState = mc.sayda.creraces.capability.DataUtils.getVariables(player)
                .map(mc.sayda.creraces.capability.IPlayerVariables::getGState).orElse(0);

        if (mc.sayda.creraces.config.CreRacesConfig.LORE_ADDONS_ENABLED.get()) {
            var modelVariant = mc.sayda.twilight_lib.capabilities.DataUtils.getModelVariantData(player);
            if (modelVariant != null) {
                modelVariant.setModelVariant(effectiveState == 1 ? "alex" : "default");
                CompoundTag serialized = modelVariant.serialize();
                if (serialized != null) {
                    mc.sayda.twilight_lib.capabilities.DataUtils.getPersistentData(player)
                            .put(mc.sayda.twilight_lib.TwilightConstants.NBT_MODEL_VARIANT, serialized);
                }
                mc.sayda.twilight_lib.network.NetworkHandler.sendModelVariantToAll(
                        mc.sayda.twilight_lib.network.SyncModelVariantPacket.of(player.getUUID(), modelVariant));
            }

            IAddons addons = DataUtils.getAddonsData(player);
            if (addons != null) {
                if (effectiveState == 1) {
                    setAddonActiveRobust(addons, "chest", true, true);
                } else {
                    // Forcefully deactivate if they are now MALE
                    setAddonActiveRobust(addons, "chest", false, true);
                }
                if (sync) {
                    syncAddons(player);
                }
            }
        }
    }

    public static void setAddonActiveRobust(IAddons addons, String id, boolean active, boolean persistent) {
        if (addons == null || id == null || id.isEmpty())
            return;

        mc.sayda.creraces.CreRaces.LOGGER.debug("[CreRaces] setAddonActiveRobust: id={}, active={}, persistent={}", id,
                active, persistent);

        // Try standard 3-arg method if available (Twilight Lib 2.0.0+)
        try {
            Method m = addons.getClass().getMethod("setActiveAddon", String.class, boolean.class, boolean.class);
            m.invoke(addons, id, active, persistent);
            return;
        } catch (Exception ignored) {
        }

        if (active) {
            // Fallback for older/different versions
            if (persistent && !addons.hasAddon(id)) {
                addons.addAddon(id);
            }
            addons.setActiveAddon(id, true);
        } else {
            addons.setActiveAddon(id, false);
            if (persistent) {
                try {
                    Method m = addons.getClass().getMethod("removeAddon", String.class);
                    m.invoke(addons, id);
                } catch (Exception ignored) {
                    try {
                        Method m = addons.getClass().getMethod("revokeAddon", String.class);
                        m.invoke(addons, id);
                    } catch (Exception ignored2) {
                    }
                }
            }
        }
    }

    public static Set<String> getExternalGrantsRobust(mc.sayda.twilight_lib.capabilities.IAddons addons) {
        try {
            Method m = addons.getClass().getMethod("getExternalGrants");
            Object result = m.invoke(addons);
            if (result instanceof Set) {
                return (Set<String>) result;
            }
        } catch (Exception e) {
        }
        return new HashSet<>();
    }

    public static mc.sayda.twilight_lib.network.SyncAddonsPacket createSyncPacket(java.util.UUID playerUUID,
            Set<String> active, Set<String> external, Map<String, Integer> tints) {
        try {
            // Try 4-arg constructor (UUID, Set, Set, Map)
            var c4 = mc.sayda.twilight_lib.network.SyncAddonsPacket.class.getConstructor(java.util.UUID.class,
                    Set.class, Set.class, Map.class);
            return (mc.sayda.twilight_lib.network.SyncAddonsPacket) c4.newInstance(playerUUID, active, external, tints);
        } catch (Exception e) {
            try {
                // Fallback to legacy 3-arg constructor (UUID, Set, Map)
                var c3 = mc.sayda.twilight_lib.network.SyncAddonsPacket.class.getConstructor(java.util.UUID.class,
                        Set.class, Map.class);
                return (mc.sayda.twilight_lib.network.SyncAddonsPacket) c3.newInstance(playerUUID, active, tints);
            } catch (Exception e2) {
                // Fallback to minimal buffer constructor if all else fails (unlikely, but safe)
                return null;
            }
        }
    }
}
