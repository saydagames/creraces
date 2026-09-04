package mc.sayda.creraces.forge.migration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.util.WorldDataPaths;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import javax.annotation.Nullable;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Detects a leftover CreRaces Classic world (Forge SavedData files this rewrite never writes)
 * and tracks whether the one-time migration prompt has already been resolved for this world.
 *
 * Deliberately its own small plain-text JSON file (<world>/creraces/migration.json), not folded
 * into a generic settings store or vanilla's binary SavedData, a server owner should be able to
 * force a re-run just by deleting one obviously-named file, without needing to hand-edit a blob
 * shared with unrelated settings or an opaque .dat file.
 *
 * Path-based overloads exist because for singleplayer, the decision is made client-side (see
 * LegacyWorldLoadGate) before an integrated server object even exists.
 */
public final class LegacyDetection {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String MARKER_FILE = "migration.json";
    /** Classic's MapVariables SavedData, its mere presence proves Classic was once loaded here. */
    private static final String CLASSIC_MAPVARS_FILE = "creraces_mapvars.dat";

    private LegacyDetection() {}

    public static boolean classicWorldPresent(MinecraftServer server) {
        return classicWorldPresent(worldRoot(server));
    }

    public static boolean classicWorldPresent(Path worldRoot) {
        return Files.exists(worldRoot.resolve("data").resolve(CLASSIC_MAPVARS_FILE));
    }

    public static boolean alreadyHandled(MinecraftServer server) {
        return readChoice(server) != null;
    }

    public static boolean alreadyHandled(Path worldRoot) {
        return readChoice(worldRoot) != null;
    }

    /** Returns the previously-written choice ("migrate"/"skip"), or null if never written. */
    @Nullable
    public static String readChoice(MinecraftServer server) {
        return readChoiceFromFile(WorldDataPaths.resolve(server, MARKER_FILE));
    }

    @Nullable
    public static String readChoice(Path worldRoot) {
        return readChoiceFromFile(WorldDataPaths.resolve(worldRoot, MARKER_FILE));
    }

    @Nullable
    private static String readChoiceFromFile(Path markerPath) {
        if (!Files.exists(markerPath)) return null;
        try (Reader r = Files.newBufferedReader(markerPath)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            return root != null && root.has("choice") ? root.get("choice").getAsString() : null;
        } catch (Exception e) {
            CreRaces.LOGGER.warn("[CreRaces] Failed to read legacy migration marker: {}", e.getMessage());
            return null;
        }
    }

    /** choice: "migrate" or "skip". Never called for the abort option, that path must re-prompt next time. */
    public static void writeMarker(MinecraftServer server, String choice) {
        writeMarkerToFile(WorldDataPaths.resolve(server, MARKER_FILE), choice);
    }

    public static void writeMarker(Path worldRoot, String choice) {
        writeMarkerToFile(WorldDataPaths.resolve(worldRoot, MARKER_FILE), choice);
    }

    private static void writeMarkerToFile(Path markerPath, String choice) {
        JsonObject root = new JsonObject();
        root.addProperty("handled", true);
        root.addProperty("choice", choice);
        try (Writer w = Files.newBufferedWriter(markerPath)) {
            GSON.toJson(root, w);
        } catch (Exception e) {
            CreRaces.LOGGER.error("[CreRaces] Failed to write legacy migration marker: {}", e.getMessage());
        }
    }

    private static Path worldRoot(MinecraftServer server) {
        return server.getWorldPath(Objects.requireNonNull(LevelResource.ROOT));
    }
}
