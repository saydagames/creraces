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

        // Gameplay
        CreRacesConfig.FORCED_SELECTION = () -> common.gameplay.forced_selection;
        CreRacesConfig.HUMAN_SELECTION_ALLOWED = () -> common.gameplay.human_selection_allowed;
        CreRacesConfig.TEAM_INVITE_TIMEOUT_TICKS = () -> common.gameplay.team_invite_timeout_ticks;
        CreRacesConfig.ABILITY_HASTE_CAP = () -> common.gameplay.ability_haste_cap;
        CreRacesConfig.RACIAL_AD_MULTIPLIER = () -> common.gameplay.racial_ad_multiplier;
        CreRacesConfig.COIN_DROP_ENABLED = () -> common.gameplay.coin_drop_enabled;
        CreRacesConfig.COIN_TRANSFER_ALLOWED = () -> common.gameplay.coin_transfer_allowed;
        CreRacesConfig.SAG_WINGS_RAIN_FLIGHT = () -> common.gameplay.sag_wings_rain_flight;
        CreRacesConfig.GENDER_SYSTEM_ENABLED = () -> common.gameplay.gender_system_enabled;
        CreRacesConfig.RACE_FRIENDLY_MOBS_ENABLED = () -> common.gameplay.race_friendly_mobs_enabled;
        CreRacesConfig.RITUAL_MODE = () -> common.gameplay.ritual_mode;

        // Safety
        CreRacesConfig.BREAK_BLOCKS_MAX_RADIUS = () -> common.safety.break_blocks_max_radius;
        CreRacesConfig.AOE_MAX_RADIUS = () -> common.safety.aoe_max_radius;
        CreRacesConfig.BEAM_MAX_LENGTH = () -> common.safety.beam_max_length;
        CreRacesConfig.ENTITY_DATA_KEY_MAX_LENGTH = () -> common.safety.entity_data_key_max_length;
        CreRacesConfig.NETWORK_TEAM_NAME_MAX_LEN = () -> common.safety.network_team_name_max_len;
        CreRacesConfig.NETWORK_PLAYER_NAME_MAX_LEN = () -> common.safety.network_player_name_max_len;
        CreRacesConfig.NETWORK_BUFFER_MAX_UTF_LEN = () -> common.safety.network_buffer_max_utf_len;

        // MiniBuild
        CreRacesConfig.MINI_BUILD_ENABLED = () -> common.minibuild.mini_build_enabled;
        CreRacesConfig.MINI_FURNACE_ENABLED = () -> common.minibuild.mini_furnace_enabled;
        CreRacesConfig.MINI_PLACE_WHITELIST_ENABLED = () -> common.minibuild.mini_place_whitelist_enabled;
        CreRacesConfig.RACE_SOCIAL_DEFENSE_RANGE = () -> common.minibuild.social_defense_range;

        // Pockets
        CreRacesConfig.POCKET_DIM_SPACING = () -> common.pockets.pocket_dim_spacing;
        CreRacesConfig.POCKET_EXPANSION_LIMIT = () -> common.pockets.pocket_expansion_limit;
        CreRacesConfig.POCKET_EXPANSION_COST = () -> common.pockets.pocket_expansion_cost;

        // Client
        CreRacesConfig.MINI_MODEL_CACHE_SIZE = () -> client.rendering.mini_model_cache_size;
        CreRacesConfig.VISUAL_SYNC_DISTANCE = () -> client.rendering.visual_sync_distance;
        CreRacesConfig.RACE_OVERLAY_OFFSET_X = () -> client.hud.race_overlay_offset_x;
        CreRacesConfig.RACE_OVERLAY_OFFSET_Y = () -> client.hud.race_overlay_offset_y;
        CreRacesConfig.RACE_OVERLAYS_ENABLED = () -> client.hud.race_overlays_enabled;
        CreRacesConfig.ENGINE_POPUPS_ENABLED = () -> client.hud.engine_popups_enabled;

        // Balancing
        CreRacesConfig.RACE_DEFAULT_HP_MOD = () -> balancing.attributes.race_default_hp_mod;
        CreRacesConfig.RACE_DEFAULT_HEIGHT_MOD = () -> balancing.attributes.race_default_height_mod;
        CreRacesConfig.RACE_DEFAULT_WIDTH_MOD = () -> balancing.attributes.race_default_width_mod;

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
        CreRacesConfig.ENTITY_POISON_EMITTER_PULSE_INTERVAL = () -> balancing.entities.poison_emitter_pulse_interval;
        CreRacesConfig.ENTITY_POISON_EMITTER_RADIUS = () -> balancing.entities.poison_emitter_radius;
        CreRacesConfig.ENTITY_POISON_EMITTER_VENOM_DURATION = () -> balancing.entities.poison_emitter_venom_duration;
        CreRacesConfig.ENTITY_POISON_EMITTER_LIFETIME_TICKS = () -> balancing.entities.poison_emitter_lifetime_ticks;

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

        CreRacesConfig.UNDEAD_SUMMON_CAP = () -> balancing.specialized.undead_summon_cap;
        CreRacesConfig.REMAINS_DECAY_TIME = () -> (int) balancing.specialized.remains_decay_time;
        CreRacesConfig.DRYAD_BLESSING_COST = () -> balancing.specialized.dryad_blessing_cost;
    }

    public static class CommonConfig {
        public Documentation documentation = new Documentation();
        public Gameplay gameplay = new Gameplay();
        public Safety safety = new Safety();
        public MiniBuild minibuild = new MiniBuild();
        public Pockets pockets = new Pockets();

        public static class Documentation {
            public String wiki_base_url = "https://creraces.wiki.gg/";
            public String wiki_page_path = "wiki/";
            public String wiki_ability_namespace = "Ability:";
            public boolean disable_remote_docs = false;
            public int doc_fetch_timeout_seconds = 10;
            public String doc_cache_dir = "cache";
            public String doc_cache_filename = "docs.json";
        }

        public static class Gameplay {
            public boolean forced_selection = true;
            public boolean human_selection_allowed = true;
            public int team_invite_timeout_ticks = 6000;
            public double ability_haste_cap = 40.0;
            public double racial_ad_multiplier = 0.01;
            public boolean coin_drop_enabled = true;
            public boolean coin_transfer_allowed = true;
            public boolean sag_wings_rain_flight = true;
            public boolean gender_system_enabled = true;
            public boolean race_friendly_mobs_enabled = true;
            public int ritual_mode = 2;
        }

        public static class Safety {
            public int break_blocks_max_radius = 16;
            public int aoe_max_radius = 64;
            public int beam_max_length = 64;
            public int entity_data_key_max_length = 64;
            public int network_team_name_max_len = 32;
            public int network_player_name_max_len = 16;
            public int network_buffer_max_utf_len = 256;
        }

        public static class MiniBuild {
            public boolean mini_build_enabled = true;
            public boolean mini_furnace_enabled = true;
            public boolean mini_place_whitelist_enabled = true;
            public double social_defense_range = 16.0;
        }

        public static class Pockets {
            public double pocket_dim_spacing = 1000.0;
            public int pocket_expansion_limit = 8;
            public double pocket_expansion_cost = 200.0;
        }
    }

    public static class ClientConfig {
        public Rendering rendering = new Rendering();
        public HUD hud = new HUD();

        public static class Rendering {
            public int mini_model_cache_size = 256;
            public int visual_sync_distance = 64;
        }

        public static class HUD {
            public int race_overlay_offset_x = 0;
            public int race_overlay_offset_y = 0;
            public boolean race_overlays_enabled = true;
            public boolean engine_popups_enabled = false;
        }
    }

    public static class BalancingConfig {
        public Attributes attributes = new Attributes();
        public Entities entities = new Entities();
        public Specialized specialized = new Specialized();

        public static class Attributes {
            public double race_default_hp_mod = 0.0;
            public double race_default_height_mod = 0.0;
            public double race_default_width_mod = 0.0;
        }

        public static class Entities {
            public double troll_pillar_health = 40.0;
            public double troll_pillar_armor = 10.0;
            public double troll_pillar_follow_range = 16.0;
            public double troll_pillar_knockback_res = 1.0;
            public int troll_pillar_pulse_interval = 20;
            public double troll_pillar_curse_radius = 5.0;
            public int troll_pillar_curse_duration = 100;
            public int troll_pillar_lifetime_ticks = 600;

            public double poison_emitter_health = 10.0;
            public double poison_emitter_armor = 5.0;
            public double poison_emitter_follow_range = 16.0;
            public double poison_emitter_knockback_res = 1.0;
            public int poison_emitter_pulse_interval = 20;
            public double poison_emitter_radius = 3.0;
            public int poison_emitter_venom_duration = 102;
            public int poison_emitter_lifetime_ticks = 100;

            public double poison_emitter_mobile_max_health = 16.0;
            public double poison_emitter_mobile_movement_speed = 0.35;

            public double feather_damage = 2.0;
            public double feather_gravity = 0.05;

            public double tornado_health = 20.0;
            public double tornado_armor = 5.0;
            public double tornado_attack_damage = 3.0;
            public double tornado_flying_speed = 0.1;
            public double tornado_movement_speed = 0.3;
            public double tornado_hurricane_radius = 4.0;
            public int tornado_dizziness_duration = 60;
            public double tornado_pull_force = 0.05;
            public double tornado_follow_range = 16.0;
            public int tornado_lifetime_ticks = 200;
        }

        public static class Specialized {
            public double undead_summon_cap = 5.0;
            public double remains_decay_time = 1200.0;
            public double dryad_blessing_cost = 500.0;
        }
    }
}
