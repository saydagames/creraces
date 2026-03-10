package mc.sayda.creraces.config;

import java.util.function.Supplier;

public class CreRacesConfig {
    // [SECTION: COMMON]
    public static Supplier<Boolean> MINI_BUILD_ENABLED = () -> true;
    public static Supplier<Boolean> MINIBUILD_REQUIRES_LEARNED = () -> true;
    public static Supplier<Boolean> MINI_FURNACE_ENABLED = () -> true;
    public static Supplier<Boolean> MINI_PLACE_WHITELIST_ENABLED = () -> true;
    public static Supplier<Double> MINI_CRAFTING_DISTANCE_SQR = () -> 64.0;
    public static Supplier<Long> MINI_PLACEMENT_SPAM_THRESHOLD_MS = () -> 50L;
    public static Supplier<Double> MINI_BLOCK_REACH_MARGIN = () -> 1.5;
    public static Supplier<Integer> MICRO_BLOCK_LIGHT_PER_TORCH = () -> 5;
    public static Supplier<Integer> MICRO_BLOCK_MAX_LIGHT = () -> 15;

    // [SECTION: POCKET DIMENSION]
    public static Supplier<String> ACTION_DEFAULT_POCKET_DIM = () -> "creraces:pocket";
    public static Supplier<String> ACTION_DEFAULT_POCKET_STRUCTURE = () -> "creraces:dryad_box_1";
    public static Supplier<Double> ACTION_DEFAULT_POCKET_SPAWN_X_OFFSET = () -> 6.5;
    public static Supplier<Double> ACTION_DEFAULT_POCKET_SPAWN_Y_OFFSET = () -> 2.0;
    public static Supplier<Double> ACTION_DEFAULT_POCKET_SPAWN_Z_OFFSET = () -> 6.5;
    public static Supplier<Double> POCKET_DIM_SPACING = () -> 1000.0;
    public static Supplier<Double> POCKET_DIM_Y = () -> 100.0;
    public static Supplier<Integer> POCKET_EXPANSION_LIMIT = () -> 5;
    public static Supplier<Double> POCKET_EXPANSION_COST = () -> 200.0;

