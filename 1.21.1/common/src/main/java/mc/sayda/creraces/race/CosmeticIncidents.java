package mc.sayda.creraces.race;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.GState;
import mc.sayda.creraces.engine.TraitDispatch;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.engine.traits.AddonTrait;
import mc.sayda.twilight_lib.TwilightConstants;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import mc.sayda.twilight_lib.capabilities.IAddons;
import mc.sayda.twilight_lib.network.NetworkHandler;
import mc.sayda.twilight_lib.network.SyncAddonsPacket;
import mc.sayda.twilight_lib.network.SyncModelVariantPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Bridges CreRaces racial customizations with Twilight Lib's cosmetic system.
 * The reflective calls keep older Twilight Lib builds working, which lack some of the
 * addon methods and the four-argument sync packet.
 */
public class CosmeticIncidents {
    // IPlayerVariables stores gState as 0 (male) or 1 (female).
    private static final int GSTATE_FEMALE = 1;

    public static void applyCustomizations(Player player, Map<String, String> custMap, Race race) {
        if (race == null)
            return;

        IAddons addons = DataUtils.getAddonsData(player);
        if (addons == null)
            return;

        // Owned addons (supporter/admin grants) survive this.
        clearRacialAddons(addons);

        if (!CreRacesConfig.RACE_ADDONS_ENABLED.get()) {
            syncAddons(player);
            return;
        }

        for (RaceCustomization cust : race.customization()) {
            String value = custMap.getOrDefault(cust.id(), cust.defaultValue());

            if (cust.addonData() != null) {
                applyAddonData(player, cust.addonData(), value, custMap, addons);
            }

            // A fixed addonId is always shown; a "tint" customization colours it.
            if (cust.addonId() != null) {
                setAddonActiveRobust(addons, cust.addonId(), true, true);
                if (cust.type().equals("tint")) {
                    addons.setAddonTint(cust.addonId(), parseHex(value, 0xFFFFFF));
                }
            }
        }

        for (var trait : TraitDispatch.forPlayer(player)) {
            if (!(trait instanceof AddonTrait addonTrait) || !addonTrait.isEnabled())
                continue;

            var condition = addonTrait.getCondition();
            if (condition != null && !condition.evaluate(player, null, null, null))
                continue;

            String addonId = resolvePlaceholders(addonTrait.getAddonId(), custMap, race);
            setAddonActiveRobust(addons, addonId, true, true);

            if (addonTrait.getTint() != null && !addonTrait.getTint().isEmpty()) {
                addons.setAddonTint(addonId, resolveColor(addonTrait.getTint(), custMap));
            }
        }

        // Clearing above also removed the gState chest addon, so put it back.
        if (player instanceof ServerPlayer serverPlayer) {
            applyGStateAddons(serverPlayer, false);
        }

        syncAddons(player);
    }

    public static void syncAddons(Player player) {
        if (!(player instanceof ServerPlayer))
            return;

        IAddons addons = DataUtils.getAddonsData(player);
        if (addons == null)
            return;

        var pkt = createSyncPacket(player.getUUID(), addons.getActiveAddons(),
                getExternalGrantsRobust(addons), addons.getAllAddonTints());
        if (pkt != null)
            NetworkHandler.sendAddonsToAll(pkt);
    }

    private static void applyAddonData(Player player, JsonObject data, String value, Map<String, String> custMap,
            IAddons addons) {
        if (data == null || addons == null)
            return;

        if (data.has(value)) {
            JsonElement element = data.get(value);
            if (element.isJsonArray()) {
                // Every unconditional entry applies; the first conditional entry that passes ends the list.
                for (JsonElement e : element.getAsJsonArray()) {
                    boolean hasCondition = e.isJsonObject() && e.getAsJsonObject().has("condition");
                    if (applyComplexAddon(player, e, custMap, addons) && hasCondition) {
                        break;
                    }
                }
            } else {
                applyComplexAddon(player, element, custMap, addons);
            }
        }

        if (data.has("pattern")) {
            String addonId = data.get("pattern").getAsString().replace("{self}", value);
            setAddonActiveRobust(addons, resolvePlaceholders(addonId, custMap, null), true, true);
        }
    }

    /** Returns true if an addon was activated. */
    private static boolean applyComplexAddon(Player player, JsonElement element, Map<String, String> custMap,
            IAddons addons) {
        if (element.isJsonPrimitive()) {
            setAddonActiveRobust(addons, resolvePlaceholders(element.getAsString(), custMap, null), true, true);
            return true;
        }
        if (!element.isJsonObject())
            return false;

        JsonObject obj = element.getAsJsonObject();
        if (obj.has("condition") && player != null) {
            Condition cond = Condition.fromJson(obj.getAsJsonObject("condition"));
            if (!cond.evaluate(player, null, null, null)) {
                return false;
            }
        }

        if (!obj.has("id"))
            return false;
        String id = resolvePlaceholders(obj.get("id").getAsString(), custMap, null);
        setAddonActiveRobust(addons, id, true, true);
        if (obj.has("tint")) {
            addons.setAddonTint(id, resolveColor(obj.get("tint").getAsString(), custMap));
        }
        return true;
    }

    public static void clearAllRacialAddons(Player player) {
        IAddons addons = DataUtils.getAddonsData(player);
        if (addons != null) {
            clearRacialAddons(addons);
            syncAddons(player);
        }
    }

    /**
     * Deactivates every addon the player does not permanently own (supporter/admin grants),
     * which covers both racial addons and Mirror selections.
     */
    private static void clearRacialAddons(IAddons addons) {
        Set<String> owned = addons.getAddons();
        for (String id : new HashSet<>(addons.getActiveAddons())) {
            if (!owned.contains(id)) {
                CreRaces.LOGGER.debug("Deactivating non-owned addon on reset: {}", id);
                setAddonActiveRobust(addons, id, false, true);
            }
        }
    }

