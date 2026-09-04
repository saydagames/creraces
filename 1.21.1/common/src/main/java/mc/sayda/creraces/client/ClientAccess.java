package mc.sayda.creraces.client;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

public class ClientAccess {
    public static net.minecraft.world.entity.player.Player lastSyncedPlayer = null;
    public static boolean isWaitingForRaceSelection = false;

    public static Level getLevel() {
        return EnvExecutor.getEnvSpecific(() -> () -> net.minecraft.client.Minecraft.getInstance().level,
                () -> () -> null);
    }

    public static void setScreen(Screen screen) {
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> net.minecraft.client.Minecraft.getInstance().setScreen(screen));
    }

    public static Player getPlayer() {
        return EnvExecutor.getEnvSpecific(() -> () -> net.minecraft.client.Minecraft.getInstance().player,
                () -> () -> null);
    }

    public static void displayItemActivation(ItemStack stack) {
        EnvExecutor.runInEnv(Env.CLIENT,
                () -> () -> net.minecraft.client.Minecraft.getInstance().gameRenderer.displayItemActivation(stack));
    }

    public static void handleSyncIncident(java.util.UUID playerId, net.minecraft.nbt.CompoundTag data) {
        net.minecraft.world.entity.player.Player target = null;
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getUUID().equals(playerId)) {
            target = minecraft.player;
        } else if (minecraft.level != null) {
            target = minecraft.level.getPlayerByUUID(playerId);
        }

        if (target == null) {
            mc.sayda.creraces.CreRaces.LOGGER.warn("ClientAccess: Could not find target player {} for sync", playerId);
            return;
        }

        final net.minecraft.world.entity.player.Player finalTarget = target;
        mc.sayda.creraces.capability.DataUtils.getVariables(finalTarget).ifPresent(vars -> {
            boolean oldSmallBuild = vars.isSmallBuild();
            vars.deserialize(data);
            if (vars.isSmallBuild() != oldSmallBuild) {
                mc.sayda.creraces.CreRaces.LOGGER.info("ClientAccess: smallBuild for {} changed from {} to {}",
                        finalTarget.getName().getString(), oldSmallBuild, vars.isSmallBuild());
            }
            // We skip client-side application here.
            // Authority resides on the server, which sends SyncAddonsPacket.
            // Local player preview is handled by screens (DynamicMirrorScreen) separately.

            // Sync persistent data tag directly to the Entity for client-side tag checks
            if (data.contains("creraces:persistent_data")
                    && finalTarget instanceof mc.sayda.creraces.util.IPersistentDataAccessor accessor) {
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

    public static void handleRaceSync(java.util.Map<net.minecraft.resources.ResourceLocation, String> raceData) {
        mc.sayda.creraces.race.RaceRegistry.clear();
        java.util.Map<net.minecraft.resources.ResourceLocation, com.google.gson.JsonElement> data = new java.util.HashMap<>();

        raceData.forEach((id, json) -> {
            try {
                data.put(id, com.google.gson.JsonParser.parseString(json));
            } catch (Exception e) {
                mc.sayda.creraces.CreRaces.LOGGER.error("Failed to parse synced race {}: {}", id, e.getMessage());
            }
        });

        mc.sayda.creraces.race.RaceManager.syncFromServer(data);
        mc.sayda.creraces.CreRaces.LOGGER.info("Synced {} races from server.", raceData.size());

        // Server will send SyncAddonsPacket for the new race
    }

    public static void handleQuestSync(java.util.Map<net.minecraft.resources.ResourceLocation, String> questData) {
        java.util.Map<net.minecraft.resources.ResourceLocation, com.google.gson.JsonElement> data = new java.util.HashMap<>();

        questData.forEach((id, json) -> {
            try {
                data.put(id, com.google.gson.JsonParser.parseString(json));
            } catch (Exception e) {
                mc.sayda.creraces.CreRaces.LOGGER.error("Failed to parse synced quest {}: {}", id, e.getMessage());
            }
        });

        mc.sayda.creraces.quest.QuestManager.syncFromServer(data);
        mc.sayda.creraces.CreRaces.LOGGER.info("Synced {} quests from server.", questData.size());
    }

    public static void handleAbilitySync(java.util.Map<net.minecraft.resources.ResourceLocation, String> abilityData) {
        java.util.Map<net.minecraft.resources.ResourceLocation, com.google.gson.JsonElement> data = new java.util.HashMap<>();

        abilityData.forEach((id, json) -> {
            try {
                data.put(id, com.google.gson.JsonParser.parseString(json));
            } catch (Exception e) {
                mc.sayda.creraces.CreRaces.LOGGER.error("Failed to parse synced ability {}: {}", id, e.getMessage());
            }
        });

        mc.sayda.creraces.ability.AbilityRegistry.clear();
        mc.sayda.creraces.ability.AbilityManager.syncFromServer(data);
        mc.sayda.creraces.CreRaces.LOGGER.info("Synced {} abilities from server.", abilityData.size());
    }
}