    // [SECTION: ENTITY & OTHER]
    public static Supplier<Double> ENTITY_FEATHER_DAMAGE = () -> 2.0;
    public static Supplier<Double> ENTITY_FEATHER_GRAVITY = () -> 0.05;
    public static Supplier<Double> ENTITY_POISON_EMITTER_HEALTH = () -> 10.0;
    public static Supplier<Double> ENTITY_POISON_EMITTER_ARMOR = () -> 5.0;
    public static Supplier<Double> ENTITY_POISON_EMITTER_FOLLOW_RANGE = () -> 16.0;
    public static Supplier<Double> ENTITY_POISON_EMITTER_KNOCKBACK_RES = () -> 1.0;
    public static Supplier<Double> ENTITY_POISON_EMITTER_MOBILE_MAX_HEALTH = () -> 16.0;
    public static Supplier<Double> ENTITY_POISON_EMITTER_MOBILE_MOVEMENT_SPEED = () -> 0.35;
    public static Supplier<Integer> ENTITY_POISON_EMITTER_PULSE_INTERVAL = () -> 20;
    public static Supplier<Double> ENTITY_POISON_EMITTER_RADIUS = () -> 3.0;
    public static Supplier<Integer> ENTITY_POISON_EMITTER_VENOM_DURATION = () -> 102;
    public static Supplier<Integer> ENTITY_POISON_EMITTER_LIFETIME_TICKS = () -> 100;
    public static Supplier<Double> ENTITY_TORNADO_HEALTH = () -> 20.0;
    public static Supplier<Double> ENTITY_TORNADO_ARMOR = () -> 5.0;
    public static Supplier<Double> ENTITY_TORNADO_ATTACK_DAMAGE = () -> 3.0;
    public static Supplier<Double> ENTITY_TORNADO_FLYING_SPEED = () -> 0.1;
    public static Supplier<Double> ENTITY_TORNADO_MOVEMENT_SPEED = () -> 0.3;
    public static Supplier<Integer> ENTITY_TORNADO_LIFETIME_TICKS = () -> 200;
    public static Supplier<Float> ENTITY_TORNADO_RADIUS = () -> 4.0f;
    public static Supplier<Integer> ENTITY_TORNADO_DIZZINESS_DURATION = () -> 60;
    public static Supplier<Double> ENTITY_TORNADO_PULL_FORCE = () -> 0.05;
    public static Supplier<Double> ENTITY_TORNADO_FOLLOW_RANGE = () -> 16.0;
    public static Supplier<Double> ENTITY_TROLL_PILLAR_MAX_HEALTH = () -> 40.0;
    public static Supplier<Double> ENTITY_TROLL_PILLAR_ARMOR = () -> 10.0;
    public static Supplier<Double> ENTITY_TROLL_PILLAR_FOLLOW_RANGE = () -> 16.0;
    public static Supplier<Double> ENTITY_TROLL_PILLAR_KNOCKBACK_RES = () -> 1.0;
    public static Supplier<Double> ENTITY_TROLL_PILLAR_CURSE_RADIUS = () -> 5.0;
    public static Supplier<Integer> ENTITY_TROLL_PILLAR_PULSE_INTERVAL = () -> 20;
    public static Supplier<Integer> ENTITY_TROLL_PILLAR_CURSE_DURATION = () -> 100;
    public static Supplier<Integer> ENTITY_TROLL_PILLAR_LIFETIME_TICKS = () -> 600;
    public static Supplier<Double> ABILITY_HASTE_CAP = () -> 40.0;
    public static Supplier<Integer> BREAK_BLOCKS_MAX_RADIUS = () -> 16;
    public static Supplier<Integer> AOE_MAX_RADIUS = () -> 64;
    public static Supplier<Integer> BEAM_MAX_LENGTH = () -> 64;
    public static Supplier<Integer> ENTITY_DATA_KEY_MAX_LENGTH = () -> 64;
    public static Supplier<Boolean> FORCED_SELECTION = () -> false;
    public static Supplier<Integer> MINI_MODEL_CACHE_SIZE = () -> 128;
    public static Supplier<Integer> MINI_DUMMY_CACHE_SIZE = () -> 32;
    public static Supplier<Integer> SPIRIT_REALM_TINT_COLOR = () -> 0x4400FF;
    public static Supplier<Float> SPIRIT_REALM_MOON_ALPHA = () -> 0.5f;
    public static Supplier<Float> SPIRIT_REALM_MOON_SIZE = () -> 20.0f;
    public static Supplier<Integer> SPIRIT_SPAWN_CHECK_RADIUS = () -> 16;
    public static Supplier<Integer> CUSTOMIZATION_VALUE_MAX_LENGTH = () -> 128;
    public static Supplier<Integer> NETWORK_BUFFER_MAX_UTF_LEN = () -> 32767;
    public static Supplier<Integer> NETWORK_TEAM_NAME_MAX_LEN = () -> 16;
    public static Supplier<Integer> NETWORK_PLAYER_NAME_MAX_LEN = () -> 16;
    public static Supplier<Double> RACIAL_AD_MULTIPLIER = () -> 1.0;
    public static Supplier<Double> RESOURCE_MIN_CAPACITY = () -> 10.0;
    public static Supplier<Boolean> ATTRIBUTE_RECHECK_ENABLED = () -> true; // TODO: Evaluate usefulness
    public static Supplier<Integer> TICK_AUTHORITATIVE_SYNC_INTERVAL = () -> 100;
    public static Supplier<Integer> PASSIVE_EXECUTION_INTERVAL = () -> 20;
    public static Supplier<Double> SUNLIGHT_EQUIPMENT_BREAK_CHANCE = () -> 0.01;
    public static Supplier<Integer> SUNLIGHT_BURN_SECONDS = () -> 5;
    public static Supplier<Integer> PASSIVE_EFFECT_BUFFER_TICKS = () -> 20;
    public static Supplier<Integer> PASSIVE_DEFAULT_MAX_FOOD = () -> 20;
    public static Supplier<Double> PASSIVE_DEFAULT_MAX_SATURATION = () -> 20.0;
    public static Supplier<Double> RACE_SOCIAL_DEFENSE_RANGE = () -> 16.0;
    public static Supplier<Double> MAX_STAT_CAP = () -> 1000.0;
    public static Supplier<Double> MAX_PERCENTAGE_CAP = () -> 10.0;
    public static Supplier<String> DOC_CACHE_DIR = () -> "creraces/cache";
    public static Supplier<String> DOC_CACHE_FILENAME = () -> "wiki_docs.json";
    public static Supplier<Integer> DOC_FETCH_TIMEOUT_SECONDS = () -> 10;
    public static Supplier<Boolean> DISABLE_REMOTE_DOCS = () -> false;
    public static Supplier<String> WIKI_PAGE_PATH = () -> "wiki";
    public static Supplier<String> WIKI_ABILITY_NAMESPACE = () -> "abilities";
    public static Supplier<String> WIKI_BASE_URL = () -> "https://wiki.saydagames.com";
    public static Supplier<String> WIKI_API_BASE = () -> "https://api.wiki.saydagames.com";
    public static Supplier<Boolean> HUMAN_SELECTION_ALLOWED = () -> true;
    public static Supplier<Integer> TEAM_INVITE_TIMEOUT_TICKS = () -> 600;
    public static Supplier<Boolean> COIN_DROP_ENABLED = () -> true;
    public static Supplier<Boolean> COIN_TRANSFER_ALLOWED = () -> true;
    public static Supplier<Boolean> SAG_WINGS_RAIN_FLIGHT = () -> true;
    public static Supplier<Boolean> GENDER_SYSTEM_ENABLED = () -> true;
    /** Equip race-specific visual addons (body parts) on race selection. */
    public static Supplier<Boolean> RACE_ADDONS_ENABLED = () -> true;
    /**
     * Equip lore-style addons (female body, chest etc.) in addition to race addons.
     * Requires RACE_ADDONS_ENABLED.
     */
    public static Supplier<Boolean> LORE_ADDONS_ENABLED = () -> true;
    public static Supplier<Boolean> RACE_FRIENDLY_MOBS_ENABLED = () -> true;
    public static Supplier<Integer> RITUAL_MODE = () -> 2;
    public static Supplier<Integer> VISUAL_SYNC_DISTANCE = () -> 64;
    public static Supplier<Integer> RACE_OVERLAY_OFFSET_X = () -> 0;
    public static Supplier<Integer> RACE_OVERLAY_OFFSET_Y = () -> 0;
    public static Supplier<Boolean> RACE_OVERLAYS_ENABLED = () -> true;
    public static Supplier<Boolean> ENGINE_POPUPS_ENABLED = () -> true;
    public static Supplier<Double> RACE_DEFAULT_HP_MOD = () -> 1.0;
    public static Supplier<Double> RACE_DEFAULT_HEIGHT_MOD = () -> 1.0;
    public static Supplier<Double> RACE_DEFAULT_WIDTH_MOD = () -> 1.0;
    public static Supplier<Double> UNDEAD_SUMMON_CAP = () -> 5.0;
    public static Supplier<Integer> REMAINS_DECAY_TIME = () -> 1200;
    public static Supplier<Double> DRYAD_BLESSING_COST = () -> 50.0;
    public static Supplier<Double> SCALING_DEFAULT_FACTOR = () -> 1.0;
}