    public static String resolvePlaceholders(String template, Map<String, String> custMap) {
        return resolvePlaceholders(template, custMap, null);
    }

    public static String resolvePlaceholders(String template, Map<String, String> custMap, @Nullable Race race) {
        String result = template;
        for (Map.Entry<String, String> entry : custMap.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        if (race != null) {
            result = result.replace("{race}", race.id().toString());
            if (race.getGState() != GState.BOTH) {
                result = result.replace("{gender}", race.getGState() == GState.FEMALE ? "female" : "male");
            }

            for (RaceCustomization cust : race.customization()) {
                result = result.replace("{" + cust.id() + "}", cust.defaultValue());
            }
        }

        // Anything still unresolved falls back to variant 0.
        if (result.contains("{")) {
            result = result.replaceAll("\\{[^}]*\\}", "0");
        }
        return result;
    }

    public static String resolvePlaceholders(Player player, String template) {
        return mc.sayda.creraces.capability.DataUtils.getVariables(player).map(vars -> {
            ResourceLocation raceId = vars.getRace();
            Race race = RaceRegistry.get(raceId);
            String result = resolvePlaceholders(template, vars.getCustomizations(), race);

            // These are not customizations, so the custMap pass above never fills them.
            result = result.replace("{race}", raceId.toString());
            result = result.replace("{gstate}", String.valueOf(vars.getGState()));
            result = result.replace("{gender}", vars.getGState() == GSTATE_FEMALE ? "female" : "male");

            return result;
        }).orElse(template);
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

    public static void applyGStateCosmetics(ServerPlayer player, Race race, IPlayerVariables vars) {
        if (!CreRacesConfig.GSTATE_ENABLED.get())
            return;

        if (race.getGState() == GState.FEMALE) {
            vars.setGState(GSTATE_FEMALE);
        } else if (race.getGState() == GState.MALE) {
            vars.setGState(0);
        }

        applyGStateAddons(player, true);
        vars.sync(player);
    }

    public static void applyGStateAddons(ServerPlayer player) {
        applyGStateAddons(player, true);
    }

    public static void applyGStateAddons(ServerPlayer player, boolean sync) {
        if (!CreRacesConfig.GSTATE_ENABLED.get() || !CreRacesConfig.RACE_ADDONS_ENABLED.get()
                || !CreRacesConfig.LORE_ADDONS_ENABLED.get())
            return;

        boolean female = mc.sayda.creraces.capability.DataUtils.getVariables(player)
                .map(IPlayerVariables::getGState).orElse(0) == GSTATE_FEMALE;

        var modelVariant = DataUtils.getModelVariantData(player);
        if (modelVariant != null) {
            modelVariant.setModelVariant(female ? "alex" : "default");
            CompoundTag serialized = modelVariant.serialize();
            if (serialized != null) {
                DataUtils.getPersistentData(player).put(TwilightConstants.NBT_MODEL_VARIANT, serialized);
            }
            NetworkHandler.sendModelVariantToAll(SyncModelVariantPacket.of(player.getUUID(), modelVariant));
        }

        IAddons addons = DataUtils.getAddonsData(player);
        if (addons != null) {
            setAddonActiveRobust(addons, "chest", female, true);
            if (sync) {
                syncAddons(player);
            }
        }
    }

    public static void setAddonActiveRobust(IAddons addons, String id, boolean active, boolean persistent) {
        if (addons == null || id == null || id.isEmpty())
            return;

        CreRaces.LOGGER.debug("setAddonActiveRobust: id={}, active={}, persistent={}", id, active, persistent);

        try {
            Method m = addons.getClass().getMethod("setActiveAddon", String.class, boolean.class, boolean.class);
            m.invoke(addons, id, active, persistent);
            return;
        } catch (ReflectiveOperationException ignored) {
            // No persistent overload in this Twilight Lib build; emulate it below.
        }

        if (active) {
            if (persistent && !addons.hasAddon(id)) {
                addons.addAddon(id);
            }
            addons.setActiveAddon(id, true);
            return;
        }

        addons.setActiveAddon(id, false);
        if (persistent && !invokeIfPresent(addons, "removeAddon", id)) {
            invokeIfPresent(addons, "revokeAddon", id);
        }
    }

    /** Calls a single-String method by name, returning false if it does not exist or fails. */
    private static boolean invokeIfPresent(IAddons addons, String methodName, String arg) {
        try {
            addons.getClass().getMethod(methodName, String.class).invoke(addons, arg);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static Set<String> getExternalGrantsRobust(IAddons addons) {
        try {
            Method m = addons.getClass().getMethod("getExternalGrants");
            Object result = m.invoke(addons);
            if (result instanceof Set) {
                return (Set<String>) result;
            }
        } catch (ReflectiveOperationException ignored) {
            // Builds without external grants simply report none.
        }
        return new HashSet<>();
    }

    /** Returns null if neither known packet constructor exists. */
    @Nullable
    public static SyncAddonsPacket createSyncPacket(UUID playerUUID, Set<String> active, Set<String> external,
            Map<String, Integer> tints) {
        try {
            return SyncAddonsPacket.class.getConstructor(UUID.class, Set.class, Set.class, Map.class)
                    .newInstance(playerUUID, active, external, tints);
        } catch (ReflectiveOperationException e) {
            try {
                // Builds from before external grants existed.
                return SyncAddonsPacket.class.getConstructor(UUID.class, Set.class, Map.class)
                        .newInstance(playerUUID, active, tints);
            } catch (ReflectiveOperationException e2) {
                return null;
            }
        }
    }
}
