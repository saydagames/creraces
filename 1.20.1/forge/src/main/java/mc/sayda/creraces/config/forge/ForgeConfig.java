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
                public final ForgeConfigSpec.ConfigValue<Boolean> disable_remote_docs;
                public final ForgeConfigSpec.ConfigValue<Integer> doc_fetch_timeout_seconds;
                public final ForgeConfigSpec.ConfigValue<String> doc_cache_dir;
                public final ForgeConfigSpec.ConfigValue<String> doc_cache_filename;

                public final ForgeConfigSpec.ConfigValue<Boolean> forced_selection;
                public final ForgeConfigSpec.ConfigValue<Double> ability_haste_cap;
                public final ForgeConfigSpec.ConfigValue<Double> racial_ad_multiplier;

                public final ForgeConfigSpec.ConfigValue<Boolean> coin_drop_enabled;
                public final ForgeConfigSpec.ConfigValue<Boolean> sag_wings;
                public final ForgeConfigSpec.ConfigValue<Boolean> gstate_enabled;
                public final ForgeConfigSpec.ConfigValue<Integer> ritual_mode;

                // Safety caps (0 = disabled)
                public final ForgeConfigSpec.ConfigValue<Integer> break_blocks_max_radius;
                public final ForgeConfigSpec.ConfigValue<Integer> aoe_max_radius;
                public final ForgeConfigSpec.ConfigValue<Integer> beam_max_length;
                public final ForgeConfigSpec.ConfigValue<Integer> entity_data_key_max_length;

                public final ForgeConfigSpec.ConfigValue<Integer> network_team_name_max_len;
                public final ForgeConfigSpec.ConfigValue<Integer> network_player_name_max_len;

                public final ForgeConfigSpec.ConfigValue<Boolean> mini_build_enabled;
                public final ForgeConfigSpec.ConfigValue<Boolean> mini_furnace_enabled;
                public final ForgeConfigSpec.ConfigValue<Boolean> mini_place_whitelist_enabled;
                public final ForgeConfigSpec.ConfigValue<Double> social_defense_range;
                public final ForgeConfigSpec.ConfigValue<Integer> micro_block_light_per_torch;
                public final ForgeConfigSpec.ConfigValue<Integer> micro_block_max_light;
                public final ForgeConfigSpec.ConfigValue<Boolean> mini_build_requires_learned;
                public final ForgeConfigSpec.ConfigValue<Double> mini_crafting_distance_sqr;
                public final ForgeConfigSpec.ConfigValue<Long> mini_placement_spam_threshold_ms;
                public final ForgeConfigSpec.ConfigValue<Double> mini_block_reach_margin;

                // Pocket
                public final ForgeConfigSpec.ConfigValue<String> action_default_pocket_dim;
                public final ForgeConfigSpec.ConfigValue<String> action_default_pocket_structure;
                public final ForgeConfigSpec.ConfigValue<Double> action_default_pocket_spawn_x_offset;
                public final ForgeConfigSpec.ConfigValue<Double> action_default_pocket_spawn_y_offset;
                public final ForgeConfigSpec.ConfigValue<Double> action_default_pocket_spawn_z_offset;
                public final ForgeConfigSpec.ConfigValue<Double> pocket_dim_spacing;
                public final ForgeConfigSpec.ConfigValue<Double> pocket_dim_y;
                public final ForgeConfigSpec.ConfigValue<Integer> pocket_expansion_limit;
                public final ForgeConfigSpec.ConfigValue<Double> pocket_expansion_cost;

                // Technical/Internal
                public final ForgeConfigSpec.ConfigValue<Boolean> race_addons_enabled;
                public final ForgeConfigSpec.ConfigValue<Boolean> lore_addons_enabled;

                public final ForgeConfigSpec.ConfigValue<Integer> spirit_spawn_check_radius;
                public final ForgeConfigSpec.ConfigValue<Integer> customization_value_max_length;
                public final ForgeConfigSpec.ConfigValue<Double> resource_min_capacity;
                public final ForgeConfigSpec.ConfigValue<Integer> passive_execution_interval;
                public final ForgeConfigSpec.ConfigValue<Double> sunlight_equipment_break_chance;
                public final ForgeConfigSpec.ConfigValue<Integer> sunlight_burn_seconds;
                public final ForgeConfigSpec.ConfigValue<Integer> passive_effect_buffer_ticks;
                public final ForgeConfigSpec.ConfigValue<Integer> passive_default_max_food;
                public final ForgeConfigSpec.ConfigValue<Double> passive_default_max_saturation;

                public final ForgeConfigSpec.ConfigValue<Double> rat_venom_scaling;

                public Common(ForgeConfigSpec.Builder builder) {
                        wiki_base_url = builder.comment("The base URL for the wiki").define("wiki_base_url",
                                        CreRacesConfig.WIKI_BASE_URL.get());
                        wiki_api_base = builder.comment("The API base URL for the wiki").define("wiki_api_base",
                                        CreRacesConfig.WIKI_API_BASE.get());
                        wiki_page_path = builder.comment("The path part of the wiki URL").define("wiki_page_path",
                                        CreRacesConfig.WIKI_PAGE_PATH.get());
                        wiki_ability_namespace = builder.comment("The prefix for ability pages")
                                        .define("wiki_ability_namespace", CreRacesConfig.WIKI_ABILITY_NAMESPACE.get());
                        disable_remote_docs = builder.comment("Set to true to disable remote documentation fetching")
                                        .define("disable_remote_docs",
                                                        CreRacesConfig.DISABLE_REMOTE_DOCS.get());
                        doc_fetch_timeout_seconds = builder
                                        .comment("Timeout in seconds for fetching remote documentation")
                                        .defineInRange("doc_fetch_timeout_seconds",
                                                        CreRacesConfig.DOC_FETCH_TIMEOUT_SECONDS.get(), 1, 60);
                        doc_cache_dir = builder.comment("Directory name for the documentation cache")
                                        .define("doc_cache_dir", CreRacesConfig.DOC_CACHE_DIR.get());
                        doc_cache_filename = builder.comment("Filename for the documentation cache")
                                        .define("doc_cache_filename", CreRacesConfig.DOC_CACHE_FILENAME.get());
                        builder.pop();

                        builder.push("Gameplay");
                        forced_selection = builder.comment(
                                        "Keep the selection GUI open until a race is chosen. Also grants invulnerability while choosing.")
                                        .define("forced_selection", CreRacesConfig.FORCED_SELECTION.get());
                        ability_haste_cap = builder.comment("The soft cap for Ability Haste. Max value is 100.")
                                        .defineInRange("ability_haste_cap", CreRacesConfig.ABILITY_HASTE_CAP.get(), 0.0,
                                                        100.0);
                        racial_ad_multiplier = builder.comment(
                                        "Multiplier for Racial Attack Damage (AD). Default is 0.01 (1% per point).")
                                        .defineInRange("racial_ad_multiplier",
                                                        CreRacesConfig.RACIAL_AD_MULTIPLIER.get(), 0.0, 100.0);
                        coin_drop_enabled = builder.comment("Enable loot-based coin drops.")
                                        .define("coin_drop_enabled", CreRacesConfig.COIN_DROP_ENABLED.get());
                        sag_wings = builder.comment("Allow wings flight to sag during rain.")
                                        .define("sag_wings",
                                                        CreRacesConfig.SAG_WINGS.get());
                        gstate_enabled = builder.comment("Enable the gender system (affects some race visuals).")
                                        .define("gstate_enabled",
                                                        CreRacesConfig.GSTATE_ENABLED.get());
                        ritual_mode = builder.comment("Ritual Mode: 0=Disabled, 1=Human Only, 2=Reset Allowed.")
                                        .defineInRange("ritual_mode", CreRacesConfig.RITUAL_MODE.get(), 0, 2);
                        builder.pop();

                        builder.push("Potion Effects");
                        rat_venom_scaling = builder.comment("Damage scaling factor for Rat Venom per stack. Default 0.2.")
                                        .defineInRange("rat_venom_scaling", CreRacesConfig.RAT_VENOM_SCALING.get(), 0.0, 10.0);
                        builder.pop();

                        builder.push("Safety");
                        break_blocks_max_radius = builder.comment("Max radius for creraces:break_blocks. 0 = no cap.")
                                        .defineInRange("break_blocks_max_radius",
                                                        CreRacesConfig.BREAK_BLOCKS_MAX_RADIUS.get(), 0, 256);
                        aoe_max_radius = builder
                                        .comment("Max radius for creraces:aoe / creraces:apply_effect AoE. 0 = no cap.")
                                        .defineInRange("aoe_max_radius", CreRacesConfig.AOE_MAX_RADIUS.get(), 0,
                                                        512);
                        beam_max_length = builder.comment("Max length for creraces:beam. 0 = no cap.")
                                        .defineInRange("beam_max_length", CreRacesConfig.BEAM_MAX_LENGTH.get(), 0,
                                                        512);
                        entity_data_key_max_length = builder.comment(
                                        "Max character length for creraces:modify_entity_data keys. 0 = no cap.")
                                        .defineInRange("entity_data_key_max_length",
                                                        CreRacesConfig.ENTITY_DATA_KEY_MAX_LENGTH.get(), 0, 1024);
                        network_team_name_max_len = builder.comment("Max length for team names.")
                                        .defineInRange("network_team_name_max_len",
                                                        CreRacesConfig.NETWORK_TEAM_NAME_MAX_LEN.get(), 1, 256);
                        network_player_name_max_len = builder.comment("Max length for player names in teams.")
                                        .defineInRange("network_player_name_max_len",
                                                        CreRacesConfig.NETWORK_PLAYER_NAME_MAX_LEN.get(), 1, 64);
                        customization_value_max_length = builder.defineInRange("customization_value_max_length",
                                        CreRacesConfig.CUSTOMIZATION_VALUE_MAX_LENGTH.get(), 1, 2048);
                        builder.pop();

                        builder.push("MiniBuild");
                        mini_build_enabled = builder
                                        .comment("Enable the Mini Build System. ON by default.")
                                        .define("mini_build_enabled",
                                                        CreRacesConfig.MINI_BUILD_ENABLED.get());
                        mini_furnace_enabled = builder.comment("Enable the Mini Furnace system. ON by default.")
                                        .define("mini_furnace_enabled",
                                                        CreRacesConfig.MINI_FURNACE_ENABLED.get());
                        mini_place_whitelist_enabled = builder.comment("Enable the whitelist for Mini Place system.")
                                        .define("mini_place_whitelist_enabled",
                                                        CreRacesConfig.MINI_PLACE_WHITELIST_ENABLED.get());
                        micro_block_light_per_torch = builder.comment("Light level added per torch in a micro-block.")
                                        .defineInRange("micro_block_light_per_torch",
                                                        CreRacesConfig.MICRO_BLOCK_LIGHT_PER_TORCH.get(), 0, 15);
                        micro_block_max_light = builder.comment("Maximum light level for a micro-block.")
                                        .defineInRange("micro_block_max_light",
                                                        CreRacesConfig.MICRO_BLOCK_MAX_LIGHT.get(), 0, 15);
                        mini_build_requires_learned = builder.comment("Players must learn/unlock minibuilding.")
                                        .define("mini_build_requires_learned",
                                                        CreRacesConfig.MINIBUILD_REQUIRES_LEARNED.get());
                        mini_crafting_distance_sqr = builder.comment("Distance squared at which minibuilding works.")
                                        .defineInRange("mini_crafting_distance_sqr",
                                                        CreRacesConfig.MINI_CRAFTING_DISTANCE_SQR.get(), 1.0,
                                                        1024.0);
                        mini_placement_spam_threshold_ms = builder.comment("Minimum MS between mini block placements.")
                                        .defineInRange("mini_placement_spam_threshold_ms",
                                                        CreRacesConfig.MINI_PLACEMENT_SPAM_THRESHOLD_MS.get(),
                                                        0L, 1000L);
                        mini_block_reach_margin = builder.comment("Extra reach margin for working with mini blocks.")
                                        .defineInRange("mini_block_reach_margin",
                                                        CreRacesConfig.MINI_BLOCK_REACH_MARGIN.get(), 0.0,
                                                        5.0);
                        builder.pop();

                        builder.push("Pockets");
                        action_default_pocket_dim = builder.comment("Default dimension ID for player pockets.")
                                        .define("action_default_pocket_dim",
                                                        CreRacesConfig.ACTION_DEFAULT_POCKET_DIM.get());
                        action_default_pocket_structure = builder.comment("Default structure ID for new pockets.")
                                        .define("action_default_pocket_structure",
                                                        CreRacesConfig.ACTION_DEFAULT_POCKET_STRUCTURE.get());
                        action_default_pocket_spawn_x_offset = builder
                                        .defineInRange("action_default_pocket_spawn_x_offset",
                                                        CreRacesConfig.ACTION_DEFAULT_POCKET_SPAWN_X_OFFSET
                                                                        .get(),
                                                        -100.0, 100.0);
                        action_default_pocket_spawn_y_offset = builder
                                        .defineInRange("action_default_pocket_spawn_y_offset",
                                                        CreRacesConfig.ACTION_DEFAULT_POCKET_SPAWN_Y_OFFSET
                                                                        .get(),
                                                        -100.0, 100.0);
                        action_default_pocket_spawn_z_offset = builder
                                        .defineInRange("action_default_pocket_spawn_z_offset",
                                                        CreRacesConfig.ACTION_DEFAULT_POCKET_SPAWN_Z_OFFSET
                                                                        .get(),
                                                        -100.0, 100.0);

                        pocket_dim_spacing = builder.comment(
                                        "Distance between player pockets. WARNING: Do not decrease on existing worlds.")
                                        .defineInRange("pocket_dim_spacing",
                                                        CreRacesConfig.POCKET_DIM_SPACING.get(), 100.0,
                                                        100000.0);
                        pocket_dim_y = builder.comment("Y coordinate for pocket floors.")
                                        .defineInRange("pocket_dim_y", CreRacesConfig.POCKET_DIM_Y.get(), 0.0,
                                                        255.0);
                        pocket_expansion_limit = builder.comment("Max number of expansions allowed per player.")
                                        .defineInRange("pocket_expansion_limit",
                                                        CreRacesConfig.POCKET_EXPANSION_LIMIT.get(), 0, 100);
                        pocket_expansion_cost = builder.comment("Coin cost per expansion.")
                                        .defineInRange("pocket_expansion_cost",
                                                        CreRacesConfig.POCKET_EXPANSION_COST.get(), 0.0,
                                                        1000000.0);
                        builder.pop();

                        builder.push("Internal");
                        resource_min_capacity = builder.defineInRange("resource_min_capacity",
                                        CreRacesConfig.RESOURCE_MIN_CAPACITY.get(), 0.0, 1000.0);
                        passive_execution_interval = builder.defineInRange("passive_execution_interval",
                                        CreRacesConfig.PASSIVE_EXECUTION_INTERVAL.get(), 1, 1200);
                        sunlight_equipment_break_chance = builder.defineInRange("sunlight_equipment_break_chance",
                                        CreRacesConfig.SUNLIGHT_EQUIPMENT_BREAK_CHANCE.get(),
                                        0.0, 1.0);
                        sunlight_burn_seconds = builder.defineInRange("sunlight_burn_seconds",
                                        CreRacesConfig.SUNLIGHT_BURN_SECONDS.get(), 0, 3600);
                        passive_effect_buffer_ticks = builder.defineInRange("passive_effect_buffer_ticks",
                                        CreRacesConfig.PASSIVE_EFFECT_BUFFER_TICKS.get(), 0, 1200);
                        passive_default_max_food = builder.defineInRange("passive_default_max_food",
                                        CreRacesConfig.PASSIVE_DEFAULT_MAX_FOOD.get(), 1, 100);
                        passive_default_max_saturation = builder.defineInRange("passive_default_max_saturation",
                                        CreRacesConfig.PASSIVE_DEFAULT_MAX_SATURATION.get(),
                                        0.0, 100.0);
                        race_addons_enabled = builder.define("race_addons_enabled",
                                        CreRacesConfig.RACE_ADDONS_ENABLED.get());
                        lore_addons_enabled = builder.define("lore_addons_enabled",
                                        CreRacesConfig.LORE_ADDONS_ENABLED.get());
                        spirit_spawn_check_radius = builder.defineInRange("spirit_spawn_check_radius",
                                        CreRacesConfig.SPIRIT_SPAWN_CHECK_RADIUS.get(), 1, 512);
                        builder.pop();

                        builder.push("Social");
                        social_defense_range = builder.comment("Range (blocks) for social passive defense assistance.")
                                        .defineInRange("social_defense_range",
                                                        CreRacesConfig.RACE_SOCIAL_DEFENSE_RANGE.get(), 0.0,
                                                        128.0);
                        builder.pop();
                }
        }

        public static class Client {
                public final ForgeConfigSpec.ConfigValue<Integer> mini_model_cache_size;
                public final ForgeConfigSpec.ConfigValue<Integer> visual_sync_distance;
                public final ForgeConfigSpec.ConfigValue<Integer> race_overlay_offset_x;
                public final ForgeConfigSpec.ConfigValue<Integer> race_overlay_offset_y;
                public final ForgeConfigSpec.ConfigValue<Boolean> race_overlays_enabled;
                public final ForgeConfigSpec.ConfigValue<Boolean> engine_popups_enabled;
                public final ForgeConfigSpec.ConfigValue<Integer> mini_dummy_cache_size;
                public final ForgeConfigSpec.ConfigValue<Integer> spirit_realm_tint_color;
                public final ForgeConfigSpec.ConfigValue<Double> spirit_realm_moon_alpha;
                public final ForgeConfigSpec.ConfigValue<Double> spirit_realm_moon_size;

                public Client(ForgeConfigSpec.Builder builder) {
                        builder.push("Rendering");
                        mini_model_cache_size = builder.comment("Max models stored in memory for Mini-Blocks.")
                                        .defineInRange("mini_model_cache_size",
                                                        CreRacesConfig.MINI_MODEL_CACHE_SIZE.get(), 1, 2048);
                        visual_sync_distance = builder
                                        .comment("Distance (blocks) for visual sync (beams, tethers, animations).")
                                        .defineInRange("visual_sync_distance",
                                                        CreRacesConfig.VISUAL_SYNC_DISTANCE.get(), 1, 512);
                        builder.pop();

                        builder.push("HUD");
                        race_overlay_offset_x = builder.comment("X offset for the race overlay HUD.")
                                        .defineInRange("race_overlay_offset_x",
                                                        CreRacesConfig.RACE_OVERLAY_OFFSET_X.get(), -2048, 2048);
                        race_overlay_offset_y = builder.comment("Y offset for the race overlay HUD.")
                                        .defineInRange("race_overlay_offset_y",
                                                        CreRacesConfig.RACE_OVERLAY_OFFSET_Y.get(), -2048, 2048);
                        race_overlays_enabled = builder.comment("Enable racial UI overlays.")
                                        .define("race_overlays_enabled",
                                                        CreRacesConfig.RACE_OVERLAYS_ENABLED.get());
                        engine_popups_enabled = builder.comment("Enable engine-driven popup messages.")
                                        .define("engine_popups_enabled",
                                                        CreRacesConfig.ENGINE_POPUPS_ENABLED.get());
                        mini_dummy_cache_size = builder.defineInRange("mini_dummy_cache_size",
                                        CreRacesConfig.MINI_DUMMY_CACHE_SIZE.get(), 1, 256);
                        spirit_realm_tint_color = builder.defineInRange("spirit_realm_tint_color",
                                        CreRacesConfig.SPIRIT_REALM_TINT_COLOR.get(), 0,
                                        0xFFFFFF);
                        spirit_realm_moon_alpha = builder.defineInRange("spirit_realm_moon_alpha",
                                        (double) CreRacesConfig.SPIRIT_REALM_MOON_ALPHA.get(), 0.0, 1.0);
                        spirit_realm_moon_size = builder.defineInRange("spirit_realm_moon_size",
                                        (double) CreRacesConfig.SPIRIT_REALM_MOON_SIZE.get(), 1.0, 100.0);
                        builder.pop();
                }
        }

        public static class Entities {

                public final ForgeConfigSpec.DoubleValue troll_pillar_health;
                public final ForgeConfigSpec.DoubleValue troll_pillar_armor;
                public final ForgeConfigSpec.DoubleValue troll_pillar_follow_range;
                public final ForgeConfigSpec.DoubleValue troll_pillar_knockback_res;
                public final ForgeConfigSpec.DoubleValue poison_emitter_health;
                public final ForgeConfigSpec.DoubleValue poison_emitter_armor;
                public final ForgeConfigSpec.DoubleValue poison_emitter_follow_range;
                public final ForgeConfigSpec.DoubleValue poison_emitter_knockback_res;
                public final ForgeConfigSpec.DoubleValue tornado_health;
                public final ForgeConfigSpec.DoubleValue tornado_follow_range;

                public final ForgeConfigSpec.DoubleValue remains_decay_time;
                public final ForgeConfigSpec.DoubleValue remains_health;

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
                public final ForgeConfigSpec.DoubleValue poison_emitter_radius;
                public final ForgeConfigSpec.IntValue poison_emitter_lifetime_ticks;

                public final ForgeConfigSpec.DoubleValue feather_damage;
                public final ForgeConfigSpec.DoubleValue feather_gravity;

                public Entities(ForgeConfigSpec.Builder builder) {
                        builder.push("Entities");
                        troll_pillar_health = builder.comment("Max health for Troll Pillars.")
                                        .defineInRange("troll_pillar_health",
                                                        CreRacesConfig.ENTITY_TROLL_PILLAR_MAX_HEALTH.get(),
                                                        1.0, 1000.0);
                        troll_pillar_armor = builder.comment("Armor for Troll Pillars.")
                                        .defineInRange("troll_pillar_armor",
                                                        CreRacesConfig.ENTITY_TROLL_PILLAR_ARMOR.get(), 0.0,
                                                        100.0);
                        troll_pillar_follow_range = builder.comment("Follow range for Troll Pillars.")
                                        .defineInRange("troll_pillar_follow_range",
                                                        CreRacesConfig.ENTITY_TROLL_PILLAR_FOLLOW_RANGE.get(),
                                                        1.0, 128.0);
                        troll_pillar_knockback_res = builder.comment("Knockback resistance for Troll Pillars.")
                                        .defineInRange("troll_pillar_knockback_res",
                                                        CreRacesConfig.ENTITY_TROLL_PILLAR_KNOCKBACK_RES.get(),
                                                        0.0, 1.0);

                        poison_emitter_health = builder.comment("Max health for Poison Emitters.")
                                        .defineInRange("poison_emitter_health",
                                                        CreRacesConfig.ENTITY_POISON_EMITTER_HEALTH.get(), 1.0,
                                                        1000.0);
                        poison_emitter_armor = builder.comment("Armor for Poison Emitters.")
                                        .defineInRange("poison_emitter_armor",
                                                        CreRacesConfig.ENTITY_POISON_EMITTER_ARMOR.get(), 0.0,
                                                        100.0);
                        poison_emitter_follow_range = builder.comment("Follow range for Poison Emitters.")
                                        .defineInRange("poison_emitter_follow_range",
                                                        CreRacesConfig.ENTITY_POISON_EMITTER_FOLLOW_RANGE
                                                                        .get(),
                                                        1.0, 128.0);
                        poison_emitter_knockback_res = builder.comment("Knockback resistance for Poison Emitters.")
                                        .defineInRange("poison_emitter_knockback_res",
                                                        CreRacesConfig.ENTITY_POISON_EMITTER_KNOCKBACK_RES
                                                                        .get(),
                                                        0.0, 1.0);

                        poison_emitter_radius = builder.comment("Radius (blocks) for Poison Emitters.")
                                        .defineInRange("poison_emitter_radius",
                                                        CreRacesConfig.ENTITY_POISON_EMITTER_RADIUS.get(), 1.0,
                                                        64.0);
                        feather_damage = builder.comment("Base damage for Harpy Feathers.")
                                        .defineInRange("feather_damage",
                                                        CreRacesConfig.ENTITY_FEATHER_DAMAGE.get(), 0.0,
                                                        100.0);
                        feather_gravity = builder.comment("Gravity scale for Harpy Feathers.")
                                        .defineInRange("feather_gravity",
                                                        CreRacesConfig.ENTITY_FEATHER_GRAVITY.get(), 0.0, 1.0);
                        tornado_health = builder.comment("Health for Tornado entities.")
                                        .defineInRange("tornado_health",
                                                        CreRacesConfig.ENTITY_TORNADO_HEALTH.get(), 1.0,
                                                        10000.0);
                        tornado_armor = builder.comment("Armor for Tornado entities.")
                                        .defineInRange("tornado_armor",
                                                        CreRacesConfig.ENTITY_TORNADO_ARMOR.get(), 0.0, 100.0);
                        tornado_attack_damage = builder.comment("Attack damage for Tornado entities.")
                                        .defineInRange("tornado_attack_damage",
                                                        CreRacesConfig.ENTITY_TORNADO_ATTACK_DAMAGE.get(), 0.0,
                                                        100.0);
                        tornado_follow_range = builder.comment("Follow range for Tornado entities.")
                                        .defineInRange("tornado_follow_range",
                                                        CreRacesConfig.ENTITY_TORNADO_FOLLOW_RANGE.get(), 1.0,
                                                        128.0);
                        builder.pop();

                        builder.push("Mechanics");
                        troll_pillar_pulse_interval = builder.comment("Interval (ticks) between Troll Pillar pulses.")
                                        .defineInRange("troll_pillar_pulse_interval",
                                                        CreRacesConfig.ENTITY_TROLL_PILLAR_PULSE_INTERVAL.get(),
                                                        1, 1200);
                        troll_pillar_curse_radius = builder.comment("Radius (blocks) for Troll Pillar curse.")
                                        .defineInRange("troll_pillar_curse_radius",
                                                        CreRacesConfig.ENTITY_TROLL_PILLAR_CURSE_RADIUS.get(),
                                                        1.0, 64.0);
                        troll_pillar_curse_duration = builder.comment("Duration (ticks) for Troll Pillar curse.")
                                        .defineInRange("troll_pillar_curse_duration",
                                                        CreRacesConfig.ENTITY_TROLL_PILLAR_CURSE_DURATION.get(),
                                                        1, 1200);
                        troll_pillar_lifetime_ticks = builder.comment("Lifetime (ticks) for Troll Pillar entities.")
                                        .defineInRange("troll_pillar_lifetime_ticks",
                                                        CreRacesConfig.ENTITY_TROLL_PILLAR_LIFETIME_TICKS.get(),
                                                        1, 10000);

                        tornado_flying_speed = builder.comment("Flying speed for Tornado entities.")
                                        .defineInRange("tornado_flying_speed",
                                                        CreRacesConfig.ENTITY_TORNADO_FLYING_SPEED.get(), 0.0,
                                                        1.0);
                        tornado_movement_speed = builder.comment("Movement speed for Tornado entities.")
                                        .defineInRange("tornado_movement_speed",
                                                        CreRacesConfig.ENTITY_TORNADO_MOVEMENT_SPEED.get(),
                                                        0.0, 1.0);
                        tornado_hurricane_radius = builder.comment("Radius (blocks) for Tornado hurricane effect.")
                                        .defineInRange("tornado_hurricane_radius",
                                                        (double) CreRacesConfig.ENTITY_TORNADO_RADIUS.get(), 1.0, 64.0);
                        tornado_dizziness_duration = builder.comment("Duration (ticks) for Tornado dizziness effect.")
                                        .defineInRange("tornado_dizziness_duration",
                                                        CreRacesConfig.ENTITY_TORNADO_DIZZINESS_DURATION.get(), 1,
                                                        1200);
                        tornado_pull_force = builder.comment("Force applied to pull entities towards Tornado.")
                                        .defineInRange("tornado_pull_force",
                                                        CreRacesConfig.ENTITY_TORNADO_PULL_FORCE.get(), 0.0,
                                                        1.0);

                        poison_emitter_mobile_max_health = builder.comment("Max health for mobile Poison Emitters.")
                                        .defineInRange("poison_emitter_mobile_max_health",
                                                        CreRacesConfig.ENTITY_POISON_EMITTER_MOBILE_MAX_HEALTH
                                                                        .get(),
                                                        1.0, 1000.0);
                        poison_emitter_mobile_movement_speed = builder
                                        .comment("Movement speed for mobile Poison Emitters.")
                                        .defineInRange("poison_emitter_mobile_movement_speed",
                                                        CreRacesConfig.ENTITY_POISON_EMITTER_MOBILE_MOVEMENT_SPEED
                                                                        .get(),
                                                        0.0, 1.0);
                        poison_emitter_lifetime_ticks = builder.comment("Lifetime (ticks) for Poison Emitters.")
                                        .defineInRange("poison_emitter_lifetime_ticks",
                                                        CreRacesConfig.ENTITY_POISON_EMITTER_LIFETIME_TICKS.get(),
                                                        1, 10000);
                        tornado_lifetime_ticks = builder.comment("Lifetime (ticks) for Tornado entities.")
                                        .defineInRange("tornado_lifetime_ticks",
                                                        CreRacesConfig.ENTITY_TORNADO_LIFETIME_TICKS.get(), 1,
                                                        10000);
                        builder.pop();

                        builder.push("Specialized");
                        remains_decay_time = builder.comment("Time in ticks before remains decay.")
                                        .defineInRange("remains_decay_time",
                                                        (double) CreRacesConfig.REMAINS_DECAY_TIME.get(), 0.0,
                                                        100000.0);
                        remains_health = builder.defineInRange("remains_health",
                                        CreRacesConfig.REMAINS_HEALTH.get(), 1.0, 1000.0);
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
                CreRacesConfig.MINIBUILD_REQUIRES_LEARNED = () -> COMMON.mini_build_requires_learned.get();
                CreRacesConfig.MINI_CRAFTING_DISTANCE_SQR = () -> COMMON.mini_crafting_distance_sqr.get();
                CreRacesConfig.MINI_PLACEMENT_SPAM_THRESHOLD_MS = () -> COMMON.mini_placement_spam_threshold_ms.get();
                CreRacesConfig.MINI_BLOCK_REACH_MARGIN = () -> COMMON.mini_block_reach_margin.get();

                CreRacesConfig.COIN_DROP_ENABLED = () -> COMMON.coin_drop_enabled.get();
                CreRacesConfig.SAG_WINGS = () -> COMMON.sag_wings.get();
                CreRacesConfig.GSTATE_ENABLED = () -> COMMON.gstate_enabled.get();
                CreRacesConfig.RITUAL_MODE = () -> COMMON.ritual_mode.get();

                CreRacesConfig.POCKET_DIM_SPACING = () -> COMMON.pocket_dim_spacing.get();
                CreRacesConfig.POCKET_DIM_Y = () -> COMMON.pocket_dim_y.get();
                CreRacesConfig.POCKET_EXPANSION_LIMIT = () -> COMMON.pocket_expansion_limit.get();
                CreRacesConfig.POCKET_EXPANSION_COST = () -> COMMON.pocket_expansion_cost.get();

                CreRacesConfig.ACTION_DEFAULT_POCKET_DIM = () -> COMMON.action_default_pocket_dim.get();
                CreRacesConfig.ACTION_DEFAULT_POCKET_STRUCTURE = () -> COMMON.action_default_pocket_structure.get();
                CreRacesConfig.ACTION_DEFAULT_POCKET_SPAWN_X_OFFSET = () -> COMMON.action_default_pocket_spawn_x_offset
                                .get();
                CreRacesConfig.ACTION_DEFAULT_POCKET_SPAWN_Y_OFFSET = () -> COMMON.action_default_pocket_spawn_y_offset
                                .get();
                CreRacesConfig.ACTION_DEFAULT_POCKET_SPAWN_Z_OFFSET = () -> COMMON.action_default_pocket_spawn_z_offset
                                .get();

                CreRacesConfig.SPIRIT_SPAWN_CHECK_RADIUS = () -> COMMON.spirit_spawn_check_radius.get();
                CreRacesConfig.CUSTOMIZATION_VALUE_MAX_LENGTH = () -> COMMON.customization_value_max_length.get();
                CreRacesConfig.RESOURCE_MIN_CAPACITY = () -> COMMON.resource_min_capacity.get();
                CreRacesConfig.PASSIVE_EXECUTION_INTERVAL = () -> COMMON.passive_execution_interval.get();
                CreRacesConfig.SUNLIGHT_EQUIPMENT_BREAK_CHANCE = () -> COMMON.sunlight_equipment_break_chance.get();
                CreRacesConfig.SUNLIGHT_BURN_SECONDS = () -> COMMON.sunlight_burn_seconds.get();
                CreRacesConfig.PASSIVE_EFFECT_BUFFER_TICKS = () -> COMMON.passive_effect_buffer_ticks.get();
                CreRacesConfig.PASSIVE_DEFAULT_MAX_FOOD = () -> COMMON.passive_default_max_food.get();
                CreRacesConfig.PASSIVE_DEFAULT_MAX_SATURATION = () -> COMMON.passive_default_max_saturation.get();
                CreRacesConfig.RACE_ADDONS_ENABLED = () -> COMMON.race_addons_enabled.get();
                CreRacesConfig.LORE_ADDONS_ENABLED = () -> COMMON.lore_addons_enabled.get();

                CreRacesConfig.RAT_VENOM_SCALING = () -> COMMON.rat_venom_scaling.get();

                CreRacesConfig.MINI_MODEL_CACHE_SIZE = () -> CLIENT.mini_model_cache_size.get();
                CreRacesConfig.VISUAL_SYNC_DISTANCE = () -> CLIENT.visual_sync_distance.get();
                CreRacesConfig.RACE_OVERLAY_OFFSET_X = () -> CLIENT.race_overlay_offset_x.get();
                CreRacesConfig.RACE_OVERLAY_OFFSET_Y = () -> CLIENT.race_overlay_offset_y.get();
                CreRacesConfig.RACE_OVERLAYS_ENABLED = () -> CLIENT.race_overlays_enabled.get();
                CreRacesConfig.ENGINE_POPUPS_ENABLED = () -> CLIENT.engine_popups_enabled.get();
                CreRacesConfig.MINI_DUMMY_CACHE_SIZE = () -> CLIENT.mini_dummy_cache_size.get();
                CreRacesConfig.SPIRIT_REALM_TINT_COLOR = () -> CLIENT.spirit_realm_tint_color.get();
                CreRacesConfig.SPIRIT_REALM_MOON_ALPHA = () -> (float) (double) CLIENT.spirit_realm_moon_alpha.get();
                CreRacesConfig.SPIRIT_REALM_MOON_SIZE = () -> (float) (double) CLIENT.spirit_realm_moon_size.get();

                CreRacesConfig.REMAINS_HEALTH = () -> ENTITIES.remains_health.get();

                CreRacesConfig.ENTITY_TROLL_PILLAR_MAX_HEALTH = () -> ENTITIES.troll_pillar_health.get();
                CreRacesConfig.ENTITY_TROLL_PILLAR_ARMOR = () -> ENTITIES.troll_pillar_armor.get();
                CreRacesConfig.ENTITY_TROLL_PILLAR_FOLLOW_RANGE = () -> ENTITIES.troll_pillar_follow_range.get();
                CreRacesConfig.ENTITY_TROLL_PILLAR_KNOCKBACK_RES = () -> ENTITIES.troll_pillar_knockback_res.get();

                CreRacesConfig.ENTITY_POISON_EMITTER_HEALTH = () -> ENTITIES.poison_emitter_health.get();
                CreRacesConfig.ENTITY_POISON_EMITTER_ARMOR = () -> ENTITIES.poison_emitter_armor.get();
                CreRacesConfig.ENTITY_POISON_EMITTER_FOLLOW_RANGE = () -> ENTITIES.poison_emitter_follow_range.get();
                CreRacesConfig.ENTITY_POISON_EMITTER_KNOCKBACK_RES = () -> ENTITIES.poison_emitter_knockback_res.get();

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
                CreRacesConfig.ENTITY_POISON_EMITTER_LIFETIME_TICKS = () -> ENTITIES.poison_emitter_lifetime_ticks
                                .get();
                CreRacesConfig.ENTITY_POISON_EMITTER_RADIUS = () -> ENTITIES.poison_emitter_radius.get();
                CreRacesConfig.ENTITY_TORNADO_LIFETIME_TICKS = () -> ENTITIES.tornado_lifetime_ticks.get();

                CreRacesConfig.ENTITY_FEATHER_GRAVITY = () -> ENTITIES.feather_gravity.get();

                CreRacesConfig.REMAINS_DECAY_TIME = () -> ENTITIES.remains_decay_time.get().intValue();
        }
}
