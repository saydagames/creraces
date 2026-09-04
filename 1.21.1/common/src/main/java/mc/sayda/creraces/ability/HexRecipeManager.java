package mc.sayda.creraces.ability;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HexRecipeManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final String FOLDER = "research_recipes";
    private static final List<HexRecipe> RECIPES = new ArrayList<>();

    @Override
    @Nonnull
    protected Map<ResourceLocation, JsonElement> prepare(@Nonnull ResourceManager rm, @Nonnull ProfilerFiller p) {
        Map<ResourceLocation, JsonElement> files = mc.sayda.creraces.util.GsonHelper.getJsonFiles(rm, FOLDER);
        return files != null ? files : new HashMap<>();
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> data, @Nonnull ResourceManager rm,
            @Nonnull ProfilerFiller p) {
        RECIPES.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            try {
                JsonObject obj = entry.getValue().getAsJsonObject();
                ResourceLocation ability = ResourceLocation.parse(GsonHelper.getAsString(obj, "ability"));
                int level = obj.has("level") ? obj.get("level").getAsInt() : 1;

                JsonArray patternArr = obj.getAsJsonArray("pattern");
                Map<HexPos, EssenceType> rawPattern = new HashMap<>();
                for (JsonElement el : patternArr) {
                    JsonObject cell = el.getAsJsonObject();
                    int q = cell.get("q").getAsInt();
                    int r = cell.get("r").getAsInt();
                    EssenceType essence = EssenceType.byId(cell.get("essence").getAsString());
                    rawPattern.put(new HexPos(q, r), essence);
                }

                boolean positioned = obj.has("positioned") && obj.get("positioned").getAsBoolean();
                Map<HexPos, EssenceType> storedPattern = positioned ? rawPattern : HexRecipe.normalize(rawPattern);
                RECIPES.add(new HexRecipe(ability, level, storedPattern, positioned));
                CreRaces.LOGGER.debug("HexRecipeManager: loaded recipe for {}", ability);
            } catch (Exception e) {
                CreRaces.LOGGER.warn("HexRecipeManager: failed to parse {}: {}", entry.getKey(), e.getMessage());
            }
        }
        CreRaces.LOGGER.info("HexRecipeManager: loaded {} hex recipes", RECIPES.size());
    }

    public static Optional<HexRecipe> match(Map<HexPos, EssenceType> grid) {
        if (grid.isEmpty()) return Optional.empty();
        return RECIPES.stream().filter(r -> r.matches(grid)).findFirst();
    }

    public static Optional<HexRecipe> findByAbility(ResourceLocation abilityId) {
        if (abilityId == null) return Optional.empty();
        return RECIPES.stream().filter(r -> abilityId.equals(r.ability())).findFirst();
    }
}
