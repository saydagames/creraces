package mc.sayda.creraces.util;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Fetches remote documentation and caches it, in memory and on disk, as UI Components. */
public class RemoteDocFetcher {
    private static final Map<ResourceLocation, Component> FETCHED_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Component> PASSIVE_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Component> FULL_CACHE = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> FETCHING_SET = ConcurrentHashMap.newKeySet();

    public static void clearCache() {
        FETCHED_CACHE.clear();
        PASSIVE_CACHE.clear();
        FULL_CACHE.clear();
        FETCHING_SET.clear();
    }

    /**
     * The remote description for a race or ability: the cached text if there is any, otherwise a
     * "loading" placeholder while a fetch runs in the background, or {@code fallback} when the
     * config has no source.
     */
    public static Component getRemoteDescription(ResourceLocation id, RemoteDocConfig config, Component fallback) {
        return getRemote(id, config, fallback, FETCHED_CACHE, "");
    }

    /** As {@link #getRemoteDescription}, for the passives section. */
    public static Component getRemotePassive(ResourceLocation id, RemoteDocConfig config, Component fallback) {
        return getRemote(id, config, fallback, PASSIVE_CACHE, "_passive");
    }

    /** As {@link #getRemoteDescription}, for the full description. */
    public static Component getRemoteFullDescription(ResourceLocation id, RemoteDocConfig config, Component fallback) {
        return getRemote(id, config, fallback, FULL_CACHE, "_full");
    }

    private static Component getRemote(ResourceLocation id, RemoteDocConfig config, Component fallback,
            Map<ResourceLocation, Component> cache, String cacheSuffix) {
        if (config == null || config.source().isEmpty())
            return fallback;

        Component memoryCached = cache.get(id);
        if (memoryCached != null) {
            return memoryCached;
        }

        // Each section is stored on disk under its own suffixed id.
        ResourceLocation cacheId = cacheSuffix.isEmpty() ? id
                : ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + cacheSuffix);
        String diskCached = DocCache.get(cacheId);
        if (diskCached != null) {
            Component comp = WikitextUtil.toComponent(diskCached);
            cache.put(id, comp);
            return comp;
        }

        if (FETCHING_SET.add(cacheId)) {
            DocFetcher.fetch(config.source(), config.selector())
                    .handle((result, ex) -> {
                        try {
                            if (result != null && !result.isEmpty()) {
                                DocCache.store(cacheId, result);
                                cache.put(id, WikitextUtil.toComponent(result));
                            } else if (config.fallback() != null && !config.fallback().isEmpty()) {
                                cache.put(id, Component.translatable(config.fallback()));
                            } else {
                                cache.put(id, fallback);
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
