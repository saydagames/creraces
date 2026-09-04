package mc.sayda.creraces.neoforge.migration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Detects a leftover CreRaces Classic world (Forge SavedData files this rewrite never writes)
 * and tracks whether the one-time migration prompt has already been resolved for this world.
 * Mirrors the 1.20.1 Forge module's LegacyDetection, see that file for the full rationale.
 */
public final class LegacyDetection {
    private static final Logger LOGGER = LoggerFactory.getLogger("CreRaces");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String MARKER_FILE = "creraces_legacy_migration.json";
    private static final String CLASSIC_MAPVARS_FILE = "creraces_mapvars.dat";

    private LegacyDetection() {}

    public static boolean classicWorldPresent(MinecraftServer server) {
        return classicWorldPresent(worldRoot(server));
    }

    // Path overloads exist because in singleplayer the decision is made client side, by
    // LegacyWorldLoadGate, before an integrated server object exists at all.
    public static boolean classicWorldPresent(Path worldRoot) {
        return Files.exists(worldRoot.resolve("data").resolve(CLASSIC_MAPVARS_FILE));
    }

    public static boolean alreadyHandled(MinecraftServer server) {
        return alreadyHandled(worldRoot(server));
    }

    public static boolean alreadyHandled(Path worldRoot) {
        return Files.exists(markerPath(worldRoot));
    }

    /** Returns the previously-written choice ("migrate"/"skip"), or null if never written. */
    @Nullable
    public static String readChoice(MinecraftServer server) {
        return readChoice(worldRoot(server));
    }

    @Nullable
    public static String readChoice(Path worldRoot) {
        Path path = markerPath(worldRoot);
        if (!Files.exists(path)) return null;
        try (Reader r = Files.newBufferedReader(path)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            return root != null && root.has("choice") ? root.get("choice").getAsString() : null;
        } catch (Exception e) {
            LOGGER.warn("Failed to read legacy migration marker: {}", e.getMessage());
            return null;
        }
    }

    public static void writeMarker(MinecraftServer server, String choice) {
        writeMarker(worldRoot(server), choice);
    }

    public static void writeMarker(Path worldRoot, String choice) {
        Path path = markerPath(worldRoot);
        JsonObject root = new JsonObject();
        root.addProperty("handled", true);
        root.addProperty("choice", choice);
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) {
                GSON.toJson(root, w);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to write legacy migration marker: {}", e.getMessage());
        }
    }

    private static Path worldRoot(MinecraftServer server) {
        return server.getWorldPath(Objects.requireNonNull(LevelResource.ROOT));
    }

    private static Path markerPath(Path worldRoot) {
        return worldRoot.resolve(MARKER_FILE);
    }
}
