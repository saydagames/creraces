package mc.sayda.creraces.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles disk-based caching of remote documentation.
 */
public class DocCache {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ResourceLocation, String> CACHE = new ConcurrentHashMap<>();
    private static File cacheFile;
    private static volatile boolean isDirty = false;

    public static void init(Path configDir) {
        cacheFile = configDir.resolve(CreRacesConfig.DOC_CACHE_DIR.get())
                .resolve(CreRacesConfig.DOC_CACHE_FILENAME.get()).toFile();
        load();

        new Timer("DocCache-Saver", true).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isDirty) {
                    // Cleared before saving so a store() that lands mid-save is picked up next round.
                    isDirty = false;
                    save();
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
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                JsonElement value = entry.getValue();
                if (id != null && value != null && !value.isJsonNull()) {
                    CACHE.put(id, value.getAsString());
                }
            }
        } catch (IOException | RuntimeException e) {
            // A corrupt cache file only costs a re-fetch; it must not stop the mod from loading.
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
