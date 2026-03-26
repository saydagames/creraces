package mc.sayda.creraces.race;

import net.minecraft.resources.ResourceLocation;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static registry for all loaded races.
 * Populated by RaceManager during resource reload.
 */
public class RaceRegistry {
    private static final Map<ResourceLocation, Race> RACES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, mc.sayda.creraces.util.RemoteDocConfig> REMOTE_DOCS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, mc.sayda.creraces.util.RemoteDocConfig> REMOTE_PASSIVES = new ConcurrentHashMap<>();

    // Default race if none selected (Human or None)
    public static final ResourceLocation NONE = new ResourceLocation("creraces", "none");
    public static final ResourceLocation HARPY = new ResourceLocation("creraces", "harpy");

    public static void register(Race race) {
        RACES.put(race.id(), race);
    }

    public static Race get(ResourceLocation id) {
        return RACES.get(id);
    }

    public static Collection<Race> getAll() {
        return Collections.unmodifiableCollection(RACES.values());
    }

    public static void clear() {
        RACES.clear();
        REMOTE_DOCS.clear();
        REMOTE_PASSIVES.clear();
    }

    public static void registerRemoteDoc(ResourceLocation id, mc.sayda.creraces.util.RemoteDocConfig config) {
        REMOTE_DOCS.put(id, config);
    }

    public static mc.sayda.creraces.util.RemoteDocConfig getRemoteDoc(ResourceLocation id) {
        return REMOTE_DOCS.get(id);
    }

    public static void registerRemotePassive(ResourceLocation id, mc.sayda.creraces.util.RemoteDocConfig config) {
        REMOTE_PASSIVES.put(id, config);
    }

    public static mc.sayda.creraces.util.RemoteDocConfig getRemotePassive(ResourceLocation id) {
        return REMOTE_PASSIVES.get(id);
    }

    public static Collection<Race> getRaces() {
        return getAll();
    }

    public static List<Race> getSubRaces(ResourceLocation parentId) {
        return RACES.values().stream()
                .filter(r -> r.selectable() && r.parentRaces().contains(parentId))
                .toList();
    }

    public static boolean isSelectableRoot(Race r) {
        if (!r.selectable())
            return false;
        if (r.parentRaces().isEmpty())
            return true;
        // If all parents are non-selectable, this race is a root entry
        return r.parentRaces().stream().allMatch(pId -> {
            Race p = get(pId);
            return p != null && !p.selectable();
        });
    }

    public static boolean isParent(ResourceLocation id) {
        return RACES.values().stream().anyMatch(r -> r.parentRaces().contains(id));
    }

    public static boolean exists(ResourceLocation id) {
        return RACES.containsKey(id);
    }

    public static java.util.Optional<Race> getRaceByIndex(double index) {
        return RACES.values().stream()
                .filter(r -> r.index() == index)
                .findFirst();
    }
}
