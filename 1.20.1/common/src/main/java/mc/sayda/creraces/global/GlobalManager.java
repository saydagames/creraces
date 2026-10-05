package mc.sayda.creraces.global;

import com.google.gson.JsonElement;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Handles loading of global event handlers from JSON files in data/creraces/globals/.
 * Server-side only: unlike races/abilities/quests, nothing here needs client sync.
 */
public class GlobalManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final String FOLDER = "globals";

    @Override
    @Nonnull
    protected Map<ResourceLocation, JsonElement> prepare(@Nonnull ResourceManager resourceManager,
            @Nonnull ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> files = GsonHelper.getJsonFiles(resourceManager, FOLDER);
        return files != null ? files : new HashMap<>();
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> data, @Nonnull ResourceManager resourceManager,
            @Nonnull ProfilerFiller profiler) {
        GlobalRegistry.clear();
        GlobalDispatcher.clearFailureLog();

        // Sorted so handler execution order is deterministic across reloads - getJsonFiles
        // returns a HashMap, and handler order is observable (guarded dispatch runs them in
        // sequence).
        Map<ResourceLocation, JsonElement> sorted = new TreeMap<>(data);
        int count = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : sorted.entrySet()) {
            try {
                GlobalHandler handler = GlobalHandler.fromJson(entry.getKey(), entry.getValue().getAsJsonObject());
                GlobalRegistry.register(handler);
                count++;
            } catch (Exception e) {
                CreRaces.LOGGER.error("Failed to load global handler {}: {}", entry.getKey(), e.getMessage());
            }
        }

        CreRaces.LOGGER.info("GlobalManager: loaded {} global handlers", count);
    }
}
