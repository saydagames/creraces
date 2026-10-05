package mc.sayda.creraces.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilityManager;
import mc.sayda.creraces.ability.AbilityRegistry;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.quest.QuestManager;
import mc.sayda.creraces.race.RaceManager;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientAccess {
    public static Player lastSyncedPlayer = null;
    /** A race pick is waiting for the server sync that confirms it; see CreRacesClient.enforceRaceSelection. */
    public static boolean isWaitingForRaceSelection = false;

    public static Level getLevel() {
        return EnvExecutor.getEnvSpecific(() -> () -> Minecraft.getInstance().level, () -> () -> null);
    }

    public static void setScreen(Screen screen) {
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> Minecraft.getInstance().setScreen(screen));
    }

    public static Player getPlayer() {
        return EnvExecutor.getEnvSpecific(() -> () -> Minecraft.getInstance().player, () -> () -> null);
    }

    public static void displayItemActivation(ItemStack stack) {
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> Minecraft.getInstance().gameRenderer.displayItemActivation(stack));
    }

    public static void handleSyncIncident(UUID playerId, CompoundTag data) {
        Minecraft minecraft = Minecraft.getInstance();
        Player target = null;
        if (minecraft.player != null && minecraft.player.getUUID().equals(playerId)) {
            target = minecraft.player;
        } else if (minecraft.level != null) {
            target = minecraft.level.getPlayerByUUID(playerId);
        }

        if (target == null) {
            CreRaces.LOGGER.warn("ClientAccess: Could not find target player {} for sync", playerId);
            return;
        }

        final Player finalTarget = target;
        DataUtils.getVariables(finalTarget).ifPresent(vars -> {
            boolean oldSmallBuild = vars.isSmallBuild();
            vars.deserialize(data);
            if (vars.isSmallBuild() != oldSmallBuild) {
                CreRaces.LOGGER.info("ClientAccess: smallBuild for {} changed from {} to {}",
                        finalTarget.getName().getString(), oldSmallBuild, vars.isSmallBuild());
            }
            // Addons are not applied here: the server stays authoritative and sends SyncAddonsPacket
            // itself, and the local preview is handled by the screens that show it.

            // Mirror the persistent data tag onto the entity so client-side tag checks can read it
            if (data.contains("creraces:persistent_data")
                    && finalTarget instanceof IPersistentDataAccessor accessor) {
                accessor.creraces$getPersistentData().merge(data.getCompound("creraces:persistent_data"));
            }

            if (finalTarget == minecraft.player) {
                lastSyncedPlayer = finalTarget;
                if (data.contains("hasChosenRace") && data.getBoolean("hasChosenRace")) {
                    isWaitingForRaceSelection = false;
                }
            }
        });
    }

    public static void handleRaceSync(Map<ResourceLocation, String> raceData) {
        RaceRegistry.clear();
        RaceManager.syncFromServer(parseSyncedJson(raceData, "race"));
        CreRaces.LOGGER.info("Synced {} races from server.", raceData.size());
        // The server follows up with SyncAddonsPacket for the new race
    }

    public static void handleQuestSync(Map<ResourceLocation, String> questData) {
        QuestManager.syncFromServer(parseSyncedJson(questData, "quest"));
        CreRaces.LOGGER.info("Synced {} quests from server.", questData.size());
    }

    public static void handleAbilitySync(Map<ResourceLocation, String> abilityData) {
        Map<ResourceLocation, JsonElement> parsed = parseSyncedJson(abilityData, "ability");
        AbilityRegistry.clear();
        AbilityManager.syncFromServer(parsed);
        CreRaces.LOGGER.info("Synced {} abilities from server.", abilityData.size());
    }

    /** Parses each synced JSON string, logging and skipping any entry that fails to parse. */
    private static Map<ResourceLocation, JsonElement> parseSyncedJson(Map<ResourceLocation, String> raw, String kind) {
        Map<ResourceLocation, JsonElement> parsed = new HashMap<>();
        raw.forEach((id, json) -> {
            try {
                parsed.put(id, JsonParser.parseString(json));
            } catch (JsonParseException e) {
                CreRaces.LOGGER.error("Failed to parse synced {} {}: {}", kind, id, e.getMessage());
            }
        });
        return parsed;
    }
}
