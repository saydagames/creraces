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
                public final ForgeConfigSpec.ConfigValue<String> developer_resource_path;

                public final ForgeConfigSpec.ConfigValue<Boolean> forced_selection;
                public final ForgeConfigSpec.ConfigValue<Double> ability_haste_cap;
                public final ForgeConfigSpec.ConfigValue<Double> racial_ad_multiplier;
                public final ForgeConfigSpec.ConfigValue<Long> resource_decay_grace_period;

                public final ForgeConfigSpec.ConfigValue<Boolean> coin_drop_enabled;
                public final ForgeConfigSpec.ConfigValue<Boolean> sag_wings;
                public final ForgeConfigSpec.ConfigValue<Boolean> gstate_enabled;
                public final ForgeConfigSpec.ConfigValue<Integer> ritual_mode;

                // Safety caps (-1 = disabled)
                public final ForgeConfigSpec.ConfigValue<Integer> aoe_max_radius;
                public final ForgeConfigSpec.ConfigValue<Integer> beam_max_length;
                public final ForgeConfigSpec.ConfigValue<Integer> break_blocks_max_radius;
                public final ForgeConfigSpec.ConfigValue<Double> remove_block_hardness_limit;
                public final ForgeConfigSpec.ConfigValue<Integer> customization_value_max_length;
                public final ForgeConfigSpec.ConfigValue<Integer> delay_action_max_ticks;
                public final ForgeConfigSpec.ConfigValue<Integer> entity_data_key_max_length;
                public final ForgeConfigSpec.ConfigValue<Integer> give_item_max_count;
                public final ForgeConfigSpec.ConfigValue<Integer> mass_summon_max_count;
                public final ForgeConfigSpec.ConfigValue<Integer> network_team_name_max_len;
                public final ForgeConfigSpec.ConfigValue<Integer> team_max_size;
                public final ForgeConfigSpec.ConfigValue<Integer> double_jump_cooldown_ticks;
                public final ForgeConfigSpec.ConfigValue<Integer> spirit_spawn_check_radius;
                public final ForgeConfigSpec.ConfigValue<Integer> max_rat_tunnels;

                public final ForgeConfigSpec.ConfigValue<Boolean> mini_build_enabled;
                public final ForgeConfigSpec.ConfigValue<Boolean> mini_furnace_enabled;
                public final ForgeConfigSpec.ConfigValue<Boolean> mini_campfire_enabled;
                public final ForgeConfigSpec.ConfigValue<Boolean> mini_brewing_stand_enabled;
                public final ForgeConfigSpec.ConfigValue<Boolean> mini_place_whitelist_enabled;
                public final ForgeConfigSpec.ConfigValue<Double> social_defense_range;
                public final ForgeConfigSpec.ConfigValue<Integer> micro_block_light_per_torch;
                public final ForgeConfigSpec.ConfigValue<Integer> micro_block_max_light;
                public final ForgeConfigSpec.ConfigValue<Boolean> mini_build_requires_learned;
                public final ForgeConfigSpec.ConfigValue<Double> mini_crafting_distance_sqr;
                public final ForgeConfigSpec.ConfigValue<Long> mini_placement_spam_threshold_ms;
                public final ForgeConfigSpec.ConfigValue<Double> mini_block_reach_margin;
                public final ForgeConfigSpec.ConfigValue<Boolean> mini_block_water_resistant;
                public final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> mini_build_dimension_blacklist;

                public final ForgeConfigSpec.ConfigValue<Integer> territory_default_claim_radius;
                public final ForgeConfigSpec.ConfigValue<Integer> territory_max_nodes_per_player;
                public final ForgeConfigSpec.ConfigValue<Boolean> territory_inter_race_blocking;
                public final ForgeConfigSpec.ConfigValue<Long>    territory_leader_decay_threshold_days;
                public final ForgeConfigSpec.ConfigValue<Long>    territory_succession_window_days;
                public final ForgeConfigSpec.ConfigValue<Integer> territory_succession_tick_interval;

                public final ForgeConfigSpec.ConfigValue<Boolean> team_require_same_race;

                public final ForgeConfigSpec.ConfigValue<Integer> fairy_realm_border_size;

                public final ForgeConfigSpec.ConfigValue<String> action_default_pocket_dim;
                public final ForgeConfigSpec.ConfigValue<String> action_default_pocket_structure;
                public final ForgeConfigSpec.ConfigValue<Double> action_default_pocket_spawn_x_offset;
                public final ForgeConfigSpec.ConfigValue<Double> action_default_pocket_spawn_y_offset;
                public final ForgeConfigSpec.ConfigValue<Double> action_default_pocket_spawn_z_offset;
                public final ForgeConfigSpec.ConfigValue<Double> pocket_dim_spacing;
                public final ForgeConfigSpec.ConfigValue<Double> pocket_dim_y;
                public final ForgeConfigSpec.ConfigValue<Integer> pocket_expansion_limit;
                public final ForgeConfigSpec.ConfigValue<Double> pocket_expansion_cost;
                public final ForgeConfigSpec.ConfigValue<Integer> pocket_invite_max;
                public final ForgeConfigSpec.ConfigValue<Double> pocket_boundary;

                // Technical/Internal
                public final ForgeConfigSpec.ConfigValue<Boolean> race_addons_enabled;
                public final ForgeConfigSpec.ConfigValue<Boolean> lore_addons_enabled;

                public final ForgeConfigSpec.ConfigValue<Integer> passive_execution_interval;
                public final ForgeConfigSpec.ConfigValue<Double> sunlight_equipment_break_chance;
                public final ForgeConfigSpec.ConfigValue<Integer> sunlight_burn_seconds;
                public final ForgeConfigSpec.ConfigValue<Integer> passive_effect_buffer_ticks;
                public final ForgeConfigSpec.ConfigValue<Integer> passive_default_max_food;
                public final ForgeConfigSpec.ConfigValue<Double> passive_default_max_saturation;
                public final ForgeConfigSpec.ConfigValue<Double> max_soul;

                public final ForgeConfigSpec.ConfigValue<Double> rat_venom_scaling;
                public final ForgeConfigSpec.ConfigValue<Double> boiling_scaling;
                public final ForgeConfigSpec.ConfigValue<Double> bleeding_scaling;

                public Common(ForgeConfigSpec.Builder builder) {
                        builder.push("Documentation");
                        wiki_base_url = builder.comment("The base URL for the wiki. Default: https://creraces.wiki.gg")
                                        .define("wiki_base_url",
                                                        CreRacesConfig.WIKI_BASE_URL.get());
                        wiki_api_base = builder
                                        .comment("The API base URL for the wiki. Default: https://creraces.wiki.gg")
                                        .define("wiki_api_base",
                                                        CreRacesConfig.WIKI_API_BASE.get());
                        wiki_page_path = builder.comment("The path part of the wiki URL. Default: wiki").define(
                                        "wiki_page_path",
                                        CreRacesConfig.WIKI_PAGE_PATH.get());
                        wiki_ability_namespace = builder.comment("The prefix for ability pages. Default: Ability")
                                        .define("wiki_ability_namespace", CreRacesConfig.WIKI_ABILITY_NAMESPACE.get());
                        disable_remote_docs = builder
                                        .comment("Set to true to disable remote documentation fetching. Default: false")
                                        .define("disable_remote_docs",
                                                        CreRacesConfig.DISABLE_REMOTE_DOCS.get());
                        doc_fetch_timeout_seconds = builder
                                        .comment("Timeout in seconds for fetching remote documentation. Default: 10")
                                        .defineInRange("doc_fetch_timeout_seconds",
                                                        CreRacesConfig.DOC_FETCH_TIMEOUT_SECONDS.get(), 1, 60);
                        doc_cache_dir = builder
                                        .comment("Directory name for the documentation cache. Default: creraces/cache")
                                        .define("doc_cache_dir", CreRacesConfig.DOC_CACHE_DIR.get());
                        doc_cache_filename = builder
                                        .comment("Filename for the documentation cache. Default: wiki_docs.json")
                                        .define("doc_cache_filename", CreRacesConfig.DOC_CACHE_FILENAME.get());
                        developer_resource_path = builder.comment(
                                        "Absolute path to look for race/ability JSONs in dev. (Example: H:/Github/creraces/1.20.1/common/src/main/resources/data). Default is empty string.")
                                        .define("developer_resource_path",
                                                        CreRacesConfig.DEVELOPER_RESOURCE_PATH.get());
                        builder.pop();

                        builder.push("Gameplay");
                        forced_selection = builder.comment(
                                        "Keep the selection GUI open until a race is chosen. Also grants invulnerability while choosing. Default: true")
                                        .define("forced_selection", CreRacesConfig.FORCED_SELECTION.get());
                        ability_haste_cap = builder
                                        .comment("The soft cap for Ability Haste. Max value is 100. Default: 40.0")
                                        .defineInRange("ability_haste_cap", CreRacesConfig.ABILITY_HASTE_CAP.get(), 0.0,
                                                        100.0);
                        racial_ad_multiplier = builder.comment(
                                        "Multiplier for Racial Attack Damage (AD). Default is 0.002 (0.2% per point).")
                                        .defineInRange("racial_ad_multiplier",
                                                        CreRacesConfig.RACIAL_AD_MULTIPLIER.get(), 0.0, 100.0);
                        resource_decay_grace_period = builder
                                        .comment("Grace period (ticks) before Rage/Grit begins to decay. Default: 400")
                                        .defineInRange("resource_decay_grace_period",
                                                        CreRacesConfig.RESOURCE_DECAY_GRACE_PERIOD.get(), 0L, 100000L);
                        coin_drop_enabled = builder.comment("Enable loot-based coin drops. Default: true")
                                        .define("coin_drop_enabled", CreRacesConfig.COIN_DROP_ENABLED.get());
                        sag_wings = builder.comment("Allow wings flight to sag during rain. Default: true")
                                        .define("sag_wings",
                                                        CreRacesConfig.SAG_WINGS.get());
                        gstate_enabled = builder
                                        .comment("Enable the gender system (affects some race visuals). Default: true")
                                        .define("gstate_enabled",
                                                        CreRacesConfig.GSTATE_ENABLED.get());
                        ritual_mode = builder
                                        .comment("Ritual Mode: 0=Disabled, 1=Human Only, 2=Reset Allowed. Default: 2")
                                        .defineInRange("ritual_mode", CreRacesConfig.RITUAL_MODE.get(), 0, 2);
                        builder.pop();

                        builder.push("Potion Effects");
                        rat_venom_scaling = builder
                                        .comment("Damage scaling factor for Rat Venom per stack. Default 0.05.")
                                        .defineInRange("rat_venom_scaling", CreRacesConfig.RAT_VENOM_SCALING.get(), 0.0,
                                                        10.0);
                        boiling_scaling = builder
                                        .comment("Damage scaling factor for Boiling per point of AP. Default reaches Boiling 5 at 300 AP.")
                                        .defineInRange("boiling_scaling", CreRacesConfig.BOILING_SCALING.get(), 0.0,
                                                        10.0);
                        bleeding_scaling = builder
                                        .comment("Damage scaling factor for Bleeding per point of AP. Default reaches Bleeding 3 at 300 AP.")
                                        .defineInRange("bleeding_scaling", CreRacesConfig.BLEEDING_SCALING.get(), 0.0,
                                                        10.0);
                        builder.pop();

                        builder.push("Safety");
                        aoe_max_radius = builder.comment(
                                        "Max radius for creraces:aoe / creraces:apply_effect AoE. -1 = no cap. Default: -1")
                                        .defineInRange("aoe_max_radius", CreRacesConfig.AOE_MAX_RADIUS.get(), -1, 512);
                        beam_max_length = builder.comment("Max length for creraces:beam. -1 = no cap. Default: -1")
                                        .defineInRange("beam_max_length", CreRacesConfig.BEAM_MAX_LENGTH.get(), -1,
                                                        512);
                        break_blocks_max_radius = builder
                                        .comment("Max radius for creraces:break_blocks. -1 = no cap. Default: -1")
                                        .defineInRange("break_blocks_max_radius",
                                                        CreRacesConfig.BREAK_BLOCKS_MAX_RADIUS.get(), -1, 256);
                        remove_block_hardness_limit = builder.comment(
                                        "Max hardness for creraces:remove_block. -1.0 = no cap (allows breaking bedrock). Default: -1.0")
                                        .defineInRange("remove_block_hardness_limit",
                                                        CreRacesConfig.REMOVE_BLOCK_HARDNESS_LIMIT.get(), -1.0, 1000.0);
                        customization_value_max_length = builder
                                        .comment("Max length for customization values. -1 = no cap. Default: 128")
                                        .defineInRange("customization_value_max_length",
                                                        CreRacesConfig.CUSTOMIZATION_VALUE_MAX_LENGTH.get(), -1, 2048);
                        delay_action_max_ticks = builder
                                        .comment("Max delay ticks for creraces:delay actions. -1 = no cap. Default: -1")
                                        .defineInRange("delay_action_max_ticks",
                                                        CreRacesConfig.DELAY_ACTION_MAX_TICKS.get(), -1, 72000);
                        entity_data_key_max_length = builder.comment(
                                        "Max character length for creraces:modify_entity_data keys. -1 = no cap. Default: 64")
                                        .defineInRange("entity_data_key_max_length",
                                                        CreRacesConfig.ENTITY_DATA_KEY_MAX_LENGTH.get(), -1, 1024);
                        give_item_max_count = builder
                                        .comment("Max item count for creraces:give_item. -1 = no cap. Default: -1")
                                        .defineInRange("give_item_max_count", CreRacesConfig.GIVE_ITEM_MAX_COUNT.get(),
                                                        -1, 6400);
                        mass_summon_max_count = builder
                                        .comment("Max entities for creraces:mass_summon per cast. -1 = no cap. Default: -1")
                                        .defineInRange("mass_summon_max_count",
                                                        CreRacesConfig.MASS_SUMMON_MAX_COUNT.get(),
                                                        -1, 512);
                        network_team_name_max_len = builder.comment("Max length for team names. Default: 16")
                                        .defineInRange("network_team_name_max_len",
                                                        CreRacesConfig.NETWORK_TEAM_NAME_MAX_LEN.get(), 1, 256);
                        team_max_size = builder.comment("Max number of players allowed in a team. Default: 10")
                                        .defineInRange("team_max_size",
                                                        CreRacesConfig.TEAM_MAX_SIZE.get(), 1, 1000);
                        double_jump_cooldown_ticks = builder.comment(
                                        "Server-side cooldown (ticks) between DoubleJump packets. Prevents spam. Default: 10")
                                        .defineInRange("double_jump_cooldown_ticks",
                                                        CreRacesConfig.DOUBLE_JUMP_COOLDOWN_TICKS.get(), 1, 200);
                        spirit_spawn_check_radius = builder
                                        .comment("Radius to check for existing spirits when spawning a new one. Default: 16")
                                        .defineInRange("spirit_spawn_check_radius",
                                                        CreRacesConfig.SPIRIT_SPAWN_CHECK_RADIUS.get(), 1, 512);
                        max_rat_tunnels = builder
                                        .comment("Max number of active tunnel pairs a Ratkin can have. Default: 1")
                                        .defineInRange("max_rat_tunnels", CreRacesConfig.MAX_RAT_TUNNELS.get(), 1, 10);
                        builder.pop();

                        builder.push("MiniBuild");
                        mini_build_enabled = builder
                                        .comment("Enable the Mini Build System. Default: true")
                                        .define("mini_build_enabled",
                                                        CreRacesConfig.MINI_BUILD_ENABLED.get());
                        mini_furnace_enabled = builder.comment("Enable the Mini Furnace system. Default: true")
                                        .define("mini_furnace_enabled",
                                                        CreRacesConfig.MINI_FURNACE_ENABLED.get());
                        mini_campfire_enabled = builder.comment("Enable the Mini Campfire system. Default: true")
                                        .define("mini_campfire_enabled",
                                                        CreRacesConfig.MINI_CAMPFIRE_ENABLED.get());
                        mini_brewing_stand_enabled = builder.comment("Enable the Mini Brewing Stand system. Default: true")
                                        .define("mini_brewing_stand_enabled",
                                                        CreRacesConfig.MINI_BREWING_STAND_ENABLED.get());
                        mini_place_whitelist_enabled = builder
                                        .comment("Enable the whitelist for Mini Place system. Default: true")
                                        .define("mini_place_whitelist_enabled",
                                                        CreRacesConfig.MINI_PLACE_WHITELIST_ENABLED.get());
                        micro_block_light_per_torch = builder
                                        .comment("Light level added per torch in a micro-block. Default: 5")
                                        .defineInRange("micro_block_light_per_torch",
                                                        CreRacesConfig.MICRO_BLOCK_LIGHT_PER_TORCH.get(), 0, 15);
                        micro_block_max_light = builder.comment("Maximum light level for a micro-block. Default: 15")
                                        .defineInRange("micro_block_max_light",
                                                        CreRacesConfig.MICRO_BLOCK_MAX_LIGHT.get(), 0, 15);
                        mini_build_requires_learned = builder
                                        .comment("Players must learn/unlock minibuilding. Default: true")
                                        .define("mini_build_requires_learned",
                                                        CreRacesConfig.MINIBUILD_REQUIRES_LEARNED.get());
                        mini_crafting_distance_sqr = builder
                                        .comment("Distance squared at which minibuilding works. Default: 64.0")
                                        .defineInRange("mini_crafting_distance_sqr",
                                                        CreRacesConfig.MINI_CRAFTING_DISTANCE_SQR.get(), 1.0,
                                                        1024.0);
                        mini_placement_spam_threshold_ms = builder
                                        .comment("Minimum MS between mini block placements. Default: 50")
                                        .defineInRange("mini_placement_spam_threshold_ms",
                                                        CreRacesConfig.MINI_PLACEMENT_SPAM_THRESHOLD_MS.get(),
                                                        0L, 1000L);
                        mini_block_reach_margin = builder
                                        .comment("Extra reach margin for working with mini blocks. Default: 1.5")
                                        .defineInRange("mini_block_reach_margin",
                                                        CreRacesConfig.MINI_BLOCK_REACH_MARGIN.get(), 0.0,
                                                        5.0);
                        mini_block_water_resistant = builder
                                        .comment("Blocks cannot be destroyed by flowing water. Default: true")
                                        .define("mini_block_water_resistant",
                                                        CreRacesConfig.MINI_BLOCK_WATER_RESISTANT.get());
                        mini_build_dimension_blacklist = builder.comment(
                                        "Dimensions where mini block placement is blocked. Default: [creraces:fairy_realm]")
                                        .defineList("mini_build_dimension_blacklist",
                                                        CreRacesConfig.MINI_BUILD_DIMENSION_BLACKLIST.get(),
                                                        s -> s instanceof String);
                        builder.pop();

                        builder.push("Territory");
                        territory_default_claim_radius = builder
                                        .comment("Chunk radius for the initial faction claim grid (1 = 3x3). Default: 1")
                                        .defineInRange("territory_default_claim_radius",
                                                        CreRacesConfig.TERRITORY_DEFAULT_CLAIM_RADIUS.get(), 0, 8);
                        territory_max_nodes_per_player = builder
                                        .comment("Max territory nodes a player can place. -1 = unlimited. Default: -1")
                                        .defineInRange("territory_max_nodes_per_player",
                                                        CreRacesConfig.TERRITORY_MAX_NODES_PER_PLAYER.get(), -1, 1024);
                        territory_inter_race_blocking = builder
                                        .comment("Cross-race claims block each other. Default: true")
                                        .define("territory_inter_race_blocking",
                                                        CreRacesConfig.TERRITORY_INTER_RACE_BLOCKING.get());
                        territory_leader_decay_threshold_days = builder
                                        .comment("Days offline before succession triggers. -1 = disabled. Default: 14")
                                        .defineInRange("territory_leader_decay_threshold_days",
                                                        CreRacesConfig.TERRITORY_LEADER_DECAY_THRESHOLD_DAYS.get(), -1L, 3650L);
                        territory_succession_window_days = builder
                                        .comment("Days of activity required to be eligible for succession. Default: 7")
                                        .defineInRange("territory_succession_window_days",
                                                        CreRacesConfig.TERRITORY_SUCCESSION_WINDOW_DAYS.get(), 1L, 365L);
                        territory_succession_tick_interval = builder
                                        .comment("Ticks between succession checks (~5 min = 6000). Default: 6000")
                                        .defineInRange("territory_succession_tick_interval",
                                                        CreRacesConfig.TERRITORY_SUCCESSION_TICK_INTERVAL.get(), 20, 72000);
                        builder.pop();

                        builder.push("Team");
                        team_require_same_race = builder
                                        .comment("If true, team invites are restricted to players of the same race. Default: false")
                                        .define("team_require_same_race",
                                                        CreRacesConfig.TEAM_REQUIRE_SAME_RACE.get());
                        builder.pop();

                        builder.push("FairyRealm");
                        fairy_realm_border_size = builder
                                        .comment("World border diameter for the fairy_realm dimension. Default: 1000")
                                        .defineInRange("fairy_realm_border_size",
                                                        CreRacesConfig.FAIRY_REALM_BORDER_SIZE.get(), 100, 60000000);
                        builder.pop();

                        builder.push("Pockets");
                        action_default_pocket_dim = builder
                                        .comment("Default dimension ID for player pockets. Default: creraces:pocket")
                                        .define("action_default_pocket_dim",
                                                        CreRacesConfig.ACTION_DEFAULT_POCKET_DIM.get());
                        action_default_pocket_structure = builder
                                        .comment("Default structure ID for new pockets. Default: creraces:dryad_box_1")
                                        .define("action_default_pocket_structure",
                                                        CreRacesConfig.ACTION_DEFAULT_POCKET_STRUCTURE.get());
                        action_default_pocket_spawn_x_offset = builder
                                        .comment("X offset for default pocket spawn point. Default: 6.5")
                                        .defineInRange("action_default_pocket_spawn_x_offset",
                                                        CreRacesConfig.ACTION_DEFAULT_POCKET_SPAWN_X_OFFSET
                                                                        .get(),
                                                        -100.0, 100.0);
                        action_default_pocket_spawn_y_offset = builder
                                        .comment("Y offset for default pocket spawn point. Default: 2.0")
                                        .defineInRange("action_default_pocket_spawn_y_offset",
                                                        CreRacesConfig.ACTION_DEFAULT_POCKET_SPAWN_Y_OFFSET
                                                                        .get(),
                                                        -100.0, 100.0);
                        action_default_pocket_spawn_z_offset = builder
                                        .comment("Z offset for default pocket spawn point. Default: 6.5")
                                        .defineInRange("action_default_pocket_spawn_z_offset",
                                                        CreRacesConfig.ACTION_DEFAULT_POCKET_SPAWN_Z_OFFSET
                                                                        .get(),
                                                        -100.0, 100.0);
                        pocket_dim_spacing = builder.comment(
                                        "Distance between player pockets. WARNING: Do not decrease on existing worlds. Default: 1000.0")
                                        .defineInRange("pocket_dim_spacing",
                                                        CreRacesConfig.POCKET_DIM_SPACING.get(), 100.0,
                                                        100000.0);
                        pocket_dim_y = builder.comment("Y coordinate for pocket floors. Default: 100.0")
                                        .defineInRange("pocket_dim_y", CreRacesConfig.POCKET_DIM_Y.get(), 0.0,
                                                        255.0);
                        pocket_expansion_cost = builder.comment("Coin cost per expansion. Default: 200.0")
                                        .defineInRange("pocket_expansion_cost",
                                                        CreRacesConfig.POCKET_EXPANSION_COST.get(), 0.0,
                                                        1000000.0);
                        pocket_expansion_limit = builder
                                        .comment("Max number of expansions allowed per player. Default: 8")
                                        .defineInRange("pocket_expansion_limit",
                                                        CreRacesConfig.POCKET_EXPANSION_LIMIT.get(), 0, 100);
                        pocket_invite_max = builder
                                        .comment("Max number of pending invitations a player can have. -1 = no cap. Default: -1")
                                        .defineInRange("pocket_invite_max", CreRacesConfig.POCKET_INVITE_MAX.get(), -1,
                                                        1024);
                        pocket_boundary = builder.comment(
                                        "The boundary distance from the pocket origin where expansion is allowed. Default: 450.0")
                                        .defineInRange("pocket_boundary", CreRacesConfig.POCKET_BOUNDARY.get(), 10.0,
                                                        1000.0);
                        builder.pop();

                        builder.push("Internal");
                        passive_execution_interval = builder
                                        .comment("Interval in ticks between passive ability executions. Default: 20")
                                        .defineInRange("passive_execution_interval",
                                                        CreRacesConfig.PASSIVE_EXECUTION_INTERVAL.get(), 1, 1200);
                        sunlight_equipment_break_chance = builder.comment(
                                        "Chance (0.0-1.0) for equipment to take damage in sunlight. Default: 0.01")
                                        .defineInRange("sunlight_equipment_break_chance",
                                                        CreRacesConfig.SUNLIGHT_EQUIPMENT_BREAK_CHANCE.get(),
                                                        0.0, 1.0);
                        sunlight_burn_seconds = builder
                                        .comment("Duration of burn effect from sunlight in seconds. Default: 5")
                                        .defineInRange("sunlight_burn_seconds",
                                                        CreRacesConfig.SUNLIGHT_BURN_SECONDS.get(), 0, 3600);
                        passive_effect_buffer_ticks = builder.comment("Buffer ticks for passive effects. Default: 20")
                                        .defineInRange("passive_effect_buffer_ticks",
                                                        CreRacesConfig.PASSIVE_EFFECT_BUFFER_TICKS.get(), 0, 1200);
                        passive_default_max_food = builder.comment("Default max food level override. Default: 20")
                                        .defineInRange("passive_default_max_food",
                                                        CreRacesConfig.PASSIVE_DEFAULT_MAX_FOOD.get(), 1, 100);
                        passive_default_max_saturation = builder
                                        .comment("Default max saturation level override. Default: 20.0")
                                        .defineInRange("passive_default_max_saturation",
                                                        CreRacesConfig.PASSIVE_DEFAULT_MAX_SATURATION.get(),
                                                        0.0, 100.0);
                        race_addons_enabled = builder.comment("Enable racial visual body part addons. Default: true")
                                        .define("race_addons_enabled",
                                                        CreRacesConfig.RACE_ADDONS_ENABLED.get());
                        lore_addons_enabled = builder.comment(
                                        "Enable lore-specific body addons (requires race_addons_enabled). Default: true")
                                        .define("lore_addons_enabled",
                                                        CreRacesConfig.LORE_ADDONS_ENABLED.get());
                        max_soul = builder.comment("Maximum soul value for soul-based races. Default: 9.0")
                                        .defineInRange("max_soul",
                                                        CreRacesConfig.MAX_SOUL.get(), 0.0, 100.0);
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
                public final ForgeConfigSpec.ConfigValue<Integer> hud_anchor_x;
                public final ForgeConfigSpec.ConfigValue<Integer> hud_anchor_y;
                public final ForgeConfigSpec.ConfigValue<Integer> hud_portrait_x;
                public final ForgeConfigSpec.ConfigValue<Integer> hud_portrait_y;
                public final ForgeConfigSpec.ConfigValue<Integer> hud_abilities_x;
                public final ForgeConfigSpec.ConfigValue<Integer> hud_abilities_y;
                public final ForgeConfigSpec.ConfigValue<Integer> hud_bars_x;
                public final ForgeConfigSpec.ConfigValue<Integer> hud_bars_y;
                public final ForgeConfigSpec.ConfigValue<String> bar_label_mode;
                public final ForgeConfigSpec.ConfigValue<Boolean> bar_show_seconds;
                public final ForgeConfigSpec.ConfigValue<Boolean> hud_bars_grow_up;
                public final ForgeConfigSpec.ConfigValue<Boolean> hud_abilities_vertical;
                public final ForgeConfigSpec.ConfigValue<String> hud_slot_label_side;
                public final ForgeConfigSpec.DoubleValue hud_scale;

                public Client(ForgeConfigSpec.Builder builder) {
                        builder.push("Rendering");
                        mini_model_cache_size = builder
                                        .comment("Max models stored in memory for Mini-Blocks. Default: 128")
                                        .defineInRange("mini_model_cache_size",
                                                        CreRacesConfig.MINI_MODEL_CACHE_SIZE.get(), 1, 2048);
                        visual_sync_distance = builder
                                        .comment("Distance (blocks) for visual sync (beams, tethers, animations). Default: 64")
                                        .defineInRange("visual_sync_distance",
                                                        CreRacesConfig.VISUAL_SYNC_DISTANCE.get(), 1, 512);
                        builder.pop();

                        builder.push("HUD");
                        race_overlay_offset_x = builder.comment("X offset for the race overlay HUD. Default: 0")
                                        .defineInRange("race_overlay_offset_x",
                                                        CreRacesConfig.RACE_OVERLAY_OFFSET_X.get(), -2048, 2048);
                        race_overlay_offset_y = builder.comment("Y offset for the race overlay HUD. Default: 0")
                                        .defineInRange("race_overlay_offset_y",
                                                        CreRacesConfig.RACE_OVERLAY_OFFSET_Y.get(), -2048, 2048);
                        race_overlays_enabled = builder.comment("Enable racial UI overlays. Default: true")
                                        .define("race_overlays_enabled",
                                                        CreRacesConfig.RACE_OVERLAYS_ENABLED.get());
                        engine_popups_enabled = builder.comment("Enable engine-driven popup messages. Default: true")
                                        .define("engine_popups_enabled",
                                                        CreRacesConfig.ENGINE_POPUPS_ENABLED.get());
                        mini_dummy_cache_size = builder
                                        .comment("Size of the mini block dummy entity cache. Default: 32")
                                        .defineInRange("mini_dummy_cache_size",
                                                        CreRacesConfig.MINI_DUMMY_CACHE_SIZE.get(), 1, 256);
                        hud_anchor_x = builder.comment("Global HUD anchor X offset. Default: 0")
                                        .defineInRange("hud_anchor_x", CreRacesConfig.HUD_ANCHOR_X.get(), -2048, 2048);
                        hud_anchor_y = builder.comment("Global HUD anchor Y offset. Default: 0")
                                        .defineInRange("hud_anchor_y", CreRacesConfig.HUD_ANCHOR_Y.get(), -2048, 2048);
                        hud_portrait_x = builder.comment("Portrait group X position. Default: 14")
                                        .defineInRange("hud_portrait_x", CreRacesConfig.HUD_PORTRAIT_X.get(), -2048, 2048);
                        hud_portrait_y = builder.comment("Portrait group Y position. Default: 13")
                                        .defineInRange("hud_portrait_y", CreRacesConfig.HUD_PORTRAIT_Y.get(), -2048, 2048);
                        hud_abilities_x = builder.comment("Ability slots group X position. Default: 54")
                                        .defineInRange("hud_abilities_x", CreRacesConfig.HUD_ABILITIES_X.get(), -2048, 2048);
                        hud_abilities_y = builder.comment("Ability slots group Y position. Default: 17")
                                        .defineInRange("hud_abilities_y", CreRacesConfig.HUD_ABILITIES_Y.get(), -2048, 2048);
                        hud_bars_x = builder.comment("Overlay bars group X position. Default: 13")
                                        .defineInRange("hud_bars_x", CreRacesConfig.HUD_BARS_X.get(), -2048, 2048);
                        hud_bars_y = builder.comment("Overlay bars group Y position. Default: 63")
                                        .defineInRange("hud_bars_y", CreRacesConfig.HUD_BARS_Y.get(), -2048, 2048);
                        bar_label_mode = builder.comment("Bar label display mode: name_value, name, value, hidden. Default: name_value")
                                        .define("bar_label_mode", CreRacesConfig.BAR_LABEL_MODE.get());
                        bar_show_seconds = builder.comment("Show cooldown time as seconds (true) or raw ticks (false). Default: true")
                                        .define("bar_show_seconds", CreRacesConfig.BAR_SHOW_SECONDS.get());
                        hud_bars_grow_up = builder.comment("Stack overlay bars upward from anchor instead of downward. Default: false")
                                        .define("hud_bars_grow_up", CreRacesConfig.HUD_BARS_GROW_UP.get());
                        hud_abilities_vertical = builder.comment("Stack ability slots vertically instead of horizontally. Default: false")
                                        .define("hud_abilities_vertical", CreRacesConfig.HUD_ABILITIES_VERTICAL.get());
                        hud_slot_label_side = builder.comment("Ability slot keybind label position: below, side, top, left, none. Default: below")
                                        .define("hud_slot_label_side", CreRacesConfig.HUD_SLOT_LABEL_SIDE.get());
                        hud_scale = builder.comment("HUD scale multiplier (1.0 = 100%). Default: 1.0")
                                        .defineInRange("hud_scale", 1.0, 0.1, 5.0);
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
                        troll_pillar_health = builder.comment("Max health for Troll Pillars. Default: 40.0")
                                        .defineInRange("troll_pillar_health",
                                                        CreRacesConfig.ENTITY_TROLL_PILLAR_MAX_HEALTH.get(),
                                                        1.0, 1000.0);
                        troll_pillar_armor = builder.comment("Armor for Troll Pillars. Default: 10.0")
                                        .defineInRange("troll_pillar_armor",
                                                        CreRacesConfig.ENTITY_TROLL_PILLAR_ARMOR.get(), 0.0,
                                                        100.0);
                        troll_pillar_follow_range = builder.comment("Follow range for Troll Pillars. Default: 16.0")
                                        .defineInRange("troll_pillar_follow_range",
                                                        CreRacesConfig.ENTITY_TROLL_PILLAR_FOLLOW_RANGE.get(),
                                                        1.0, 128.0);
                        troll_pillar_knockback_res = builder
                                        .comment("Knockback resistance for Troll Pillars. Default: 1.0")
                                        .defineInRange("troll_pillar_knockback_res",
                                                        CreRacesConfig.ENTITY_TROLL_PILLAR_KNOCKBACK_RES.get(),
                                                        0.0, 1.0);

                        poison_emitter_health = builder.comment("Max health for Poison Emitters. Default: 16.0")
                                        .defineInRange("poison_emitter_health",
                                                        CreRacesConfig.ENTITY_POISON_EMITTER_HEALTH.get(), 1.0,
                                                        1000.0);
                        poison_emitter_armor = builder.comment("Armor for Poison Emitters. Default: 16.0")
                                        .defineInRange("poison_emitter_armor",
                                                        CreRacesConfig.ENTITY_POISON_EMITTER_ARMOR.get(), 0.0,
                                                        100.0);
                        poison_emitter_follow_range = builder.comment("Follow range for Poison Emitters. Default: 16.0")
                                        .defineInRange("poison_emitter_follow_range",
                                                        CreRacesConfig.ENTITY_POISON_EMITTER_FOLLOW_RANGE
                                                                        .get(),
                                                        1.0, 128.0);
                        poison_emitter_knockback_res = builder
                                        .comment("Knockback resistance for Poison Emitters. Default: 1.0")
                                        .defineInRange("poison_emitter_knockback_res",
                                                        CreRacesConfig.ENTITY_POISON_EMITTER_KNOCKBACK_RES
                                                                        .get(),
                                                        0.0, 1.0);

                        poison_emitter_radius = builder.comment("Radius (blocks) for Poison Emitters. Default: 5.5")
                                        .defineInRange("poison_emitter_radius",
                                                        CreRacesConfig.ENTITY_POISON_EMITTER_RADIUS.get(), 1.0,
                                                        64.0);
                        feather_damage = builder.comment("Base damage for Harpy Feathers. Default: 2.0")
                                        .defineInRange("feather_damage",
                                                        CreRacesConfig.ENTITY_FEATHER_DAMAGE.get(), 0.0,
                                                        100.0);
                        feather_gravity = builder.comment("Gravity scale for Harpy Feathers. Default: 0.05")
                                        .defineInRange("feather_gravity",
                                                        CreRacesConfig.ENTITY_FEATHER_GRAVITY.get(), 0.0, 1.0);
                        tornado_health = builder.comment("Health for Tornado entities. Default: 20.0")
                                        .defineInRange("tornado_health",
                                                        CreRacesConfig.ENTITY_TORNADO_HEALTH.get(), 1.0,
                                                        10000.0);
                        tornado_armor = builder.comment("Armor for Tornado entities. Default: 5.0")
                                        .defineInRange("tornado_armor",
                                                        CreRacesConfig.ENTITY_TORNADO_ARMOR.get(), 0.0, 100.0);
                        tornado_attack_damage = builder.comment("Attack damage for Tornado entities. Default: 3.0")
                                        .defineInRange("tornado_attack_damage",
                                                        CreRacesConfig.ENTITY_TORNADO_ATTACK_DAMAGE.get(), 0.0,
                                                        100.0);
                        tornado_follow_range = builder.comment("Follow range for Tornado entities. Default: 16.0")
                                        .defineInRange("tornado_follow_range",
                                                        CreRacesConfig.ENTITY_TORNADO_FOLLOW_RANGE.get(), 1.0,
                                                        128.0);
                        builder.pop();

                        builder.push("Mechanics");
                        troll_pillar_pulse_interval = builder
                                        .comment("Interval (ticks) between Troll Pillar pulses. Default: 20")
                                        .defineInRange("troll_pillar_pulse_interval",
                                                        CreRacesConfig.ENTITY_TROLL_PILLAR_PULSE_INTERVAL.get(),
                                                        1, 1200);
                        troll_pillar_curse_radius = builder
                                        .comment("Radius (blocks) for Troll Pillar curse. Default: 5.0")
                                        .defineInRange("troll_pillar_curse_radius",
                                                        CreRacesConfig.ENTITY_TROLL_PILLAR_CURSE_RADIUS.get(),
                                                        1.0, 64.0);
                        troll_pillar_curse_duration = builder
                                        .comment("Duration (ticks) for Troll Pillar curse. Default: 100")
                                        .defineInRange("troll_pillar_curse_duration",
                                                        CreRacesConfig.ENTITY_TROLL_PILLAR_CURSE_DURATION.get(),
                                                        1, 1200);
                        troll_pillar_lifetime_ticks = builder
                                        .comment("Lifetime (ticks) for Troll Pillar entities. Default: 600")
                                        .defineInRange("troll_pillar_lifetime_ticks",
                                                        CreRacesConfig.ENTITY_TROLL_PILLAR_LIFETIME_TICKS.get(),
                                                        1, 10000);

                        tornado_flying_speed = builder.comment("Flying speed for Tornado entities. Default: 0.1")
                                        .defineInRange("tornado_flying_speed",
                                                        CreRacesConfig.ENTITY_TORNADO_FLYING_SPEED.get(), 0.0,
                                                        1.0);
                        tornado_movement_speed = builder.comment("Movement speed for Tornado entities. Default: 0.3")
                                        .defineInRange("tornado_movement_speed",
                                                        CreRacesConfig.ENTITY_TORNADO_MOVEMENT_SPEED.get(),
                                                        0.0, 1.0);
                        tornado_hurricane_radius = builder
                                        .comment("Radius (blocks) for Tornado hurricane effect. Default: 2.5")
                                        .defineInRange("tornado_hurricane_radius",
                                                        (double) CreRacesConfig.ENTITY_TORNADO_RADIUS.get(), 1.0, 64.0);
                        tornado_dizziness_duration = builder
                                        .comment("Duration (ticks) for Tornado dizziness effect. Default: 60")
                                        .defineInRange("tornado_dizziness_duration",
                                                        CreRacesConfig.ENTITY_TORNADO_DIZZINESS_DURATION.get(), 1,
                                                        1200);
                        tornado_pull_force = builder
                                        .comment("Force applied to pull entities towards Tornado. Default: 0.05")
                                        .defineInRange("tornado_pull_force",
                                                        CreRacesConfig.ENTITY_TORNADO_PULL_FORCE.get(), 0.0,
                                                        1.0);

                        poison_emitter_mobile_max_health = builder
                                        .comment("Max health for mobile Poison Emitters. Default: 16.0")
                                        .defineInRange("poison_emitter_mobile_max_health",
                                                        CreRacesConfig.ENTITY_POISON_EMITTER_MOBILE_MAX_HEALTH
                                                                        .get(),
                                                        1.0, 1000.0);
                        poison_emitter_mobile_movement_speed = builder
                                        .comment("Movement speed for mobile Poison Emitters. Default: 0.35")
                                        .defineInRange("poison_emitter_mobile_movement_speed",
                                                        CreRacesConfig.ENTITY_POISON_EMITTER_MOBILE_MOVEMENT_SPEED
                                                                        .get(),
                                                        0.0, 1.0);
                        poison_emitter_lifetime_ticks = builder
                                        .comment("Lifetime (ticks) for Poison Emitters. Default: 2400")
                                        .defineInRange("poison_emitter_lifetime_ticks",
                                                        CreRacesConfig.ENTITY_POISON_EMITTER_LIFETIME_TICKS.get(),
                                                        1, 10000);
                        tornado_lifetime_ticks = builder.comment("Lifetime (ticks) for Tornado entities. Default: 2400")
                                        .defineInRange("tornado_lifetime_ticks",
                                                        CreRacesConfig.ENTITY_TORNADO_LIFETIME_TICKS.get(), 1,
                                                        10000);
                        builder.pop();

                        builder.push("Specialized");
                        remains_decay_time = builder.comment("Time in ticks before remains decay. Default: 1200")
                                        .defineInRange("remains_decay_time",
                                                        (double) CreRacesConfig.REMAINS_DECAY_TIME.get(), 0.0,
                                                        100000.0);
                        remains_health = builder.comment("Max health for remains entities. Default: 10.0")
                                        .defineInRange("remains_health",
                                                        CreRacesConfig.REMAINS_HEALTH.get(), 1.0, 1000.0);
                        builder.pop();
                }
        }

        public static void saveClientHudConfig() {
                CLIENT.hud_anchor_x.set(CreRacesConfig.HUD_ANCHOR_X.get());
                CLIENT.hud_anchor_y.set(CreRacesConfig.HUD_ANCHOR_Y.get());
                CLIENT.hud_portrait_x.set(CreRacesConfig.HUD_PORTRAIT_X.get());
                CLIENT.hud_portrait_y.set(CreRacesConfig.HUD_PORTRAIT_Y.get());
                CLIENT.hud_abilities_x.set(CreRacesConfig.HUD_ABILITIES_X.get());
                CLIENT.hud_abilities_y.set(CreRacesConfig.HUD_ABILITIES_Y.get());
                CLIENT.hud_bars_x.set(CreRacesConfig.HUD_BARS_X.get());
                CLIENT.hud_bars_y.set(CreRacesConfig.HUD_BARS_Y.get());
                CLIENT.bar_label_mode.set(CreRacesConfig.BAR_LABEL_MODE.get());
                CLIENT.bar_show_seconds.set(CreRacesConfig.BAR_SHOW_SECONDS.get());
                CLIENT.hud_bars_grow_up.set(CreRacesConfig.HUD_BARS_GROW_UP.get());
                CLIENT.hud_abilities_vertical.set(CreRacesConfig.HUD_ABILITIES_VERTICAL.get());
                CLIENT.hud_slot_label_side.set(CreRacesConfig.HUD_SLOT_LABEL_SIDE.get());
                CLIENT.hud_scale.set(CreRacesConfig.HUD_SCALE.get());
                CLIENT_SPEC.save();
        }

        @SubscribeEvent
        public static void onLoad(final ModConfigEvent.Loading configEvent) {
                apply(configEvent.getConfig().getSpec());
        }

        @SubscribeEvent
        public static void onReload(final ModConfigEvent.Reloading configEvent) {
                apply(configEvent.getConfig().getSpec());
        }

        public static void apply() { apply(null); }

        public static void apply(Object spec) {
                if (spec == null || spec == COMMON_SPEC) {
                CreRacesConfig.WIKI_BASE_URL = () -> COMMON.wiki_base_url.get();
                CreRacesConfig.WIKI_API_BASE = () -> COMMON.wiki_api_base.get();
                CreRacesConfig.WIKI_PAGE_PATH = () -> COMMON.wiki_page_path.get();
                CreRacesConfig.WIKI_ABILITY_NAMESPACE = () -> COMMON.wiki_ability_namespace.get();
                CreRacesConfig.DISABLE_REMOTE_DOCS = () -> COMMON.disable_remote_docs.get();
                CreRacesConfig.DOC_FETCH_TIMEOUT_SECONDS = () -> COMMON.doc_fetch_timeout_seconds.get();
                CreRacesConfig.DOC_CACHE_DIR = () -> COMMON.doc_cache_dir.get();
                CreRacesConfig.DOC_CACHE_FILENAME = () -> COMMON.doc_cache_filename.get();
                CreRacesConfig.DEVELOPER_RESOURCE_PATH = () -> COMMON.developer_resource_path.get();

                CreRacesConfig.FORCED_SELECTION = () -> COMMON.forced_selection.get();
                CreRacesConfig.ABILITY_HASTE_CAP = () -> COMMON.ability_haste_cap.get();
                CreRacesConfig.RACIAL_AD_MULTIPLIER = () -> COMMON.racial_ad_multiplier.get();
                CreRacesConfig.RESOURCE_DECAY_GRACE_PERIOD = () -> COMMON.resource_decay_grace_period.get();
                CreRacesConfig.BREAK_BLOCKS_MAX_RADIUS = () -> COMMON.break_blocks_max_radius.get();
                CreRacesConfig.REMOVE_BLOCK_HARDNESS_LIMIT = () -> COMMON.remove_block_hardness_limit.get();
                CreRacesConfig.AOE_MAX_RADIUS = () -> COMMON.aoe_max_radius.get();
                CreRacesConfig.BEAM_MAX_LENGTH = () -> COMMON.beam_max_length.get();
                CreRacesConfig.CUSTOMIZATION_VALUE_MAX_LENGTH = () -> COMMON.customization_value_max_length.get();
                CreRacesConfig.DELAY_ACTION_MAX_TICKS = () -> COMMON.delay_action_max_ticks.get();
                CreRacesConfig.ENTITY_DATA_KEY_MAX_LENGTH = () -> COMMON.entity_data_key_max_length.get();
                CreRacesConfig.GIVE_ITEM_MAX_COUNT = () -> COMMON.give_item_max_count.get();
                CreRacesConfig.MASS_SUMMON_MAX_COUNT = () -> COMMON.mass_summon_max_count.get();
                CreRacesConfig.NETWORK_TEAM_NAME_MAX_LEN = () -> COMMON.network_team_name_max_len.get();
                CreRacesConfig.TEAM_MAX_SIZE = () -> COMMON.team_max_size.get();
                CreRacesConfig.DOUBLE_JUMP_COOLDOWN_TICKS = () -> COMMON.double_jump_cooldown_ticks.get();
                CreRacesConfig.SPIRIT_SPAWN_CHECK_RADIUS = () -> COMMON.spirit_spawn_check_radius.get();
                CreRacesConfig.MAX_RAT_TUNNELS = () -> COMMON.max_rat_tunnels.get();

                CreRacesConfig.MINI_BUILD_ENABLED = () -> COMMON.mini_build_enabled.get();
                CreRacesConfig.MINI_FURNACE_ENABLED = () -> COMMON.mini_furnace_enabled.get();
                CreRacesConfig.MINI_CAMPFIRE_ENABLED = () -> COMMON.mini_campfire_enabled.get();
                CreRacesConfig.MINI_BREWING_STAND_ENABLED = () -> COMMON.mini_brewing_stand_enabled.get();
                CreRacesConfig.MINI_PLACE_WHITELIST_ENABLED = () -> COMMON.mini_place_whitelist_enabled.get();
                CreRacesConfig.RACE_SOCIAL_DEFENSE_RANGE = () -> COMMON.social_defense_range.get();
                CreRacesConfig.MICRO_BLOCK_LIGHT_PER_TORCH = () -> COMMON.micro_block_light_per_torch.get();
                CreRacesConfig.MICRO_BLOCK_MAX_LIGHT = () -> COMMON.micro_block_max_light.get();
                CreRacesConfig.MINIBUILD_REQUIRES_LEARNED = () -> COMMON.mini_build_requires_learned.get();
                CreRacesConfig.MINI_CRAFTING_DISTANCE_SQR = () -> COMMON.mini_crafting_distance_sqr.get();
                CreRacesConfig.MINI_PLACEMENT_SPAM_THRESHOLD_MS = () -> COMMON.mini_placement_spam_threshold_ms.get();
                CreRacesConfig.MINI_BLOCK_REACH_MARGIN = () -> COMMON.mini_block_reach_margin.get();
                CreRacesConfig.MINI_BLOCK_WATER_RESISTANT = () -> COMMON.mini_block_water_resistant.get();
                CreRacesConfig.MINI_BUILD_DIMENSION_BLACKLIST = () -> (java.util.List<String>) (java.util.List<?>) COMMON.mini_build_dimension_blacklist
                                .get();

                CreRacesConfig.TERRITORY_DEFAULT_CLAIM_RADIUS        = () -> COMMON.territory_default_claim_radius.get();
                CreRacesConfig.TERRITORY_MAX_NODES_PER_PLAYER        = () -> COMMON.territory_max_nodes_per_player.get();
                CreRacesConfig.TERRITORY_INTER_RACE_BLOCKING         = () -> COMMON.territory_inter_race_blocking.get();
                CreRacesConfig.TERRITORY_LEADER_DECAY_THRESHOLD_DAYS = () -> COMMON.territory_leader_decay_threshold_days.get();
                CreRacesConfig.TERRITORY_SUCCESSION_WINDOW_DAYS      = () -> COMMON.territory_succession_window_days.get();
                CreRacesConfig.TERRITORY_SUCCESSION_TICK_INTERVAL    = () -> COMMON.territory_succession_tick_interval.get();

                CreRacesConfig.TEAM_REQUIRE_SAME_RACE = () -> COMMON.team_require_same_race.get();

                CreRacesConfig.COIN_DROP_ENABLED = () -> COMMON.coin_drop_enabled.get();
                CreRacesConfig.SAG_WINGS = () -> COMMON.sag_wings.get();
                CreRacesConfig.GSTATE_ENABLED = () -> COMMON.gstate_enabled.get();
                CreRacesConfig.RITUAL_MODE = () -> COMMON.ritual_mode.get();

                CreRacesConfig.FAIRY_REALM_BORDER_SIZE = () -> COMMON.fairy_realm_border_size.get();

                CreRacesConfig.POCKET_DIM_SPACING = () -> COMMON.pocket_dim_spacing.get();
                CreRacesConfig.POCKET_DIM_Y = () -> COMMON.pocket_dim_y.get();
                CreRacesConfig.POCKET_EXPANSION_LIMIT = () -> COMMON.pocket_expansion_limit.get();
                CreRacesConfig.POCKET_EXPANSION_COST = () -> COMMON.pocket_expansion_cost.get();
                CreRacesConfig.POCKET_INVITE_MAX = () -> COMMON.pocket_invite_max.get();
                CreRacesConfig.POCKET_BOUNDARY = () -> COMMON.pocket_boundary.get();

                CreRacesConfig.ACTION_DEFAULT_POCKET_DIM = () -> COMMON.action_default_pocket_dim.get();
                CreRacesConfig.ACTION_DEFAULT_POCKET_STRUCTURE = () -> COMMON.action_default_pocket_structure.get();
                CreRacesConfig.ACTION_DEFAULT_POCKET_SPAWN_X_OFFSET = () -> COMMON.action_default_pocket_spawn_x_offset
                                .get();
                CreRacesConfig.ACTION_DEFAULT_POCKET_SPAWN_Y_OFFSET = () -> COMMON.action_default_pocket_spawn_y_offset
                                .get();
                CreRacesConfig.ACTION_DEFAULT_POCKET_SPAWN_Z_OFFSET = () -> COMMON.action_default_pocket_spawn_z_offset
                                .get();

                CreRacesConfig.RACE_ADDONS_ENABLED = () -> COMMON.race_addons_enabled.get();
                CreRacesConfig.LORE_ADDONS_ENABLED = () -> COMMON.lore_addons_enabled.get();
                CreRacesConfig.MAX_SOUL = () -> COMMON.max_soul.get();

                CreRacesConfig.PASSIVE_EXECUTION_INTERVAL = () -> COMMON.passive_execution_interval.get();
                CreRacesConfig.SUNLIGHT_EQUIPMENT_BREAK_CHANCE = () -> COMMON.sunlight_equipment_break_chance.get();
                CreRacesConfig.SUNLIGHT_BURN_SECONDS = () -> COMMON.sunlight_burn_seconds.get();
                CreRacesConfig.PASSIVE_EFFECT_BUFFER_TICKS = () -> COMMON.passive_effect_buffer_ticks.get();
                CreRacesConfig.PASSIVE_DEFAULT_MAX_FOOD = () -> COMMON.passive_default_max_food.get();
                CreRacesConfig.PASSIVE_DEFAULT_MAX_SATURATION = () -> COMMON.passive_default_max_saturation.get();

                CreRacesConfig.RAT_VENOM_SCALING = () -> COMMON.rat_venom_scaling.get();
                CreRacesConfig.BOILING_SCALING = () -> COMMON.boiling_scaling.get();
                CreRacesConfig.BLEEDING_SCALING = () -> COMMON.bleeding_scaling.get();
                } // end COMMON

                if (spec == null || spec == CLIENT_SPEC) {
                CreRacesConfig.MINI_MODEL_CACHE_SIZE = () -> CLIENT.mini_model_cache_size.get();
                CreRacesConfig.VISUAL_SYNC_DISTANCE = () -> CLIENT.visual_sync_distance.get();
                CreRacesConfig.RACE_OVERLAY_OFFSET_X = () -> CLIENT.race_overlay_offset_x.get();
                CreRacesConfig.RACE_OVERLAY_OFFSET_Y = () -> CLIENT.race_overlay_offset_y.get();
                CreRacesConfig.RACE_OVERLAYS_ENABLED = () -> CLIENT.race_overlays_enabled.get();
                CreRacesConfig.ENGINE_POPUPS_ENABLED = () -> CLIENT.engine_popups_enabled.get();
                CreRacesConfig.MINI_DUMMY_CACHE_SIZE = () -> CLIENT.mini_dummy_cache_size.get();
                CreRacesConfig.HUD_ANCHOR_X = () -> CLIENT.hud_anchor_x.get();
                CreRacesConfig.HUD_ANCHOR_Y = () -> CLIENT.hud_anchor_y.get();
                CreRacesConfig.HUD_PORTRAIT_X = () -> CLIENT.hud_portrait_x.get();
                CreRacesConfig.HUD_PORTRAIT_Y = () -> CLIENT.hud_portrait_y.get();
                CreRacesConfig.HUD_ABILITIES_X = () -> CLIENT.hud_abilities_x.get();
                CreRacesConfig.HUD_ABILITIES_Y = () -> CLIENT.hud_abilities_y.get();
                CreRacesConfig.HUD_BARS_X = () -> CLIENT.hud_bars_x.get();
                CreRacesConfig.HUD_BARS_Y = () -> CLIENT.hud_bars_y.get();
                CreRacesConfig.BAR_LABEL_MODE = () -> CLIENT.bar_label_mode.get();
                CreRacesConfig.BAR_SHOW_SECONDS = () -> CLIENT.bar_show_seconds.get();
                CreRacesConfig.HUD_BARS_GROW_UP = () -> CLIENT.hud_bars_grow_up.get();
                CreRacesConfig.HUD_ABILITIES_VERTICAL = () -> CLIENT.hud_abilities_vertical.get();
                CreRacesConfig.HUD_SLOT_LABEL_SIDE = () -> CLIENT.hud_slot_label_side.get();
                CreRacesConfig.HUD_SCALE = () -> CLIENT.hud_scale.get();
                CreRacesConfig.HUD_CONFIG_SAVE = ForgeConfig::saveClientHudConfig;
                } // end CLIENT

                if (spec == null || spec == ENTITIES_SPEC) {
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
                } // end ENTITIES
        }
}
