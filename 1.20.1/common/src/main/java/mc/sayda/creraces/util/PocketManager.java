package mc.sayda.creraces.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the global registry of pockets.
 */
public class PocketManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SAVE_FILE_NAME = "creraces_pockets.json";
    private static final AtomicInteger NEXT_POCKET_INDEX = new AtomicInteger(1);

    public static int getNextIndex() {
        return NEXT_POCKET_INDEX.getAndIncrement();
    }

    public static void save(MinecraftServer server) {
        Path savePath = server.getWorldPath(Objects.requireNonNull(LevelResource.ROOT)).resolve(SAVE_FILE_NAME);
        JsonObject obj = new JsonObject();
        obj.addProperty("next_pocket_index", NEXT_POCKET_INDEX.get());

        try (Writer w = Files.newBufferedWriter(savePath)) {
            GSON.toJson(obj, w);
            CreRaces.LOGGER.info("Saved pocket registry to {}", savePath);
        } catch (IOException e) {
            CreRaces.LOGGER.error("Failed to save pocket registry: {}", e.getMessage());
        }
    }

    public static void load(MinecraftServer server) {
        Path savePath = server.getWorldPath(Objects.requireNonNull(LevelResource.ROOT)).resolve(SAVE_FILE_NAME);
        if (!Files.exists(savePath)) {
            NEXT_POCKET_INDEX.set(1);
            return;
        }

        try (Reader r = Files.newBufferedReader(savePath)) {
            JsonObject obj = GSON.fromJson(r, JsonObject.class);
            if (obj != null && obj.has("next_pocket_index")) {
                NEXT_POCKET_INDEX.set(obj.get("next_pocket_index").getAsInt());
            }
            CreRaces.LOGGER.info("Loaded pocket registry from {}", savePath);
        } catch (Exception e) {
            CreRaces.LOGGER.error("Failed to load pocket registry: {}", e.getMessage());
        }
    }
}
