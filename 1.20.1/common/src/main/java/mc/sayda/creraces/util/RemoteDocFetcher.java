package mc.sayda.creraces.util;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared utility for fetching and caching remote documentation as UI
 * Components.
 */
public class RemoteDocFetcher {
    private static final Map<ResourceLocation, Component> FETCHED_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Component> PASSIVE_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Component> FULL_CACHE = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> FETCHING_SET = ConcurrentHashMap.newKeySet();

    /**
     * Clears all memory caches.
     */
    public static void clearCache() {
        FETCHED_CACHE.clear();
        PASSIVE_CACHE.clear();
        FULL_CACHE.clear();
        FETCHING_SET.clear();
    }

    /**
     * Gets a remote description, triggering a fetch if not already cached.
     * 
     * @param id       The ResourceLocation of the object (race or ability).
     * @param config   The remote doc configuration.
     * @param fallback The local fallback component.
     * @return The cached component, a loading component, or the fallback.
     */
    public static Component getRemoteDescription(ResourceLocation id, RemoteDocConfig config, Component fallback) {
        return getRemote(id, config, fallback, FETCHED_CACHE, "");
    }

    /**
     * Gets a remote passive, triggering a fetch if not already cached.
     */
    public static Component getRemotePassive(ResourceLocation id, RemoteDocConfig config, Component fallback) {
        return getRemote(id, config, fallback, PASSIVE_CACHE, "_passive");
    }

    /**
     * Gets a remote full description, triggering a fetch if not already cached.
     */
    public static Component getRemoteFullDescription(ResourceLocation id, RemoteDocConfig config, Component fallback) {
        return getRemote(id, config, fallback, FULL_CACHE, "_full");
    }

    private static Component getRemote(ResourceLocation id, RemoteDocConfig config, Component fallback,
            Map<ResourceLocation, Component> cache, String cacheSuffix) {
        if (config == null || config.source().isEmpty())
            return fallback;

        // Check memory cache
        Component memoryCached = cache.get(id);
        if (memoryCached != null) {
            return memoryCached;
        }

        // Check disk cache using a modified ID if suffix provided
        ResourceLocation cacheId = cacheSuffix.isEmpty() ? id
                : new ResourceLocation(id.getNamespace(), id.getPath() + cacheSuffix);
        String diskCached = DocCache.get(cacheId);
        if (diskCached != null) {
            Component comp = WikitextUtil.toComponent(diskCached);
            cache.put(id, comp);
            return comp;
        }

        // Trigger fetch if not already fetching
        if (!FETCHING_SET.contains(cacheId)) {
            FETCHING_SET.add(cacheId);
            DocFetcher.fetch(config.source(), config.selector())
                    .handle((result, ex) -> {
                        try {
                            if (result != null && !result.isEmpty()) {
                                // Store in disk cache and memory cache as formatted component
                                DocCache.store(cacheId, result);
                                cache.put(id, WikitextUtil.toComponent(result));
                            } else {
                                // Use fallback translation key from config if available, else original
                                // component
                                if (config != null && config.fallback() != null && !config.fallback().isEmpty()) {
                                    cache.put(id, Component.translatable(config.fallback()));
                                } else {
                                    cache.put(id, fallback);
                                }
                            }
                        } finally {
                            FETCHING_SET.remove(cacheId);
                        }
                        return null;
                    });
        }

        return Component.translatable("gui.creraces.loading");
    }
}
