package mc.sayda.creraces.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mc.sayda.creraces.CreRaces;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Handles disk-based caching of remote documentation.
 */
public class DocCache {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ResourceLocation, String> CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static File cacheFile;
    private static boolean isDirty = false;

    public static void init(Path configDir) {
        cacheFile = configDir.resolve("creraces/cache")
                .resolve("wiki_docs.json").toFile();
        load();

        // Simple scheduled save every 30 seconds if dirty
        new java.util.Timer("DocCache-Saver", true).scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                if (isDirty) {
                    save();
                    isDirty = false;
                }
            }
        }, 30000, 30000);
    }

    public static void store(ResourceLocation id, String content) {
        if (content == null)
            return;
        CACHE.put(id, content);
        isDirty = true;
    }

    public static void clear() {
        CACHE.clear();
        isDirty = false;
        if (cacheFile != null && cacheFile.exists()) {
            cacheFile.delete();
        }
        CreRaces.LOGGER.info("Remote documentation cache cleared.");
    }

    public static String get(ResourceLocation id) {
        return CACHE.get(id);
    }

    private static void load() {
        if (cacheFile == null || !cacheFile.exists())
            return;

        try (FileReader reader = new FileReader(cacheFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            json.entrySet().forEach(entry -> {
                @javax.annotation.Nullable String val = entry.getValue() != null && !entry.getValue().isJsonNull() ? entry.getValue().getAsString() : null;
                if (val != null) {
                    CACHE.put(new ResourceLocation(entry.getKey()), val);
                }
            });
        } catch (IOException e) {
            CreRaces.LOGGER.error("Failed to load doc cache: {}", e.getMessage());
        }
    }

    private static void save() {
        if (cacheFile == null)
            return;

        if (!cacheFile.getParentFile().exists()) {
            cacheFile.getParentFile().mkdirs();
        }

        try (FileWriter writer = new FileWriter(cacheFile)) {
            JsonObject json = new JsonObject();
            CACHE.forEach((id, content) -> json.addProperty(id.toString(), content));
            GSON.toJson(json, writer);
        } catch (IOException e) {
            CreRaces.LOGGER.error("Failed to save doc cache: {}", e.getMessage());
        }
    }
}
