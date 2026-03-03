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

        static {
                final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
                COMMON = specPair.getLeft();
                COMMON_SPEC = specPair.getRight();
        }

        public static class Common {
                public final ForgeConfigSpec.ConfigValue<String> wiki_base_url;
                public final ForgeConfigSpec.ConfigValue<String> wiki_page_path;
                public final ForgeConfigSpec.ConfigValue<String> wiki_ability_namespace;
                public final ForgeConfigSpec.BooleanValue disable_remote_docs;
                public final ForgeConfigSpec.IntValue doc_fetch_timeout_seconds;
                public final ForgeConfigSpec.ConfigValue<String> doc_cache_dir;
                public final ForgeConfigSpec.ConfigValue<String> doc_cache_filename;

                public final ForgeConfigSpec.BooleanValue forced_selection;
                public final ForgeConfigSpec.BooleanValue mini_build_enabled;
                public final ForgeConfigSpec.BooleanValue mini_furnace_enabled;
                public final ForgeConfigSpec.DoubleValue ability_haste_cap;
                // Safety caps (0 = disabled)
                public final ForgeConfigSpec.IntValue break_blocks_max_radius;
                public final ForgeConfigSpec.IntValue aoe_max_radius;
                public final ForgeConfigSpec.IntValue beam_max_length;
                public final ForgeConfigSpec.IntValue entity_data_key_max_length;

                public Common(ForgeConfigSpec.Builder builder) {
                        builder.push("Documentation");
                        wiki_base_url = builder.comment("The base URL for the wiki").define("wiki_base_url",
                                        "https://creraces.wiki.gg/");
                        wiki_page_path = builder.comment("The path part of the wiki URL").define("wiki_page_path",
                                        "wiki/");
                        wiki_ability_namespace = builder.comment("The prefix for ability pages").define(
                                        "wiki_ability_namespace",
                                        "Ability:");
                        disable_remote_docs = builder.comment("Set to true to disable remote documentation fetching")
                                        .define("disable_remote_docs", false);
                        doc_fetch_timeout_seconds = builder
                                        .comment("Timeout in seconds for fetching remote documentation")
                                        .defineInRange("doc_fetch_timeout_seconds", 10, 1, 60);
                        doc_cache_dir = builder.comment("Directory name for the documentation cache").define(
                                        "doc_cache_dir",
                                        "cache");
                        doc_cache_filename = builder.comment("Filename for the documentation cache")
                                        .define("doc_cache_filename", "docs.json");
                        builder.pop();

                        builder.push("Gameplay");
                        forced_selection = builder.comment(
                                        "Keep the selection GUI open until a race is chosen. Also grants invulnerability while choosing.")
                                        .define("forced_selection", true);
                        mini_build_enabled = builder.comment(
                                        "Enable the Mini Build System. OFF by default for compatibility. Requires restart. "
                                                        + "When disabled, MicroBlock world data becomes air until re-enabled.")
                                        .define("mini_build_enabled", true);
                        mini_furnace_enabled = builder.comment(
                                        "Enable the Mini Furnace system. ON by default. Disabling this can improve server performance "
                                                        + "on large servers by skipping the ticking logic for all mini-furnaces.")
                                        .define("mini_furnace_enabled", true);
                        ability_haste_cap = builder.comment("The soft cap for Ability Haste. Max value is 100.")
                                        .defineInRange("ability_haste_cap", 40.0, 0.0, 100.0);

                        builder.comment("Safety caps for engine actions. Set to 0 to disable the cap.");
                        break_blocks_max_radius = builder
                                        .comment("Max radius for creraces:break_blocks. 0 = no cap.")
                                        .defineInRange("break_blocks_max_radius", 16, 0, 256);
                        aoe_max_radius = builder
                                        .comment("Max radius for creraces:aoe / creraces:apply_effect AoE. 0 = no cap.")
                                        .defineInRange("aoe_max_radius", 64, 0, 512);
                        beam_max_length = builder
                                        .comment("Max length for creraces:beam. 0 = no cap.")
                                        .defineInRange("beam_max_length", 64, 0, 512);
                        entity_data_key_max_length = builder
                                        .comment("Max character length for creraces:modify_entity_data keys. 0 = no cap.")
                                        .defineInRange("entity_data_key_max_length", 64, 0, 1024);
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
                CreRacesConfig.WIKI_PAGE_PATH = () -> COMMON.wiki_page_path.get();
                CreRacesConfig.WIKI_ABILITY_NAMESPACE = () -> COMMON.wiki_ability_namespace.get();
                CreRacesConfig.DISABLE_REMOTE_DOCS = () -> COMMON.disable_remote_docs.get();
                CreRacesConfig.DOC_FETCH_TIMEOUT_SECONDS = () -> COMMON.doc_fetch_timeout_seconds.get();
                CreRacesConfig.DOC_CACHE_DIR = () -> COMMON.doc_cache_dir.get();
                CreRacesConfig.DOC_CACHE_FILENAME = () -> COMMON.doc_cache_filename.get();

                CreRacesConfig.FORCED_SELECTION = () -> COMMON.forced_selection.get();
                CreRacesConfig.MINI_BUILD_ENABLED = () -> COMMON.mini_build_enabled.get();
                CreRacesConfig.MINI_FURNACE_ENABLED = () -> COMMON.mini_furnace_enabled.get();
                CreRacesConfig.ABILITY_HASTE_CAP = () -> COMMON.ability_haste_cap.get();
                // Safety caps
                CreRacesConfig.BREAK_BLOCKS_MAX_RADIUS = () -> COMMON.break_blocks_max_radius.get();
                CreRacesConfig.AOE_MAX_RADIUS = () -> COMMON.aoe_max_radius.get();
                CreRacesConfig.BEAM_MAX_LENGTH = () -> COMMON.beam_max_length.get();
                CreRacesConfig.ENTITY_DATA_KEY_MAX_LENGTH = () -> COMMON.entity_data_key_max_length.get();
        }
}
