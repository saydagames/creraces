package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import net.minecraft.resources.ResourceLocation;

/** Ids for traits that keep per-player state (timers, flags) keyed by the trait. */
final class TraitIds {
    private TraitIds() {
    }

    /**
     * The JSON "name" when given, else an id derived from the trait JSON itself. A derived id
     * changes whenever that JSON is edited, which orphans any state stored under the old one.
     */
    static ResourceLocation fromJson(JsonObject json, String fallbackPrefix) {
        String name = json.has("name") ? json.get("name").getAsString()
                : fallbackPrefix + Math.abs(json.toString().hashCode());
        return new ResourceLocation(CreRaces.MODID, name);
    }
}
