package mc.sayda.creraces.ability;

import net.minecraft.resources.ResourceLocation;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static registry for all loaded abilities.
 * Populated by AbilityManager during resource reload.
 */
public class AbilityRegistry {
    private static final Map<ResourceLocation, Ability> ABILITIES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, mc.sayda.creraces.util.RemoteDocConfig> REMOTE_DOCS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, mc.sayda.creraces.util.RemoteDocConfig> REMOTE_FULL_DOCS = new ConcurrentHashMap<>();

    public static void register(Ability ability) {
        ABILITIES.put(ability.id(), ability);
    }

    public static Ability get(ResourceLocation id) {
        return ABILITIES.get(id);
    }

    public static Collection<Ability> getAll() {
        return Collections.unmodifiableCollection(ABILITIES.values());
    }

    public static void clear() {
        ABILITIES.clear();
        REMOTE_DOCS.clear();
        REMOTE_FULL_DOCS.clear();
    }

    public static void registerRemoteDoc(ResourceLocation id, mc.sayda.creraces.util.RemoteDocConfig config) {
        REMOTE_DOCS.put(id, config);
    }

    public static mc.sayda.creraces.util.RemoteDocConfig getRemoteDoc(ResourceLocation id) {
        return REMOTE_DOCS.get(id);
    }

    public static void registerRemoteFullDoc(ResourceLocation id, mc.sayda.creraces.util.RemoteDocConfig config) {
        REMOTE_FULL_DOCS.put(id, config);
    }

    public static mc.sayda.creraces.util.RemoteDocConfig getRemoteFullDoc(ResourceLocation id) {
        return REMOTE_FULL_DOCS.get(id);
    }

    public static boolean isEmpty() {
        return ABILITIES.isEmpty();
    }
}
