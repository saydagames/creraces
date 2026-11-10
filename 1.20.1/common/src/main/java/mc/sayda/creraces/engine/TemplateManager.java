package mc.sayda.creraces.engine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads action templates from data/&lt;namespace&gt;/templates/*.json: reusable action lists that
 * the template action runs with its own arguments. Each file is { "actions": [...] }. Any string in
 * it may hold "$(name)" placeholders; a string that is exactly "$(name)" takes the argument's JSON
 * value whatever its type, so numbers and objects can be passed too.
 */
public class TemplateManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final String FOLDER = "templates";
    private static final Map<ResourceLocation, JsonObject> TEMPLATES = new HashMap<>();
    /** Bumped on every reload, so template actions know to re-read their template. */
    private static int generation;

    @Nullable
    public static JsonObject get(ResourceLocation id) {
        return TEMPLATES.get(id);
    }

    public static int generation() {
        return generation;
    }

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
        TEMPLATES.clear();
        data.forEach((id, json) -> {
            if (json.isJsonObject() && json.getAsJsonObject().has("actions")) {
                TEMPLATES.put(id, json.getAsJsonObject());
            } else {
                CreRaces.LOGGER.error("Template {} needs an \"actions\" list", id);
            }
        });
        generation++;
        CreRaces.LOGGER.info("TemplateManager: loaded {} action templates", TEMPLATES.size());
    }
}
