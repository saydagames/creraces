package mc.sayda.creraces.config.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import mc.sayda.creraces.config.CreRacesConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class FabricConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("creraces");

    public static void load() {
        if (!CONFIG_DIR.toFile().exists()) {
            CONFIG_DIR.toFile().mkdirs();
        }

        CommonConfig common = loadConfig("creraces-common.json", CommonConfig.class);
        ClientConfig client = loadConfig("creraces-client.json", ClientConfig.class);
        BalancingConfig balancing = loadConfig("creraces-balancing.json", BalancingConfig.class);

        apply(common, client, balancing);
    }

    private static <T> T loadConfig(String fileName, Class<T> clazz) {
        File file = CONFIG_DIR.resolve(fileName).toFile();
        T data;
        try {
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    data = GSON.fromJson(reader, clazz);
                    if (data == null)
                        data = clazz.getDeclaredConstructor().newInstance();
                }
            } else {
                data = clazz.getDeclaredConstructor().newInstance();
                saveConfig(fileName, data);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load CreRaces Fabric config: " + fileName, e);
            try {
                data = clazz.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return data;
    }

    private static void saveConfig(String fileName, Object data) {
        File file = CONFIG_DIR.resolve(fileName).toFile();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save CreRaces Fabric config: " + fileName, e);
        }
    }

    private static void apply(CommonConfig common, ClientConfig client, BalancingConfig balancing) {
        // Documentation
        CreRacesConfig.WIKI_BASE_URL = () -> common.documentation.wiki_base_url;
        CreRacesConfig.WIKI_PAGE_PATH = () -> common.documentation.wiki_page_path;
        CreRacesConfig.WIKI_ABILITY_NAMESPACE = () -> common.documentation.wiki_ability_namespace;
        CreRacesConfig.DISABLE_REMOTE_DOCS = () -> common.documentation.disable_remote_docs;
        CreRacesConfig.DOC_FETCH_TIMEOUT_SECONDS = () -> common.documentation.doc_fetch_timeout_seconds;
        CreRacesConfig.DOC_CACHE_DIR = () -> common.documentation.doc_cache_dir;
        CreRacesConfig.DOC_CACHE_FILENAME = () -> common.documentation.doc_cache_filename;
        CreRacesConfig.WIKI_API_BASE = () -> common.documentation.wiki_api_base;
        CreRacesConfig.DEVELOPER_RESOURCE_PATH = () -> common.documentation.developer_resource_path;

        // Gameplay
        CreRacesConfig.FORCED_SELECTION = () -> common.gameplay.forced_selection;
        CreRacesConfig.ABILITY_HASTE_CAP = () -> common.gameplay.ability_haste_cap;
        CreRacesConfig.RACIAL_AD_MULTIPLIER = () -> common.gameplay.racial_ad_multiplier;
        CreRacesConfig.RESOURCE_DECAY_GRACE_PERIOD = () -> common.gameplay.resource_decay_grace_period;
        CreRacesConfig.COIN_DROP_ENABLED = () -> common.gameplay.coin_drop_enabled;
        CreRacesConfig.SAG_WINGS = () -> common.gameplay.sag_wings;
        CreRacesConfig.GSTATE_ENABLED = () -> common.gameplay.gstate_enabled;
        CreRacesConfig.RITUAL_MODE = () -> common.gameplay.ritual_mode;
        CreRacesConfig.PASSIVE_EXECUTION_INTERVAL = () -> common.gameplay.passive_execution_interval;
        CreRacesConfig.SUNLIGHT_EQUIPMENT_BREAK_CHANCE = () -> common.gameplay.sunlight_equipment_break_chance;
        CreRacesConfig.SUNLIGHT_BURN_SECONDS = () -> common.gameplay.sunlight_burn_seconds;
        CreRacesConfig.PASSIVE_EFFECT_BUFFER_TICKS = () -> common.gameplay.passive_effect_buffer_ticks;
        CreRacesConfig.PASSIVE_DEFAULT_MAX_FOOD = () -> common.gameplay.passive_default_max_food;
        CreRacesConfig.PASSIVE_DEFAULT_MAX_SATURATION = () -> common.gameplay.passive_default_max_saturation;
        CreRacesConfig.RACE_ADDONS_ENABLED = () -> common.gameplay.race_addons_enabled;
        CreRacesConfig.LORE_ADDONS_ENABLED = () -> common.gameplay.lore_addons_enabled;
        CreRacesConfig.MAX_SOUL = () -> common.gameplay.max_soul;

        // Safety
        CreRacesConfig.AOE_MAX_RADIUS = () -> common.safety.aoe_max_radius;
        CreRacesConfig.BEAM_MAX_LENGTH = () -> common.safety.beam_max_length;
        CreRacesConfig.BREAK_BLOCKS_MAX_RADIUS = () -> common.safety.break_blocks_max_radius;
        CreRacesConfig.REMOVE_BLOCK_HARDNESS_LIMIT = () -> common.safety.remove_block_hardness_limit;
        CreRacesConfig.CUSTOMIZATION_VALUE_MAX_LENGTH = () -> common.safety.customization_value_max_length;
        CreRacesConfig.DELAY_ACTION_MAX_TICKS = () -> common.safety.delay_action_max_ticks;
        CreRacesConfig.ENTITY_DATA_KEY_MAX_LENGTH = () -> common.safety.entity_data_key_max_length;
        CreRacesConfig.GIVE_ITEM_MAX_COUNT = () -> common.safety.give_item_max_count;
        CreRacesConfig.MASS_SUMMON_MAX_COUNT = () -> common.safety.mass_summon_max_count;
        CreRacesConfig.NETWORK_TEAM_NAME_MAX_LEN = () -> common.safety.network_team_name_max_len;
        CreRacesConfig.TEAM_MAX_SIZE = () -> common.safety.team_max_size;
        CreRacesConfig.DOUBLE_JUMP_COOLDOWN_TICKS = () -> common.safety.double_jump_cooldown_ticks;
        CreRacesConfig.SPIRIT_SPAWN_CHECK_RADIUS = () -> common.safety.spirit_spawn_check_radius;
        CreRacesConfig.MAX_RAT_TUNNELS = () -> common.safety.max_rat_tunnels;

        // MiniBuild
        CreRacesConfig.MINI_BUILD_ENABLED = () -> common.minibuild.mini_build_enabled;
        CreRacesConfig.MINI_FURNACE_ENABLED = () -> common.minibuild.mini_furnace_enabled;
        CreRacesConfig.MINI_PLACE_WHITELIST_ENABLED = () -> common.minibuild.mini_place_whitelist_enabled;
        CreRacesConfig.RACE_SOCIAL_DEFENSE_RANGE = () -> common.minibuild.social_defense_range;
        CreRacesConfig.MICRO_BLOCK_LIGHT_PER_TORCH = () -> common.minibuild.micro_block_light_per_torch;
        CreRacesConfig.MICRO_BLOCK_MAX_LIGHT = () -> common.minibuild.micro_block_max_light;
        CreRacesConfig.MINIBUILD_REQUIRES_LEARNED = () -> common.minibuild.mini_build_requires_learned;
        CreRacesConfig.MINI_CRAFTING_DISTANCE_SQR = () -> common.minibuild.mini_crafting_distance_sqr;
        CreRacesConfig.MINI_PLACEMENT_SPAM_THRESHOLD_MS = () -> common.minibuild.mini_placement_spam_threshold_ms;
        CreRacesConfig.MINI_BLOCK_REACH_MARGIN = () -> common.minibuild.mini_block_reach_margin;
        CreRacesConfig.MINI_BLOCK_WATER_RESISTANT = () -> common.minibuild.mini_block_water_resistant;

        // Pockets
        CreRacesConfig.POCKET_DIM_SPACING = () -> common.pockets.pocket_dim_spacing;
        CreRacesConfig.POCKET_DIM_Y = () -> common.pockets.pocket_dim_y;
        CreRacesConfig.POCKET_EXPANSION_COST = () -> common.pockets.pocket_expansion_cost;
        CreRacesConfig.POCKET_EXPANSION_LIMIT = () -> common.pockets.pocket_expansion_limit;
        CreRacesConfig.POCKET_INVITE_MAX = () -> common.pockets.pocket_invite_max;
        CreRacesConfig.POCKET_BOUNDARY = () -> common.pockets.pocket_boundary;
        CreRacesConfig.ACTION_DEFAULT_POCKET_DIM = () -> common.pockets.action_default_pocket_dim;
        CreRacesConfig.ACTION_DEFAULT_POCKET_STRUCTURE = () -> common.pockets.action_default_pocket_structure;
        CreRacesConfig.ACTION_DEFAULT_POCKET_SPAWN_X_OFFSET = () -> common.pockets.action_default_pocket_spawn_x_offset;
        CreRacesConfig.ACTION_DEFAULT_POCKET_SPAWN_Y_OFFSET = () -> common.pockets.action_default_pocket_spawn_y_offset;
        CreRacesConfig.ACTION_DEFAULT_POCKET_SPAWN_Z_OFFSET = () -> common.pockets.action_default_pocket_spawn_z_offset;

        // Client
        CreRacesConfig.MINI_MODEL_CACHE_SIZE = () -> client.rendering.mini_model_cache_size;
        CreRacesConfig.VISUAL_SYNC_DISTANCE = () -> client.rendering.visual_sync_distance;
        CreRacesConfig.RACE_OVERLAY_OFFSET_X = () -> client.hud.race_overlay_offset_x;
        CreRacesConfig.RACE_OVERLAY_OFFSET_Y = () -> client.hud.race_overlay_offset_y;
        CreRacesConfig.RACE_OVERLAYS_ENABLED = () -> client.hud.race_overlays_enabled;
        CreRacesConfig.ENGINE_POPUPS_ENABLED = () -> client.hud.engine_popups_enabled;
        CreRacesConfig.MINI_DUMMY_CACHE_SIZE = () -> client.hud.mini_dummy_cache_size;

        // Balancing

        CreRacesConfig.ENTITY_TROLL_PILLAR_MAX_HEALTH = () -> balancing.entities.troll_pillar_health;
        CreRacesConfig.ENTITY_TROLL_PILLAR_ARMOR = () -> balancing.entities.troll_pillar_armor;
        CreRacesConfig.ENTITY_TROLL_PILLAR_FOLLOW_RANGE = () -> balancing.entities.troll_pillar_follow_range;
        CreRacesConfig.ENTITY_TROLL_PILLAR_KNOCKBACK_RES = () -> balancing.entities.troll_pillar_knockback_res;
        CreRacesConfig.ENTITY_TROLL_PILLAR_PULSE_INTERVAL = () -> balancing.entities.troll_pillar_pulse_interval;
        CreRacesConfig.ENTITY_TROLL_PILLAR_CURSE_RADIUS = () -> balancing.entities.troll_pillar_curse_radius;
        CreRacesConfig.ENTITY_TROLL_PILLAR_CURSE_DURATION = () -> balancing.entities.troll_pillar_curse_duration;
        CreRacesConfig.ENTITY_TROLL_PILLAR_LIFETIME_TICKS = () -> balancing.entities.troll_pillar_lifetime_ticks;

        CreRacesConfig.ENTITY_POISON_EMITTER_HEALTH = () -> balancing.entities.poison_emitter_health;
        CreRacesConfig.ENTITY_POISON_EMITTER_ARMOR = () -> balancing.entities.poison_emitter_armor;
        CreRacesConfig.ENTITY_POISON_EMITTER_FOLLOW_RANGE = () -> balancing.entities.poison_emitter_follow_range;
        CreRacesConfig.ENTITY_POISON_EMITTER_KNOCKBACK_RES = () -> balancing.entities.poison_emitter_knockback_res;
        CreRacesConfig.ENTITY_POISON_EMITTER_RADIUS = () -> balancing.entities.poison_emitter_radius;
        CreRacesConfig.ENTITY_POISON_EMITTER_LIFETIME_TICKS = () -> balancing.entities.poison_emitter_lifetime_ticks;

        CreRacesConfig.RAT_VENOM_SCALING = () -> balancing.potion_effects.rat_venom_scaling;
        CreRacesConfig.BOILING_SCALING = () -> balancing.potion_effects.boiling_scaling;
        CreRacesConfig.BLEEDING_SCALING = () -> balancing.potion_effects.bleeding_scaling;

        CreRacesConfig.ENTITY_POISON_EMITTER_MOBILE_MAX_HEALTH = () -> balancing.entities.poison_emitter_mobile_max_health;
        CreRacesConfig.ENTITY_POISON_EMITTER_MOBILE_MOVEMENT_SPEED = () -> balancing.entities.poison_emitter_mobile_movement_speed;

        CreRacesConfig.ENTITY_FEATHER_DAMAGE = () -> balancing.entities.feather_damage;
        CreRacesConfig.ENTITY_FEATHER_GRAVITY = () -> balancing.entities.feather_gravity;

        CreRacesConfig.ENTITY_TORNADO_HEALTH = () -> balancing.entities.tornado_health;
        CreRacesConfig.ENTITY_TORNADO_ARMOR = () -> balancing.entities.tornado_armor;
        CreRacesConfig.ENTITY_TORNADO_ATTACK_DAMAGE = () -> balancing.entities.tornado_attack_damage;
        CreRacesConfig.ENTITY_TORNADO_FLYING_SPEED = () -> balancing.entities.tornado_flying_speed;
        CreRacesConfig.ENTITY_TORNADO_MOVEMENT_SPEED = () -> balancing.entities.tornado_movement_speed;
        CreRacesConfig.ENTITY_TORNADO_RADIUS = () -> (float) balancing.entities.tornado_hurricane_radius;
        CreRacesConfig.ENTITY_TORNADO_DIZZINESS_DURATION = () -> balancing.entities.tornado_dizziness_duration;
        CreRacesConfig.ENTITY_TORNADO_PULL_FORCE = () -> balancing.entities.tornado_pull_force;
        CreRacesConfig.ENTITY_TORNADO_FOLLOW_RANGE = () -> balancing.entities.tornado_follow_range;
        CreRacesConfig.ENTITY_TORNADO_LIFETIME_TICKS = () -> balancing.entities.tornado_lifetime_ticks;

        CreRacesConfig.REMAINS_DECAY_TIME = () -> (int) balancing.specialized.remains_decay_time;
        CreRacesConfig.REMAINS_HEALTH = () -> balancing.specialized.remains_health;
    }

    public static class CommonConfig {
        public Documentation documentation = new Documentation();
        public Gameplay gameplay = new Gameplay();
        public Safety safety = new Safety();
        public MiniBuild minibuild = new MiniBuild();
        public Pockets pockets = new Pockets();

        public static class Documentation {
            public String wiki_base_url = CreRacesConfig.WIKI_BASE_URL.get();
            public String wiki_page_path = CreRacesConfig.WIKI_PAGE_PATH.get();
            public String wiki_ability_namespace = CreRacesConfig.WIKI_ABILITY_NAMESPACE.get();
            public boolean disable_remote_docs = CreRacesConfig.DISABLE_REMOTE_DOCS.get();
            public int doc_fetch_timeout_seconds = CreRacesConfig.DOC_FETCH_TIMEOUT_SECONDS.get();
            public String doc_cache_dir = CreRacesConfig.DOC_CACHE_DIR.get();
            public String doc_cache_filename = CreRacesConfig.DOC_CACHE_FILENAME.get();
            public String wiki_api_base = CreRacesConfig.WIKI_API_BASE.get();
            public String developer_resource_path = CreRacesConfig.DEVELOPER_RESOURCE_PATH.get();
        }

        public static class Gameplay {
            public boolean forced_selection = CreRacesConfig.FORCED_SELECTION.get();
            public double ability_haste_cap = CreRacesConfig.ABILITY_HASTE_CAP.get();
            public double racial_ad_multiplier = CreRacesConfig.RACIAL_AD_MULTIPLIER.get();
            public long resource_decay_grace_period = CreRacesConfig.RESOURCE_DECAY_GRACE_PERIOD.get();
            public boolean coin_drop_enabled = CreRacesConfig.COIN_DROP_ENABLED.get();
            public boolean sag_wings = CreRacesConfig.SAG_WINGS.get();
            public boolean gstate_enabled = CreRacesConfig.GSTATE_ENABLED.get();
            public int ritual_mode = CreRacesConfig.RITUAL_MODE.get();
            public int passive_execution_interval = CreRacesConfig.PASSIVE_EXECUTION_INTERVAL.get();
            public double sunlight_equipment_break_chance = CreRacesConfig.SUNLIGHT_EQUIPMENT_BREAK_CHANCE.get();
            public int sunlight_burn_seconds = CreRacesConfig.SUNLIGHT_BURN_SECONDS.get();
            public int passive_effect_buffer_ticks = CreRacesConfig.PASSIVE_EFFECT_BUFFER_TICKS.get();
            public int passive_default_max_food = CreRacesConfig.PASSIVE_DEFAULT_MAX_FOOD.get();
            public double passive_default_max_saturation = CreRacesConfig.PASSIVE_DEFAULT_MAX_SATURATION.get();
            public boolean race_addons_enabled = CreRacesConfig.RACE_ADDONS_ENABLED.get();
            public boolean lore_addons_enabled = CreRacesConfig.LORE_ADDONS_ENABLED.get();
            public double max_soul = CreRacesConfig.MAX_SOUL.get();
        }

        public static class Safety {
            public int aoe_max_radius = CreRacesConfig.AOE_MAX_RADIUS.get();
            public int beam_max_length = CreRacesConfig.BEAM_MAX_LENGTH.get();
            public int break_blocks_max_radius = CreRacesConfig.BREAK_BLOCKS_MAX_RADIUS.get();
            public double remove_block_hardness_limit = CreRacesConfig.REMOVE_BLOCK_HARDNESS_LIMIT.get();
            public int customization_value_max_length = CreRacesConfig.CUSTOMIZATION_VALUE_MAX_LENGTH.get();
            public int delay_action_max_ticks = CreRacesConfig.DELAY_ACTION_MAX_TICKS.get();
            public int entity_data_key_max_length = CreRacesConfig.ENTITY_DATA_KEY_MAX_LENGTH.get();
            public int give_item_max_count = CreRacesConfig.GIVE_ITEM_MAX_COUNT.get();
            public int mass_summon_max_count = CreRacesConfig.MASS_SUMMON_MAX_COUNT.get();
            public int network_team_name_max_len = CreRacesConfig.NETWORK_TEAM_NAME_MAX_LEN.get();
            public int team_max_size = CreRacesConfig.TEAM_MAX_SIZE.get();
            public int double_jump_cooldown_ticks = CreRacesConfig.DOUBLE_JUMP_COOLDOWN_TICKS.get();
            public int spirit_spawn_check_radius = CreRacesConfig.SPIRIT_SPAWN_CHECK_RADIUS.get();
            public int max_rat_tunnels = CreRacesConfig.MAX_RAT_TUNNELS.get();
        }

        public static class MiniBuild {
            public boolean mini_build_enabled = CreRacesConfig.MINI_BUILD_ENABLED.get();
            public boolean mini_furnace_enabled = CreRacesConfig.MINI_FURNACE_ENABLED.get();
            public boolean mini_place_whitelist_enabled = CreRacesConfig.MINI_PLACE_WHITELIST_ENABLED.get();
            public double social_defense_range = CreRacesConfig.RACE_SOCIAL_DEFENSE_RANGE.get();
            public int micro_block_light_per_torch = CreRacesConfig.MICRO_BLOCK_LIGHT_PER_TORCH.get();
            public int micro_block_max_light = CreRacesConfig.MICRO_BLOCK_MAX_LIGHT.get();
            public boolean mini_build_requires_learned = CreRacesConfig.MINIBUILD_REQUIRES_LEARNED.get();
            public double mini_crafting_distance_sqr = CreRacesConfig.MINI_CRAFTING_DISTANCE_SQR.get();
            public long mini_placement_spam_threshold_ms = CreRacesConfig.MINI_PLACEMENT_SPAM_THRESHOLD_MS.get();
            public double mini_block_reach_margin = CreRacesConfig.MINI_BLOCK_REACH_MARGIN.get();
            public boolean mini_block_water_resistant = CreRacesConfig.MINI_BLOCK_WATER_RESISTANT.get();
        }

        public static class Pockets {
            public double pocket_dim_spacing = CreRacesConfig.POCKET_DIM_SPACING.get();
            public double pocket_dim_y = CreRacesConfig.POCKET_DIM_Y.get();
            public double pocket_expansion_cost = CreRacesConfig.POCKET_EXPANSION_COST.get();
            public int pocket_expansion_limit = CreRacesConfig.POCKET_EXPANSION_LIMIT.get();
            public int pocket_invite_max = CreRacesConfig.POCKET_INVITE_MAX.get();
            public double pocket_boundary = CreRacesConfig.POCKET_BOUNDARY.get();
            public String action_default_pocket_dim = CreRacesConfig.ACTION_DEFAULT_POCKET_DIM.get();
            public String action_default_pocket_structure = CreRacesConfig.ACTION_DEFAULT_POCKET_STRUCTURE.get();
            public double action_default_pocket_spawn_x_offset = CreRacesConfig.ACTION_DEFAULT_POCKET_SPAWN_X_OFFSET
                    .get();
            public double action_default_pocket_spawn_y_offset = CreRacesConfig.ACTION_DEFAULT_POCKET_SPAWN_Y_OFFSET
                    .get();
            public double action_default_pocket_spawn_z_offset = CreRacesConfig.ACTION_DEFAULT_POCKET_SPAWN_Z_OFFSET
                    .get();
        }
    }

    public static class ClientConfig {
        public Rendering rendering = new Rendering();
        public HUD hud = new HUD();

        public static class Rendering {
            public int mini_model_cache_size = CreRacesConfig.MINI_MODEL_CACHE_SIZE.get();
            public int visual_sync_distance = CreRacesConfig.VISUAL_SYNC_DISTANCE.get();
        }

        public static class HUD {
            public int race_overlay_offset_x = CreRacesConfig.RACE_OVERLAY_OFFSET_X.get();
            public int race_overlay_offset_y = CreRacesConfig.RACE_OVERLAY_OFFSET_Y.get();
            public boolean race_overlays_enabled = CreRacesConfig.RACE_OVERLAYS_ENABLED.get();
            public boolean engine_popups_enabled = CreRacesConfig.ENGINE_POPUPS_ENABLED.get();
            public int mini_dummy_cache_size = CreRacesConfig.MINI_DUMMY_CACHE_SIZE.get();
        }
    }

    public static class BalancingConfig {
        public Entities entities = new Entities();
        public PotionEffects potion_effects = new PotionEffects();
        public Specialized specialized = new Specialized();

        public static class PotionEffects {
            public double rat_venom_scaling = CreRacesConfig.RAT_VENOM_SCALING.get();
            public double boiling_scaling = CreRacesConfig.BOILING_SCALING.get();
            public double bleeding_scaling = CreRacesConfig.BLEEDING_SCALING.get();
        }

        public static class Entities {
            public double troll_pillar_health = CreRacesConfig.ENTITY_TROLL_PILLAR_MAX_HEALTH.get();
            public double troll_pillar_armor = CreRacesConfig.ENTITY_TROLL_PILLAR_ARMOR.get();
            public double troll_pillar_follow_range = CreRacesConfig.ENTITY_TROLL_PILLAR_FOLLOW_RANGE.get();
            public double troll_pillar_knockback_res = CreRacesConfig.ENTITY_TROLL_PILLAR_KNOCKBACK_RES.get();
            public int troll_pillar_pulse_interval = CreRacesConfig.ENTITY_TROLL_PILLAR_PULSE_INTERVAL.get();
            public double troll_pillar_curse_radius = CreRacesConfig.ENTITY_TROLL_PILLAR_CURSE_RADIUS.get();
            public int troll_pillar_curse_duration = CreRacesConfig.ENTITY_TROLL_PILLAR_CURSE_DURATION.get();
            public int troll_pillar_lifetime_ticks = CreRacesConfig.ENTITY_TROLL_PILLAR_LIFETIME_TICKS.get();

            public double poison_emitter_health = CreRacesConfig.ENTITY_POISON_EMITTER_HEALTH.get();
            public double poison_emitter_armor = CreRacesConfig.ENTITY_POISON_EMITTER_ARMOR.get();
            public double poison_emitter_follow_range = CreRacesConfig.ENTITY_POISON_EMITTER_FOLLOW_RANGE.get();
            public double poison_emitter_knockback_res = CreRacesConfig.ENTITY_POISON_EMITTER_KNOCKBACK_RES.get();
            public double poison_emitter_radius = CreRacesConfig.ENTITY_POISON_EMITTER_RADIUS.get();
            public int poison_emitter_lifetime_ticks = CreRacesConfig.ENTITY_POISON_EMITTER_LIFETIME_TICKS.get();

            public double poison_emitter_mobile_max_health = CreRacesConfig.ENTITY_POISON_EMITTER_MOBILE_MAX_HEALTH
                    .get();
            public double poison_emitter_mobile_movement_speed = CreRacesConfig.ENTITY_POISON_EMITTER_MOBILE_MOVEMENT_SPEED
                    .get();

            public double feather_damage = CreRacesConfig.ENTITY_FEATHER_DAMAGE.get();
            public double feather_gravity = CreRacesConfig.ENTITY_FEATHER_GRAVITY.get();

            public double tornado_health = CreRacesConfig.ENTITY_TORNADO_HEALTH.get();
            public double tornado_armor = CreRacesConfig.ENTITY_TORNADO_ARMOR.get();
            public double tornado_attack_damage = CreRacesConfig.ENTITY_TORNADO_ATTACK_DAMAGE.get();
            public double tornado_flying_speed = CreRacesConfig.ENTITY_TORNADO_FLYING_SPEED.get();
            public double tornado_movement_speed = CreRacesConfig.ENTITY_TORNADO_MOVEMENT_SPEED.get();
            public double tornado_hurricane_radius = CreRacesConfig.ENTITY_TORNADO_RADIUS.get();
            public int tornado_dizziness_duration = CreRacesConfig.ENTITY_TORNADO_DIZZINESS_DURATION.get();
            public double tornado_pull_force = CreRacesConfig.ENTITY_TORNADO_PULL_FORCE.get();
            public double tornado_follow_range = CreRacesConfig.ENTITY_TORNADO_FOLLOW_RANGE.get();
            public int tornado_lifetime_ticks = CreRacesConfig.ENTITY_TORNADO_LIFETIME_TICKS.get();
        }

        public static class Specialized {
            public double remains_decay_time = CreRacesConfig.REMAINS_DECAY_TIME.get();
            public double remains_health = CreRacesConfig.REMAINS_HEALTH.get();
        }
    }
}
