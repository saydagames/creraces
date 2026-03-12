package mc.sayda.creraces.config.forge;

import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeConfig {

        public static final Common COMMON;
        public static final ForgeConfigSpec COMMON_SPEC;

        public static final Client CLIENT;
        public static final ForgeConfigSpec CLIENT_SPEC;

        public static final Entities ENTITIES;
        public static final ForgeConfigSpec ENTITIES_SPEC;

        static {
                final Pair<Common, ForgeConfigSpec> commonPair = new ForgeConfigSpec.Builder().configure(Common::new);
                COMMON = commonPair.getLeft();
                COMMON_SPEC = commonPair.getRight();

                final Pair<Client, ForgeConfigSpec> clientPair = new ForgeConfigSpec.Builder().configure(Client::new);
                CLIENT = clientPair.getLeft();
                CLIENT_SPEC = clientPair.getRight();

                final Pair<Entities, ForgeConfigSpec> entitiesPair = new ForgeConfigSpec.Builder()
                                .configure(Entities::new);
                ENTITIES = entitiesPair.getLeft();
                ENTITIES_SPEC = entitiesPair.getRight();
        }

        public static class Common {
                public final ForgeConfigSpec.ConfigValue<String> wiki_base_url;
                public final ForgeConfigSpec.ConfigValue<String> wiki_api_base;
                public final ForgeConfigSpec.ConfigValue<String> wiki_page_path;
                public final ForgeConfigSpec.ConfigValue<String> wiki_ability_namespace;
                public final ForgeConfigSpec.BooleanValue disable_remote_docs;
                public final ForgeConfigSpec.IntValue doc_fetch_timeout_seconds;
                public final ForgeConfigSpec.ConfigValue<String> doc_cache_dir;
                public final ForgeConfigSpec.ConfigValue<String> doc_cache_filename;

                public final ForgeConfigSpec.BooleanValue forced_selection;
                public final ForgeConfigSpec.DoubleValue ability_haste_cap;
                public final ForgeConfigSpec.DoubleValue racial_ad_multiplier;
                public final ForgeConfigSpec.IntValue team_invite_timeout_ticks;

                public final ForgeConfigSpec.BooleanValue human_selection_allowed;
                public final ForgeConfigSpec.BooleanValue coin_drop_enabled;
                public final ForgeConfigSpec.BooleanValue coin_transfer_allowed;
                public final ForgeConfigSpec.BooleanValue sag_wings_rain_flight;
                public final ForgeConfigSpec.BooleanValue gender_system_enabled;
                public final ForgeConfigSpec.BooleanValue race_friendly_mobs_enabled;
                public final ForgeConfigSpec.IntValue ritual_mode;

                // Safety caps (0 = disabled)
                public final ForgeConfigSpec.IntValue break_blocks_max_radius;
                public final ForgeConfigSpec.IntValue aoe_max_radius;
                public final ForgeConfigSpec.IntValue beam_max_length;
                public final ForgeConfigSpec.IntValue entity_data_key_max_length;

                public final ForgeConfigSpec.IntValue network_team_name_max_len;
                public final ForgeConfigSpec.IntValue network_player_name_max_len;

                public final ForgeConfigSpec.BooleanValue mini_build_enabled;
                public final ForgeConfigSpec.BooleanValue mini_furnace_enabled;
                public final ForgeConfigSpec.BooleanValue mini_place_whitelist_enabled;
                public final ForgeConfigSpec.DoubleValue social_defense_range;
                public final ForgeConfigSpec.IntValue micro_block_light_per_torch;
                public final ForgeConfigSpec.IntValue micro_block_max_light;

                // Pocket
                public final ForgeConfigSpec.DoubleValue pocket_dim_spacing;
                public final ForgeConfigSpec.IntValue pocket_expansion_limit;
                public final ForgeConfigSpec.DoubleValue pocket_expansion_cost;

                public Common(ForgeConfigSpec.Builder builder) {
                        builder.push("Documentation");
                        wiki_base_url = builder.comment("The base URL for the wiki").define("wiki_base_url",
                                        "https://creraces.wiki.gg/");
                        wiki_api_base = builder.comment("The API base URL for the wiki").define("wiki_api_base",
                                        "https://creraces.wiki.gg/");
                        wiki_page_path = builder.comment("The path part of the wiki URL").define("wiki_page_path",
                                        "wiki/");
                        wiki_ability_namespace = builder.comment("The prefix for ability pages")
                                        .define("wiki_ability_namespace", "Ability:");
                        disable_remote_docs = builder.comment("Set to true to disable remote documentation fetching")
                                        .define("disable_remote_docs", false);
                        doc_fetch_timeout_seconds = builder
                                        .comment("Timeout in seconds for fetching remote documentation")
                                        .defineInRange("doc_fetch_timeout_seconds", 10, 1, 60);
                        doc_cache_dir = builder.comment("Directory name for the documentation cache")
                                        .define("doc_cache_dir", "cache");
                        doc_cache_filename = builder.comment("Filename for the documentation cache")
                                        .define("doc_cache_filename", "docs.json");
                        builder.pop();

                        builder.push("Gameplay");
                        forced_selection = builder.comment(
                                        "Keep the selection GUI open until a race is chosen. Also grants invulnerability while choosing.")
                                        .define("forced_selection", true);
                        human_selection_allowed = builder.comment("Allow players to pick 'Human' as their race.")
                                        .define("human_selection_allowed", true);
                        ability_haste_cap = builder.comment("The soft cap for Ability Haste. Max value is 100.")
                                        .defineInRange("ability_haste_cap", 40.0, 0.0, 100.0);
                        racial_ad_multiplier = builder.comment(
                                        "Multiplier for Racial Attack Damage (AD). Default is 0.01 (1% per point).")
                                        .defineInRange("racial_ad_multiplier", 0.01, 0.0, 1.0);
                        team_invite_timeout_ticks = builder
                                        .comment("Timeout in ticks for team invites. Default is 6000 (5 minutes).")
                                        .defineInRange("team_invite_timeout_ticks", 6000, 20, 72000);
                        coin_drop_enabled = builder.comment("Enable loot-based coin drops.")
                                        .define("coin_drop_enabled", true);
                        coin_transfer_allowed = builder.comment("Allow players to transfer coins.")
                                        .define("coin_transfer_allowed", true);
                        sag_wings_rain_flight = builder.comment("Allow SAG wings flight during rain.")
                                        .define("sag_wings_rain_flight", true);
                        gender_system_enabled = builder.comment("Enable the gender system (affects some race visuals).")
                                        .define("gender_system_enabled", true);
                        race_friendly_mobs_enabled = builder.comment("Enable race-specific mob friendliness.")
                                        .define("race_friendly_mobs_enabled", true);
                        ritual_mode = builder.comment("Ritual Mode: 0=Disabled, 1=Human Only, 2=Reset Allowed.")
                                        .defineInRange("ritual_mode", 2, 0, 2);
                        builder.pop();

                        builder.push("Safety");
                        break_blocks_max_radius = builder.comment("Max radius for creraces:break_blocks. 0 = no cap.")
                                        .defineInRange("break_blocks_max_radius", 16, 0, 256);
                        aoe_max_radius = builder
                                        .comment("Max radius for creraces:aoe / creraces:apply_effect AoE. 0 = no cap.")
                                        .defineInRange("aoe_max_radius", 64, 0, 512);
                        beam_max_length = builder.comment("Max length for creraces:beam. 0 = no cap.")
                                        .defineInRange("beam_max_length", 64, 0, 512);
                        entity_data_key_max_length = builder.comment(
                                        "Max character length for creraces:modify_entity_data keys. 0 = no cap.")
                                        .defineInRange("entity_data_key_max_length", 64, 0, 1024);
                        network_team_name_max_len = builder.comment("Max length for team names.")
                                        .defineInRange("network_team_name_max_len", 32, 1, 256);
                        network_player_name_max_len = builder.comment("Max length for player names in teams.")
                                        .defineInRange("network_player_name_max_len", 16, 1, 64);
                        builder.pop();

                        builder.push("MiniBuild");
                        mini_build_enabled = builder
                                        .comment("Enable the Mini Build System. ON by default.")
                                        .define("mini_build_enabled", true);
                        mini_furnace_enabled = builder.comment("Enable the Mini Furnace system. ON by default.")
                                        .define("mini_furnace_enabled", true);
                        mini_place_whitelist_enabled = builder.comment("Enable the whitelist for Mini Place system.")
                                        .define("mini_place_whitelist_enabled", true);
                        micro_block_light_per_torch = builder.comment("Light level added per torch in a micro-block.")
                                        .defineInRange("micro_block_light_per_torch", 5, 0, 15);
                        micro_block_max_light = builder.comment("Maximum light level for a micro-block.")
                                        .defineInRange("micro_block_max_light", 15, 0, 15);
                        builder.pop();

                        builder.push("Pockets");
                        pocket_dim_spacing = builder.comment(
                                        "Distance between player pockets. WARNING: Do not decrease on existing worlds.")
                                        .defineInRange("pocket_dim_spacing", 1000.0, 100.0, 100000.0);
                        pocket_expansion_limit = builder.comment("Max number of expansions allowed per player.")
                                        .defineInRange("pocket_expansion_limit", 8, 0, 100);
                        pocket_expansion_cost = builder.comment("Coin cost per expansion.")
                                        .defineInRange("pocket_expansion_cost", 200.0, 0.0, 1000000.0);
                        builder.pop();

                        builder.push("Social");
                        social_defense_range = builder.comment("Range (blocks) for social passive defense assistance.")
                                        .defineInRange("social_defense_range", 16.0, 0.0, 128.0);
                        builder.pop();
                }
        }

        public static class Client {
                public final ForgeConfigSpec.IntValue mini_model_cache_size;
                public final ForgeConfigSpec.IntValue visual_sync_distance;
                public final ForgeConfigSpec.IntValue race_overlay_offset_x;
                public final ForgeConfigSpec.IntValue race_overlay_offset_y;
                public final ForgeConfigSpec.BooleanValue race_overlays_enabled;
                public final ForgeConfigSpec.BooleanValue engine_popups_enabled;

                public Client(ForgeConfigSpec.Builder builder) {
                        builder.push("Rendering");
                        mini_model_cache_size = builder.comment("Max models stored in memory for Mini-Blocks.")
                                        .defineInRange("mini_model_cache_size", 256, 1, 2048);
                        visual_sync_distance = builder
                                        .comment("Distance (blocks) for visual sync (beams, tethers, animations).")
                                        .defineInRange("visual_sync_distance", 64, 1, 512);
                        builder.pop();

                        builder.push("HUD");
                        race_overlay_offset_x = builder.comment("X offset for the race overlay HUD.")
                                        .defineInRange("race_overlay_offset_x", 0, -2048, 2048);
                        race_overlay_offset_y = builder.comment("Y offset for the race overlay HUD.")
                                        .defineInRange("race_overlay_offset_y", 0, -2048, 2048);
                        race_overlays_enabled = builder.comment("Enable racial UI overlays.")
                                        .define("race_overlays_enabled", true);
                        engine_popups_enabled = builder.comment("Enable engine-driven popup messages.")
                                        .define("engine_popups_enabled", false);
                        builder.pop();
                }
        }

        public static class Entities {
                public final ForgeConfigSpec.DoubleValue race_default_hp_mod;
                public final ForgeConfigSpec.DoubleValue race_default_height_mod;
                public final ForgeConfigSpec.DoubleValue race_default_width_mod;

                public final ForgeConfigSpec.DoubleValue troll_pillar_health;
                public final ForgeConfigSpec.DoubleValue troll_pillar_armor;
                public final ForgeConfigSpec.DoubleValue troll_pillar_follow_range;
                public final ForgeConfigSpec.DoubleValue troll_pillar_knockback_res;
                public final ForgeConfigSpec.DoubleValue poison_emitter_health;
                public final ForgeConfigSpec.DoubleValue poison_emitter_armor;
                public final ForgeConfigSpec.DoubleValue poison_emitter_follow_range;
                public final ForgeConfigSpec.DoubleValue poison_emitter_knockback_res;
                public final ForgeConfigSpec.IntValue poison_emitter_pulse_interval;
                public final ForgeConfigSpec.DoubleValue tornado_health;
                public final ForgeConfigSpec.DoubleValue tornado_follow_range;

                public final ForgeConfigSpec.DoubleValue undead_summon_cap;
                public final ForgeConfigSpec.DoubleValue remains_decay_time;
                public final ForgeConfigSpec.DoubleValue dryad_blessing_cost;

                public final ForgeConfigSpec.IntValue troll_pillar_pulse_interval;
                public final ForgeConfigSpec.DoubleValue troll_pillar_curse_radius;
                public final ForgeConfigSpec.IntValue troll_pillar_curse_duration;
                public final ForgeConfigSpec.IntValue troll_pillar_lifetime_ticks;

                public final ForgeConfigSpec.DoubleValue tornado_armor;
                public final ForgeConfigSpec.DoubleValue tornado_attack_damage;
                public final ForgeConfigSpec.DoubleValue tornado_flying_speed;
                public final ForgeConfigSpec.DoubleValue tornado_movement_speed;
                public final ForgeConfigSpec.DoubleValue tornado_hurricane_radius;
                public final ForgeConfigSpec.IntValue tornado_dizziness_duration;
                public final ForgeConfigSpec.DoubleValue tornado_pull_force;
                public final ForgeConfigSpec.IntValue tornado_lifetime_ticks;

                public final ForgeConfigSpec.DoubleValue poison_emitter_mobile_max_health;
                public final ForgeConfigSpec.DoubleValue poison_emitter_mobile_movement_speed;
                public final ForgeConfigSpec.IntValue poison_emitter_venom_duration;
                public final ForgeConfigSpec.DoubleValue poison_emitter_radius;
                public final ForgeConfigSpec.IntValue poison_emitter_lifetime_ticks;

                public final ForgeConfigSpec.DoubleValue feather_damage;
                public final ForgeConfigSpec.DoubleValue feather_gravity;

                public Entities(ForgeConfigSpec.Builder builder) {
                        builder.push("Attributes");
                        race_default_hp_mod = builder.comment("Global HP multiplier offset for races.")
                                        .defineInRange("race_default_hp_mod", 0.0, -100.0, 1000.0);
                        race_default_height_mod = builder.comment("Global height multiplier offset for races.")
                                        .defineInRange("race_default_height_mod", 0.0, -1.0, 10.0);
                        race_default_width_mod = builder.comment("Global width multiplier offset for races.")
                                        .defineInRange("race_default_width_mod", 0.0, -1.0, 10.0);
                        builder.pop();

                        builder.push("Entities");
                        troll_pillar_health = builder.comment("Max health for Troll Pillars.")
                                        .defineInRange("troll_pillar_health", 40.0, 1.0, 1000.0);
                        troll_pillar_armor = builder.comment("Armor for Troll Pillars.")
                                        .defineInRange("troll_pillar_armor", 10.0, 0.0, 100.0);
                        troll_pillar_follow_range = builder.comment("Follow range for Troll Pillars.")
                                        .defineInRange("troll_pillar_follow_range", 16.0, 1.0, 128.0);
                        troll_pillar_knockback_res = builder.comment("Knockback resistance for Troll Pillars.")
                                        .defineInRange("troll_pillar_knockback_res", 1.0, 0.0, 1.0);

                        poison_emitter_health = builder.comment("Max health for Poison Emitters.")
                                        .defineInRange("poison_emitter_health", 10.0, 1.0, 1000.0);
                        poison_emitter_armor = builder.comment("Armor for Poison Emitters.")
                                        .defineInRange("poison_emitter_armor", 5.0, 0.0, 100.0);
                        poison_emitter_follow_range = builder.comment("Follow range for Poison Emitters.")
                                        .defineInRange("poison_emitter_follow_range", 16.0, 1.0, 128.0);
                        poison_emitter_knockback_res = builder.comment("Knockback resistance for Poison Emitters.")
                                        .defineInRange("poison_emitter_knockback_res", 1.0, 0.0, 1.0);
                        poison_emitter_pulse_interval = builder
                                        .comment("Interval (ticks) between Poison Emitter pulses.")
                                        .defineInRange("poison_emitter_pulse_interval", 20, 1, 1200);

                        poison_emitter_radius = builder.comment("Radius (blocks) for Poison Emitters.")
                                        .defineInRange("poison_emitter_radius", 3.0, 1.0, 64.0);
                        feather_damage = builder.comment("Base damage for Harpy Feathers.")
                                        .defineInRange("feather_damage", 2.0, 0.0, 100.0);
                        feather_gravity = builder.comment("Gravity scale for Harpy Feathers.")
                                        .defineInRange("feather_gravity", 0.05, 0.0, 1.0);
                        tornado_health = builder.comment("Health for Tornado entities.")
                                        .defineInRange("tornado_health", 20.0, 1.0, 10000.0);
                        tornado_armor = builder.comment("Armor for Tornado entities.")
                                        .defineInRange("tornado_armor", 5.0, 0.0, 100.0);
                        tornado_attack_damage = builder.comment("Attack damage for Tornado entities.")
                                        .defineInRange("tornado_attack_damage", 3.0, 0.0, 100.0);
                        tornado_follow_range = builder.comment("Follow range for Tornado entities.")
                                        .defineInRange("tornado_follow_range", 16.0, 1.0, 128.0);
                        builder.pop();

                        builder.push("Mechanics");
                        troll_pillar_pulse_interval = builder.comment("Interval (ticks) between Troll Pillar pulses.")
                                        .defineInRange("troll_pillar_pulse_interval", 20, 1, 1200);
                        troll_pillar_curse_radius = builder.comment("Radius (blocks) for Troll Pillar curse.")
                                        .defineInRange("troll_pillar_curse_radius", 5.0, 1.0, 64.0);
                        troll_pillar_curse_duration = builder.comment("Duration (ticks) for Troll Pillar curse.")
                                        .defineInRange("troll_pillar_curse_duration", 100, 1, 1200);
                        troll_pillar_lifetime_ticks = builder.comment("Lifetime (ticks) for Troll Pillar entities.")
                                        .defineInRange("troll_pillar_lifetime_ticks", 600, 1, 10000);

                        tornado_flying_speed = builder.comment("Flying speed for Tornado entities.")
                                        .defineInRange("tornado_flying_speed", 0.1, 0.0, 1.0);
                        tornado_movement_speed = builder.comment("Movement speed for Tornado entities.")
                                        .defineInRange("tornado_movement_speed", 0.3, 0.0, 1.0);
                        tornado_hurricane_radius = builder.comment("Radius (blocks) for Tornado hurricane effect.")
                                        .defineInRange("tornado_hurricane_radius", 4.0, 1.0, 64.0);
                        tornado_dizziness_duration = builder.comment("Duration (ticks) for Tornado dizziness effect.")
                                        .defineInRange("tornado_dizziness_duration", 60, 1, 1200);
                        tornado_pull_force = builder.comment("Force applied to pull entities towards Tornado.")
                                        .defineInRange("tornado_pull_force", 0.05, 0.0, 1.0);

                        poison_emitter_mobile_max_health = builder.comment("Max health for mobile Poison Emitters.")
                                        .defineInRange("poison_emitter_mobile_max_health", 16.0, 1.0, 1000.0);
                        poison_emitter_mobile_movement_speed = builder
                                        .comment("Movement speed for mobile Poison Emitters.")
                                        .defineInRange("poison_emitter_mobile_movement_speed", 0.35, 0.0, 1.0);
                        poison_emitter_venom_duration = builder
                                        .comment("Duration (ticks) for Ratvenom applied by emitters.")
                                        .defineInRange("poison_emitter_venom_duration", 102, 1, 1200);
                        poison_emitter_lifetime_ticks = builder.comment("Lifetime (ticks) for Poison Emitters.")
                                        .defineInRange("poison_emitter_lifetime_ticks", 100, 1, 10000);
                        tornado_lifetime_ticks = builder.comment("Lifetime (ticks) for Tornado entities.")
                                        .defineInRange("tornado_lifetime_ticks", 200, 1, 10000);
                        builder.pop();

                        builder.push("Specialized");
                        undead_summon_cap = builder.comment("Max active summons for Undead.")
                                        .defineInRange("undead_summon_cap", 5.0, 1.0, 256.0);
                        remains_decay_time = builder.comment("Time in ticks before remains decay.")
                                        .defineInRange("remains_decay_time", 1200.0, 0.0, 100000.0);
                        dryad_blessing_cost = builder.comment("Coin cost for Dryad's blessing.")
                                        .defineInRange("dryad_blessing_cost", 500.0, 0.0, 1000000.0);
                        builder.pop();
                }
        }

        @SubscribeEvent
        public static void onLoad(final ModConfigEvent.Loading configEvent) {
                apply();
        }

        @SubscribeEvent
        public static void onReload(final ModConfigEvent.Reloading configEvent) {
                apply();
        }

        public static void apply() {
                CreRacesConfig.WIKI_BASE_URL = () -> COMMON.wiki_base_url.get();
                CreRacesConfig.WIKI_API_BASE = () -> COMMON.wiki_api_base.get();
                CreRacesConfig.WIKI_PAGE_PATH = () -> COMMON.wiki_page_path.get();
                CreRacesConfig.WIKI_ABILITY_NAMESPACE = () -> COMMON.wiki_ability_namespace.get();
                CreRacesConfig.DISABLE_REMOTE_DOCS = () -> COMMON.disable_remote_docs.get();
                CreRacesConfig.DOC_FETCH_TIMEOUT_SECONDS = () -> COMMON.doc_fetch_timeout_seconds.get();
                CreRacesConfig.DOC_CACHE_DIR = () -> COMMON.doc_cache_dir.get();
                CreRacesConfig.DOC_CACHE_FILENAME = () -> COMMON.doc_cache_filename.get();

                CreRacesConfig.FORCED_SELECTION = () -> COMMON.forced_selection.get();
                CreRacesConfig.TEAM_INVITE_TIMEOUT_TICKS = () -> COMMON.team_invite_timeout_ticks.get();
                CreRacesConfig.ABILITY_HASTE_CAP = () -> COMMON.ability_haste_cap.get();
                CreRacesConfig.RACIAL_AD_MULTIPLIER = () -> COMMON.racial_ad_multiplier.get();
                CreRacesConfig.BREAK_BLOCKS_MAX_RADIUS = () -> COMMON.break_blocks_max_radius.get();
                CreRacesConfig.AOE_MAX_RADIUS = () -> COMMON.aoe_max_radius.get();
                CreRacesConfig.BEAM_MAX_LENGTH = () -> COMMON.beam_max_length.get();
                CreRacesConfig.ENTITY_DATA_KEY_MAX_LENGTH = () -> COMMON.entity_data_key_max_length.get();
                CreRacesConfig.NETWORK_TEAM_NAME_MAX_LEN = () -> COMMON.network_team_name_max_len.get();
                CreRacesConfig.NETWORK_PLAYER_NAME_MAX_LEN = () -> COMMON.network_player_name_max_len.get();

                CreRacesConfig.MINI_BUILD_ENABLED = () -> COMMON.mini_build_enabled.get();
                CreRacesConfig.MINI_FURNACE_ENABLED = () -> COMMON.mini_furnace_enabled.get();
                CreRacesConfig.MINI_PLACE_WHITELIST_ENABLED = () -> COMMON.mini_place_whitelist_enabled.get();
                CreRacesConfig.RACE_SOCIAL_DEFENSE_RANGE = () -> COMMON.social_defense_range.get();
                CreRacesConfig.MICRO_BLOCK_LIGHT_PER_TORCH = () -> COMMON.micro_block_light_per_torch.get();
                CreRacesConfig.MICRO_BLOCK_MAX_LIGHT = () -> COMMON.micro_block_max_light.get();

                CreRacesConfig.HUMAN_SELECTION_ALLOWED = () -> COMMON.human_selection_allowed.get();
                CreRacesConfig.COIN_DROP_ENABLED = () -> COMMON.coin_drop_enabled.get();
                CreRacesConfig.COIN_TRANSFER_ALLOWED = () -> COMMON.coin_transfer_allowed.get();
                CreRacesConfig.SAG_WINGS_RAIN_FLIGHT = () -> COMMON.sag_wings_rain_flight.get();
                CreRacesConfig.GENDER_SYSTEM_ENABLED = () -> COMMON.gender_system_enabled.get();
                CreRacesConfig.RACE_FRIENDLY_MOBS_ENABLED = () -> COMMON.race_friendly_mobs_enabled.get();
                CreRacesConfig.RITUAL_MODE = () -> COMMON.ritual_mode.get();

                CreRacesConfig.POCKET_DIM_SPACING = () -> COMMON.pocket_dim_spacing.get();
                CreRacesConfig.POCKET_EXPANSION_LIMIT = () -> COMMON.pocket_expansion_limit.get();
                CreRacesConfig.POCKET_EXPANSION_COST = () -> COMMON.pocket_expansion_cost.get();

                CreRacesConfig.MINI_MODEL_CACHE_SIZE = () -> CLIENT.mini_model_cache_size.get();
                CreRacesConfig.VISUAL_SYNC_DISTANCE = () -> CLIENT.visual_sync_distance.get();
                CreRacesConfig.RACE_OVERLAY_OFFSET_X = () -> CLIENT.race_overlay_offset_x.get();
                CreRacesConfig.RACE_OVERLAY_OFFSET_Y = () -> CLIENT.race_overlay_offset_y.get();
                CreRacesConfig.RACE_OVERLAYS_ENABLED = () -> CLIENT.race_overlays_enabled.get();
                CreRacesConfig.ENGINE_POPUPS_ENABLED = () -> CLIENT.engine_popups_enabled.get();

                CreRacesConfig.RACE_DEFAULT_HP_MOD = () -> ENTITIES.race_default_hp_mod.get();
                CreRacesConfig.RACE_DEFAULT_HEIGHT_MOD = () -> ENTITIES.race_default_height_mod.get();
                CreRacesConfig.RACE_DEFAULT_WIDTH_MOD = () -> ENTITIES.race_default_width_mod.get();

                CreRacesConfig.ENTITY_TROLL_PILLAR_MAX_HEALTH = () -> ENTITIES.troll_pillar_health.get();
                CreRacesConfig.ENTITY_TROLL_PILLAR_ARMOR = () -> ENTITIES.troll_pillar_armor.get();
                CreRacesConfig.ENTITY_TROLL_PILLAR_FOLLOW_RANGE = () -> ENTITIES.troll_pillar_follow_range.get();
                CreRacesConfig.ENTITY_TROLL_PILLAR_KNOCKBACK_RES = () -> ENTITIES.troll_pillar_knockback_res.get();

                CreRacesConfig.ENTITY_POISON_EMITTER_HEALTH = () -> ENTITIES.poison_emitter_health.get();
                CreRacesConfig.ENTITY_POISON_EMITTER_ARMOR = () -> ENTITIES.poison_emitter_armor.get();
                CreRacesConfig.ENTITY_POISON_EMITTER_FOLLOW_RANGE = () -> ENTITIES.poison_emitter_follow_range.get();
                CreRacesConfig.ENTITY_POISON_EMITTER_KNOCKBACK_RES = () -> ENTITIES.poison_emitter_knockback_res.get();
                CreRacesConfig.ENTITY_POISON_EMITTER_PULSE_INTERVAL = () -> ENTITIES.poison_emitter_pulse_interval
                                .get();

                CreRacesConfig.ENTITY_FEATHER_DAMAGE = () -> ENTITIES.feather_damage.get();
                CreRacesConfig.ENTITY_TORNADO_HEALTH = () -> ENTITIES.tornado_health.get();
                CreRacesConfig.ENTITY_TORNADO_ARMOR = () -> ENTITIES.tornado_armor.get();
                CreRacesConfig.ENTITY_TORNADO_ATTACK_DAMAGE = () -> ENTITIES.tornado_attack_damage.get();
                CreRacesConfig.ENTITY_TORNADO_FOLLOW_RANGE = () -> ENTITIES.tornado_follow_range.get();

                CreRacesConfig.ENTITY_TROLL_PILLAR_PULSE_INTERVAL = () -> ENTITIES.troll_pillar_pulse_interval.get();
                CreRacesConfig.ENTITY_TROLL_PILLAR_CURSE_RADIUS = () -> ENTITIES.troll_pillar_curse_radius.get();
                CreRacesConfig.ENTITY_TROLL_PILLAR_CURSE_DURATION = () -> ENTITIES.troll_pillar_curse_duration.get();
                CreRacesConfig.ENTITY_TROLL_PILLAR_LIFETIME_TICKS = () -> ENTITIES.troll_pillar_lifetime_ticks.get();

                CreRacesConfig.ENTITY_TORNADO_FLYING_SPEED = () -> ENTITIES.tornado_flying_speed.get();
                CreRacesConfig.ENTITY_TORNADO_MOVEMENT_SPEED = () -> ENTITIES.tornado_movement_speed.get();
                CreRacesConfig.ENTITY_TORNADO_RADIUS = () -> (float) (double) ENTITIES.tornado_hurricane_radius.get();
                CreRacesConfig.ENTITY_TORNADO_DIZZINESS_DURATION = () -> ENTITIES.tornado_dizziness_duration.get();
                CreRacesConfig.ENTITY_TORNADO_PULL_FORCE = () -> ENTITIES.tornado_pull_force.get();

                CreRacesConfig.ENTITY_POISON_EMITTER_MOBILE_MAX_HEALTH = () -> ENTITIES.poison_emitter_mobile_max_health
                                .get();
                CreRacesConfig.ENTITY_POISON_EMITTER_MOBILE_MOVEMENT_SPEED = () -> ENTITIES.poison_emitter_mobile_movement_speed
                                .get();
                CreRacesConfig.ENTITY_POISON_EMITTER_VENOM_DURATION = () -> ENTITIES.poison_emitter_venom_duration
                                .get();
                CreRacesConfig.ENTITY_POISON_EMITTER_LIFETIME_TICKS = () -> ENTITIES.poison_emitter_lifetime_ticks
                                .get();
                CreRacesConfig.ENTITY_POISON_EMITTER_RADIUS = () -> ENTITIES.poison_emitter_radius.get();
                CreRacesConfig.ENTITY_TORNADO_LIFETIME_TICKS = () -> ENTITIES.tornado_lifetime_ticks.get();

                CreRacesConfig.ENTITY_FEATHER_GRAVITY = () -> ENTITIES.feather_gravity.get();

                CreRacesConfig.UNDEAD_SUMMON_CAP = () -> ENTITIES.undead_summon_cap.get();
                CreRacesConfig.REMAINS_DECAY_TIME = () -> (int) (double) ENTITIES.remains_decay_time.get();
                CreRacesConfig.DRYAD_BLESSING_COST = () -> ENTITIES.dryad_blessing_cost.get();
        }
}
