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

public class FabricConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("creraces.json").toFile();

    public static void load() {
        ConfigData data = new ConfigData();

        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                data = GSON.fromJson(reader, ConfigData.class);
                if (data == null)
                    data = new ConfigData();
            } catch (IOException e) {
                LOGGER.error("Failed to load CreRaces Fabric config", e);
            }
        } else {
            save(data);
        }

        apply(data);
    }

    private static void save(ConfigData data) {
        try {
            if (CONFIG_FILE.getParentFile() != null) {
                CONFIG_FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save CreRaces Fabric config", e);
        }
    }

    private static void apply(ConfigData data) {
        CreRacesConfig.WIKI_BASE_URL = () -> data.documentation.wiki_base_url;
        CreRacesConfig.WIKI_PAGE_PATH = () -> data.documentation.wiki_page_path;
        CreRacesConfig.WIKI_ABILITY_NAMESPACE = () -> data.documentation.wiki_ability_namespace;
        CreRacesConfig.DISABLE_REMOTE_DOCS = () -> data.documentation.disable_remote_docs;
        CreRacesConfig.DOC_FETCH_TIMEOUT_SECONDS = () -> data.documentation.doc_fetch_timeout_seconds;
        CreRacesConfig.DOC_CACHE_DIR = () -> data.documentation.doc_cache_dir;
        CreRacesConfig.DOC_CACHE_FILENAME = () -> data.documentation.doc_cache_filename;

        // Gameplay & UI
        CreRacesConfig.FORCED_SELECTION = () -> data.gameplay.forced_selection;

        // Gameplay & UI
        CreRacesConfig.FORCED_SELECTION = () -> data.gameplay.forced_selection;
    }

    public static class ConfigData {
        public Documentation documentation = new Documentation();
        public Gameplay gameplay = new Gameplay();

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
        }
    }
}
