package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import net.minecraft.resources.ResourceLocation;

/** Ids for traits that keep per-player state (timers, cooldowns, flags) keyed by the trait. */
final class TraitIds {
    private TraitIds() {
    }

    /**
     * The trait's "id" as a resource location, in the creraces namespace unless it names one.
     * TraitRegistry fills in a position-based id when the JSON has none.
     */
    static ResourceLocation of(JsonObject json) {
        String id = json.get("id").getAsString();
        ResourceLocation location = ResourceLocation.tryParse(id.contains(":") ? id : CreRaces.MODID + ":" + id);
        if (location == null) {
            throw new IllegalArgumentException("trait id '" + id + "' is not a valid id");
        }
        return location;
    }
}
