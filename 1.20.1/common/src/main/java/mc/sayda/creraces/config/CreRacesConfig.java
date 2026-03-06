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
    public static Supplier<Double> ABILITY_HASTE_CAP = () -> 40.0;

    // Mini Build System — disabled by default for compatibility
    public static Supplier<Boolean> MINI_BUILD_ENABLED = () -> false;
    public static Supplier<Boolean> MINI_FURNACE_ENABLED = () -> true;
    public static Supplier<Integer> MINI_MODEL_CACHE_SIZE = () -> 256;
    public static Supplier<Integer> MINI_DUMMY_CACHE_SIZE = () -> 64;
    public static Supplier<Double> MINI_CRAFTING_DISTANCE_SQR = () -> 64.0;
    public static Supplier<Long> MINI_PLACEMENT_SPAM_THRESHOLD_MS = () -> 50L;
    public static Supplier<Float> POISON_EMITTER_RADIUS = () -> 5.5f;
    public static Supplier<Integer> POISON_EMITTER_PULSE_INTERVAL = () -> 120;
    public static Supplier<Integer> POISON_EMITTER_LIFETIME_TICKS = () -> 600;
    public static Supplier<Integer> TROLL_PILLAR_LIFETIME_TICKS = () -> 1200;
    public static Supplier<Float> TROLL_PILLAR_CURSE_RADIUS = () -> 5.0f;
    public static Supplier<Integer> TROLL_PILLAR_CURSE_DURATION = () -> 120;

    // Engine Safety Caps
    // 0 = disabled (no cap applied). These protect against extremely large
    // radius/size values
    // coming from race JSONs that could freeze or crash the server.
    /** Max radius for creraces:break_blocks. Default 16. 0 = no cap. */
    public static Supplier<Integer> BREAK_BLOCKS_MAX_RADIUS = () -> 16;
    /**
     * Max radius for creraces:aoe and similar radius-based actions. Default 64. 0 =
     * no cap.
     */
    public static Supplier<Integer> AOE_MAX_RADIUS = () -> 64;
    /** Max length for creraces:beam. Default 64. 0 = no cap. */
    public static Supplier<Integer> BEAM_MAX_LENGTH = () -> 64;
    /**
     * Max character length for creraces:modify_entity_data keys. Default 64. 0 = no
     * cap.
     */
    public static Supplier<Integer> ENTITY_DATA_KEY_MAX_LENGTH = () -> 64;

}
