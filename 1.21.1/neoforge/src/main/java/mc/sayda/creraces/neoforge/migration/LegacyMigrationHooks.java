package mc.sayda.creraces.neoforge.migration;

import dev.architectury.event.events.common.PlayerEvent;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.RaceIncidents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NeoForge counterpart of the 1.20.1 Forge module's LegacyMigrationHooks; detection, marker
 * persistence, the console prompt, and per-player race conversion all mirror that file exactly.
 * Block ID remapping is handled separately (see LegacyBlockRemaps): 1.20.1 used Forge's
 * MissingMappingsEvent, which NeoForge dropped in favor of registry aliases declared up front.
 */
public final class LegacyMigrationHooks {
    private static final Logger LOGGER = LoggerFactory.getLogger("CreRaces");
    /** True only for the remainder of this boot, after the operator chose "migrate". Never persisted. */
    private static volatile boolean migrationModeActive = false;
    private static final Set<UUID> ATTEMPTED = ConcurrentHashMap.newKeySet();
    private static final int POLL_INTERVAL_MS = 50;

    private LegacyMigrationHooks() {}

    /**
     * Must be registered before {@link mc.sayda.creraces.CreRaces#init()} (which registers
     * IncidentResolver's own PLAYER_JOIN handler) so the migrated race is already set before any
     * other join-time sync logic runs for that player.
     */
    public static void init() {
        NeoForge.EVENT_BUS.addListener(LegacyMigrationHooks::onServerAboutToStart);
        PlayerEvent.PLAYER_JOIN.register(LegacyMigrationHooks::onPlayerJoin);
    }

    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        migrationModeActive = false;
        ATTEMPTED.clear();

        if (!LegacyDetection.classicWorldPresent(server)) {
            return;
        }

        if (!server.isDedicatedServer()) {
            // Singleplayer is gated earlier, client-side, before the integrated server exists at
            // all (see LegacyWorldLoadGate/WorldOpenFlowsMixin), blocking THIS thread waiting on
            // a screen click both races vanilla's own level-loading screen for control of the
            // display and trips the server watchdog, crashing the game. By the time we get here
            // the marker should already exist; just read whatever was decided.
            String choice = LegacyDetection.readChoice(server);
            migrationModeActive = "migrate".equals(choice);
            if (choice == null) {
                LOGGER.warn("[CreRaces] Legacy world reached server startup with no prior migration decision, skipping.");
                LegacyDetection.writeMarker(server, "skip");
            }
            return;
        }

        if (LegacyDetection.alreadyHandled(server)) {
            migrationModeActive = "migrate".equals(LegacyDetection.readChoice(server));
            return;
        }

        LOGGER.info("[CreRaces] Legacy world detected, prompting for migration choice (console).");
        int choice = promptConsole((net.minecraft.server.dedicated.DedicatedServer) server);
        LOGGER.info("[CreRaces] Legacy migration choice received: {}", choice);

        switch (choice) {
            case 1 -> {
                LOGGER.warn("[CreRaces] Legacy migration aborted by operator choice, shutting down, world untouched.");
                System.exit(0);
            }
            case 2 -> {
                migrationModeActive = true;
                LegacyDetection.writeMarker(server, "migrate");
                LOGGER.info("[CreRaces] Legacy migration enabled for this boot.");
            }
            default -> {
                LegacyDetection.writeMarker(server, "skip");
                LOGGER.info("[CreRaces] Legacy migration skipped by operator choice.");
            }
        }
    }

    /**
     * Polls DedicatedServer's own console-input queue (see DedicatedServerConsoleAccessor)
     * instead of reading System.in directly, the "Server console handler" thread already reads
     * and queues every console line for the entire server lifetime; a second reader on the same
     * stream would race it for raw bytes. This fires before the tick loop starts (which is what
     * normally drains that queue), so we drain it ourselves here instead.
     */
    private static int promptConsole(net.minecraft.server.dedicated.DedicatedServer server) {
        System.out.println();
        System.out.println("=====================================================================");
        System.out.println("IMPORTANT: a previous instance of CreRaces Classic has been detected");
        System.out.println("on the world you are trying to load.");
        System.out.println();
        System.out.println("  1) Stop loading now. The world will not be touched.");
        System.out.println("  2) Load with one-time migration: convert players' Classic races into");
        System.out.println("     the new system, then never ask again on this world.");
        System.out.println("  3) Load normally. Skip migration; Classic-only data is left as-is.");
        System.out.println("=====================================================================");

        java.util.List<net.minecraft.server.ConsoleInput> queue =
                ((mc.sayda.creraces.neoforge.mixin.DedicatedServerConsoleAccessor) server).creraces$getConsoleInput();
        queue.clear();
        LOGGER.info("[CreRaces] Waiting for console input (1/2/3)...");
        System.out.print("Enter 1, 2, or 3: ");
        System.out.flush();

        while (true) {
            String line = pollNextLine(queue);
            if (line == null) {
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return 3;
                }
                continue;
            }
            line = line.trim();
            if (line.equals("1") || line.equals("2") || line.equals("3")) {
                return Integer.parseInt(line);
            }
            System.out.println("Unrecognized input, please enter 1, 2, or 3.");
            System.out.print("Enter 1, 2, or 3: ");
            System.out.flush();
        }
    }

    private static String pollNextLine(java.util.List<net.minecraft.server.ConsoleInput> queue) {
        if (queue.isEmpty()) return null;
        try {
            return queue.remove(0).msg;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private static void onPlayerJoin(Player player) {
        if (!migrationModeActive || !(player instanceof ServerPlayer serverPlayer)) return;
        if (!ATTEMPTED.add(player.getUUID())) return;

        MinecraftServer server = serverPlayer.getServer();
        if (server == null) return;

        Path playerDataFile = server.getWorldPath(Objects.requireNonNull(LevelResource.PLAYER_DATA_DIR))
                .resolve(player.getUUID() + ".dat");
        File file = playerDataFile.toFile();
        if (!file.exists()) return;

        double isRace;
        try {
            CompoundTag root = NbtIo.readCompressed(playerDataFile, NbtAccounter.unlimitedHeap());
            CompoundTag forgeCaps = root.getCompound("ForgeCaps");
            if (!forgeCaps.contains("creraces:player_variables")) return;
            CompoundTag vars = forgeCaps.getCompound("creraces:player_variables");
            if (!vars.contains("IsRace")) return;
            isRace = vars.getDouble("IsRace");
        } catch (Exception e) {
            LOGGER.warn("[CreRaces] Could not read legacy playerdata for {}: {}", player.getUUID(), e.getMessage());
            return;
        }

        ResourceLocation migratedRace = LegacyRaceMap.resolve(isRace);
        if (migratedRace == null) {
            LOGGER.info("[CreRaces] Player {} had no migratable Classic race (IsRace={}), leaving for normal selection.",
                    player.getGameProfile().getName(), isRace);
            return;
        }

        DataUtils.getVariables(serverPlayer).ifPresent(vars -> {
            vars.setRace(migratedRace);
            vars.setHasChosenRace(true);
        });
        RaceIncidents.refreshPlayer(serverPlayer);
        LOGGER.info("[CreRaces] Migrated {} to {} (Classic IsRace={})",
                player.getGameProfile().getName(), migratedRace, isRace);
    }
}
