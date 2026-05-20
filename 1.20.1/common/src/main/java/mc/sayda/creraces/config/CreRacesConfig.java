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
    public static Supplier<Boolean> MINI_BLOCK_WATER_RESISTANT = () -> true;
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
    public static Supplier<Double> POCKET_BOUNDARY = () -> 450.0;
    public static Supplier<Integer> POCKET_EXPANSION_LIMIT = () -> 8;
    public static Supplier<Double> POCKET_EXPANSION_COST = () -> 200.0;
    public static Supplier<Integer> POCKET_INVITE_MAX = () -> -1;

    // [SECTION: ENTITY & OTHER]
    public static Supplier<Double> ENTITY_FEATHER_DAMAGE = () -> 2.0;
    public static Supplier<Double> ENTITY_FEATHER_GRAVITY = () -> 0.05;
    public static Supplier<Double> ENTITY_POISON_EMITTER_HEALTH = () -> 16.0;
    public static Supplier<Double> ENTITY_POISON_EMITTER_ARMOR = () -> 16.0;
    public static Supplier<Double> ENTITY_POISON_EMITTER_FOLLOW_RANGE = () -> 16.0;
    public static Supplier<Double> ENTITY_POISON_EMITTER_KNOCKBACK_RES = () -> 1.0;
    public static Supplier<Double> ENTITY_POISON_EMITTER_MOBILE_MAX_HEALTH = () -> 16.0;
    public static Supplier<Double> ENTITY_POISON_EMITTER_MOBILE_MOVEMENT_SPEED = () -> 0.35;
    public static Supplier<Double> ENTITY_POISON_EMITTER_RADIUS = () -> 5.5;
    public static Supplier<Integer> ENTITY_POISON_EMITTER_LIFETIME_TICKS = () -> 2400; // 2 minutes

    // [SECTION: POTION EFFECTS]
    public static Supplier<Double> RAT_VENOM_SCALING = () -> 0.05; // from 0.2
    public static Supplier<Double> BOILING_SCALING = () -> 5.0 / 300.0;
    public static Supplier<Double> BLEEDING_SCALING = () -> 3.0 / 300.0;

    public static Supplier<Double> ENTITY_TORNADO_HEALTH = () -> 20.0;
    public static Supplier<Double> ENTITY_TORNADO_ARMOR = () -> 5.0;
    public static Supplier<Double> ENTITY_TORNADO_ATTACK_DAMAGE = () -> 3.0;
    public static Supplier<Double> ENTITY_TORNADO_FLYING_SPEED = () -> 0.1;
    public static Supplier<Double> ENTITY_TORNADO_MOVEMENT_SPEED = () -> 0.3;
    public static Supplier<Integer> ENTITY_TORNADO_LIFETIME_TICKS = () -> 2400;
    public static Supplier<Float> ENTITY_TORNADO_RADIUS = () -> 2.5f;
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
    public static Supplier<Integer> MAX_RAT_TUNNELS = () -> 1;
    public static Supplier<Integer> BREAK_BLOCKS_MAX_RADIUS = () -> -1;
    public static Supplier<Double> REMOVE_BLOCK_HARDNESS_LIMIT = () -> -1.0;
    public static Supplier<Integer> AOE_MAX_RADIUS = () -> -1;
    public static Supplier<Integer> BEAM_MAX_LENGTH = () -> -1;
    public static Supplier<Integer> GIVE_ITEM_MAX_COUNT = () -> -1;
    public static Supplier<Integer> MASS_SUMMON_MAX_COUNT = () -> -1;
    public static Supplier<Integer> DELAY_ACTION_MAX_TICKS = () -> -1;
    public static Supplier<Integer> ENTITY_DATA_KEY_MAX_LENGTH = () -> 64;
    public static Supplier<Boolean> FORCED_SELECTION = () -> true;
    public static Supplier<Integer> MINI_MODEL_CACHE_SIZE = () -> 128;
    public static Supplier<Integer> MINI_DUMMY_CACHE_SIZE = () -> 32;
    public static Supplier<Integer> SPIRIT_SPAWN_CHECK_RADIUS = () -> 16;
    public static Supplier<Integer> CUSTOMIZATION_VALUE_MAX_LENGTH = () -> 128;
    public static Supplier<Integer> NETWORK_TEAM_NAME_MAX_LEN = () -> 16;
    public static Supplier<Integer> TEAM_MAX_SIZE = () -> 16;
    public static Supplier<Integer> DOUBLE_JUMP_COOLDOWN_TICKS = () -> 10;
    public static Supplier<Double> RACIAL_AD_MULTIPLIER = () -> 0.002;
    public static Supplier<Long> RESOURCE_DECAY_GRACE_PERIOD = () -> 400L;
    public static Supplier<Integer> PASSIVE_EXECUTION_INTERVAL = () -> 20;
    public static Supplier<Double> SUNLIGHT_EQUIPMENT_BREAK_CHANCE = () -> 0.01;
    public static Supplier<Integer> SUNLIGHT_BURN_SECONDS = () -> 5;
    public static Supplier<Integer> PASSIVE_EFFECT_BUFFER_TICKS = () -> 20;
    public static Supplier<Integer> PASSIVE_DEFAULT_MAX_FOOD = () -> 20;
    public static Supplier<Double> PASSIVE_DEFAULT_MAX_SATURATION = () -> 20.0;
    public static Supplier<Double> RACE_SOCIAL_DEFENSE_RANGE = () -> 16.0;
    public static Supplier<String> DOC_CACHE_DIR = () -> "creraces/cache";
    public static Supplier<String> DOC_CACHE_FILENAME = () -> "wiki_docs.json";
    public static Supplier<Integer> DOC_FETCH_TIMEOUT_SECONDS = () -> 10;
    public static Supplier<Boolean> DISABLE_REMOTE_DOCS = () -> false;
    public static Supplier<String> WIKI_PAGE_PATH = () -> "wiki";
    public static Supplier<String> WIKI_ABILITY_NAMESPACE = () -> "Ability";
    public static Supplier<String> WIKI_BASE_URL = () -> "https://creraces.wiki.gg";
    public static Supplier<String> WIKI_API_BASE = () -> "https://creraces.wiki.gg";
    public static Supplier<Boolean> COIN_DROP_ENABLED = () -> true;
    public static Supplier<Boolean> SAG_WINGS = () -> true;
    public static Supplier<Boolean> GSTATE_ENABLED = () -> true;
    /** Equip race-specific visual addons (body parts) on race selection. */
    public static Supplier<Boolean> RACE_ADDONS_ENABLED = () -> true;
    /**
     * Equip lore-style addons (female body, chest etc.) in addition to race addons.
     * Requires RACE_ADDONS_ENABLED.
     */
    public static Supplier<Boolean> LORE_ADDONS_ENABLED = () -> true;
    public static Supplier<Integer> RITUAL_MODE = () -> 2;
    public static Supplier<Integer> VISUAL_SYNC_DISTANCE = () -> 64;
    public static Supplier<Integer> RACE_OVERLAY_OFFSET_X = () -> 0;
    public static Supplier<Integer> RACE_OVERLAY_OFFSET_Y = () -> 0;
    public static Supplier<Boolean> RACE_OVERLAYS_ENABLED = () -> true;
    public static Supplier<Boolean> ENGINE_POPUPS_ENABLED = () -> true;
    public static Supplier<Double> REMAINS_HEALTH = () -> 10.0;
    public static Supplier<Integer> REMAINS_DECAY_TIME = () -> 1200;
    public static Supplier<Double> MAX_SOUL = () -> 9.0;

    // [SECTION: RESOURCE DEFAULTS]
    public static Supplier<Double> DEFAULT_MAX_MANA = () -> 500.0;
    public static Supplier<Double> DEFAULT_MAX_ENERGY = () -> 200.0;
    public static Supplier<Double> DEFAULT_MAX_RAGE = () -> 100.0;
    public static Supplier<Double> DEFAULT_MAX_GRIT = () -> 100.0;
    /** Path to look for race/ability JSONs on the local filesystem (dev only). */
    public static Supplier<String> DEVELOPER_RESOURCE_PATH = () -> "";
}
