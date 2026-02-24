package mc.sayda.creraces.config;

import java.util.function.Supplier;

/**
 * Common configuration for CreRaces.
 * Values are initialized with defaults and can be overridden by
 * platform-specific loaders.
 */
public class CreRacesConfig {

    // Wiki & Documentation
    public static Supplier<String> WIKI_BASE_URL = () -> "https://creraces.wiki.gg/";
    public static Supplier<String> WIKI_PAGE_PATH = () -> "wiki/";
    public static Supplier<String> WIKI_ABILITY_NAMESPACE = () -> "Ability:";
    public static Supplier<Boolean> DISABLE_REMOTE_DOCS = () -> false;
    public static Supplier<Integer> DOC_FETCH_TIMEOUT_SECONDS = () -> 10;
    public static Supplier<String> DOC_CACHE_DIR = () -> "cache";
    public static Supplier<String> DOC_CACHE_FILENAME = () -> "docs.json";

    // Gameplay & UI
    public static Supplier<Boolean> FORCED_SELECTION = () -> true;

}
